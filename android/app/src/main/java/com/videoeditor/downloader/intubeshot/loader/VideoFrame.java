package com.videoeditor.downloader.intubeshot.loader;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import androidx.annotation.Nullable;

import com.videoeditor.downloader.intubeshot.frame.FrameEntity;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.utils.FUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.videoeditor.downloader.intubeshot.loader.VideoFrameManager.MAX_HEIGHT;
import static com.videoeditor.downloader.intubeshot.loader.VideoFrameManager.MAX_WIDTH;

public class VideoFrame {

    static {
        System.loadLibrary("ijkffmpeg");
        System.loadLibrary("vf");
    }

    private static native long initInstance();

    private static native int setDataSource(long ptr, String localPath, int width, int height);

//    private static native long getScaledFrameAtTime(long ptr, Bitmap bitmap);

    private static native void sourceRelease(long ptr);

    private static native long getDurationMilliSecond(long ptr);

    private static native int getWidth(long ptr);

    private static native int getHeight(long ptr);

    private static native int getRotateAngle(long ptr);

    private static native Bitmap getKeyFrameAtTimeMs(long ptr, Bitmap bitmap, long timeMs);

    private static native void releaseInstance(long ptr);

    private long mFVideoFramePtr;
    private boolean mIsOpen;
    private Bitmap mBitmap;
    private int mRotateAngle;
    private Paint mRotationPaint = new Paint();
    private Matrix mMatrix = new Matrix();
    private List<FrameEntity> mFrameEntityList = new ArrayList<>();
    private File mVideoFile;
    private String mVideoTag;
    private long mPreVideoTime;
    private long mStartTime;
    private long mEndTime;
    private long mOffsetDurationMs;
    private long mRealDurationMs;
    private String UUID;
    private boolean mIsCheck;
    private int mRotateWidth;
    private int mRotateHeight;
    private Object mReadFrameLock = new Object();

    public VideoFrame(File file) {
        mVideoFile = file;
        mFVideoFramePtr = initInstance();
    }

    public boolean isCheck() {
        return mIsCheck;
    }

    public void setIsCheck(boolean isCheck) {
        mIsCheck = isCheck;
    }

    public File getFile() {
        return mVideoFile;
    }

    public synchronized boolean open(int width, int height) {
        mVideoTag = FUtils.encrypt(mVideoFile.getName());
        final int ret = setDataSource(mFVideoFramePtr, mVideoFile.toString(),
                width, height);
        mIsOpen = ret >= 0;
        if (mIsOpen) {
            mRealDurationMs = getDurationMilliSecond(mFVideoFramePtr);
            if (mRealDurationMs < 500) { // 少于500ms 不要导入
                FLog.i("VideoFrame error mRotateAngle: " + mRotateAngle + " mVideoFile: " + mVideoFile + " mRealDurationMs: " + mRealDurationMs);
                return false;
            }
            mRotateAngle = getRotateAngle(mFVideoFramePtr);
            FLog.i("VideoFrame mVideoFile: " + mVideoFile + " mRotateAngle: " + mRotateAngle);
            mStartTime = 0;
            mEndTime = mRealDurationMs;
            mOffsetDurationMs = mEndTime - mStartTime;
        }
        return mIsOpen;
    }

    public synchronized boolean isOpen() {
        return mIsOpen;
    }

    public void handleVideoFrame(String uuid) {
        if (!isOpen()) {
            return;
        }
        UUID = uuid;
        mFrameEntityList.clear();
        FLog.i("mStartTime: " + mStartTime + " mEndTime: " + mEndTime);
        for (long t = mStartTime; t < mEndTime; t += VideoFrameManager.TIME_SPAN) {
            FrameKey frameKey = new FrameKey(mVideoTag, mStartTime, t - mStartTime);
            FrameEntity frameEntity = new FrameEntity(UUID, frameKey);
            if (frameEntity.getRealTime() + VideoFrameManager.TIME_SPAN > mEndTime) { // 最后一帧了
                float p = 1.0f * (mEndTime - frameEntity.getRealTime()) / VideoFrameManager.TIME_SPAN;
                FLog.i("end p: " + p);
                frameEntity.setPercentSize(p);
            }
            mFrameEntityList.add(frameEntity);
        }
    }

//    @CalledByNative
//    public static Bitmap createBitmap(int width, int height) {
//        return Bitmap.createBitmap(
//                width,
//                height,
//                Bitmap.Config.RGB_565);
//    }

    @Nullable
    public Bitmap getKeyFrameAtTimeMs(long timeMs) {
        if (isOpen()) {
//            FLog.i("mBitmap: " + mBitmap);
            mBitmap = getKeyFrameAtTimeMs(mFVideoFramePtr, mBitmap, timeMs);
            return rotationBitmap(mBitmap, mRotateAngle);
        }
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(
                    MAX_WIDTH,
                    MAX_HEIGHT,
                    Bitmap.Config.RGB_565);
        }
        return mBitmap;
    }

    public Bitmap getKeyFrameAtTimeMsFromCache(long timeMs) {
        Bitmap bitmap = FrameLruCache.getInstance().getFrame(getVideoTag() + "_" + timeMs);
        if (bitmap == null) {
            // 找不到请求seek
            bitmap = getKeyFrameAtTimeMs(timeMs);
        }
        FrameLruCache.getInstance().putFrame(getVideoTag() + "_" + timeMs, bitmap);
        return bitmap;
    }

    public List<FrameEntity> getFrameEntityList() {
        return mFrameEntityList;
    }

    public String getUUID() {
        return UUID;
    }

    public String getVideoTag() {
        return mVideoTag;
    }

    public long getPreVideoTime() {
        return mPreVideoTime;
    }

    public void setPreVideoTime(long preVideoTime) {
        mPreVideoTime = preVideoTime;
    }

    public void setVideoTime(long startTime, long endTime) {
        if (startTime < endTime) {
            if (startTime < 0) {
                endTime = 0;
            }
            if (endTime > mRealDurationMs) {
                endTime = mRealDurationMs;
            }
            mStartTime = startTime;
            mEndTime = endTime;
            mOffsetDurationMs = endTime - startTime;
        }
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public long getRealDurationMs() {
        return mRealDurationMs;
    }

    public long getOffsetDurationMs() {
        return mOffsetDurationMs;
    }

    public long getRealTime(long offsetTime) {
        long time = mStartTime + offsetTime;
        if (time > mEndTime) {
            time = mEndTime;
        }
        return time;
    }

    public long getOffsetTime(long realTime) {
        long time = realTime - mStartTime;
        if (time < 0) {
            time = 0;
        }
        return time;
    }

    public VideoFrame getNewVideoFrame(String uuid, long startTime, long endTime) {
        VideoFrame videoFrame = new VideoFrame(mVideoFile);
        boolean isOpen = videoFrame.open(MAX_WIDTH, MAX_HEIGHT);
        videoFrame.mStartTime = startTime;
        videoFrame.mEndTime = endTime;
        videoFrame.mOffsetDurationMs = endTime - startTime;
        videoFrame.mIsCheck = false;
        videoFrame.mRotateWidth = mRotateWidth;
        videoFrame.mRotateHeight = mRotateHeight;
        if (isOpen) {
            videoFrame.handleVideoFrame(uuid);
        }
        return videoFrame;
    }

//    public synchronized int getWidth() {
//        return getWidth(mFVideoFramePtr);
//    }
//
//    public synchronized int getHeight() {
//        return getHeight(mFVideoFramePtr);
//    }
//
    public int getRotateAngle() {
        return mRotateAngle;
    }

    public int getRotateWidth() {
        if (!isOpen()) {
            return 0;
        }
        if (mRotateAngle == 90 || mRotateAngle == 270) {
            mRotateWidth = getHeight(mFVideoFramePtr);
        } else {
            mRotateWidth = getWidth(mFVideoFramePtr);
        }
        return mRotateWidth;
    }

    public int getRotateHeight() {
        if (!isOpen()) {
            return 0;
        }
        if (mRotateAngle == 90 || mRotateAngle == 270) {
            mRotateHeight = getWidth(mFVideoFramePtr);
        } else {
            mRotateHeight = getHeight(mFVideoFramePtr);
        }
        return mRotateHeight;
    }

    public synchronized void release() {
        mIsOpen = false;
        mFrameEntityList.clear();
        sourceRelease(mFVideoFramePtr);
    }

    @Override
    protected void finalize() throws Throwable {
        releaseInstance(mFVideoFramePtr);
        super.finalize();
    }

    private Bitmap rotationBitmap(Bitmap bp, int degree) {
        Bitmap rotationBp;
        if (degree == 90 || degree == 270) {
            rotationBp = Bitmap.createBitmap(bp.getHeight(), bp.getWidth(), bp.getConfig());
        } else {
            rotationBp = Bitmap.createBitmap(bp.getWidth(), bp.getHeight(), bp.getConfig());
        }
        try {
            int width = bp.getWidth();
            int height = bp.getHeight();
            mMatrix.reset();
            mMatrix.setRotate(degree, (float) width / 2, (float) height / 2);
            float targetX = 0;
            float targetY = 0;
            if (degree == 90 || degree == 270) {
                if (width > height) {
                    targetX = (float) height / 2 - (float) width / 2;
                    targetY = 0 - targetX;
                } else {
                    targetY = (float) width / 2 - (float) height / 2;
                    targetX = 0 - targetY;
                }
            }
            mMatrix.postTranslate(targetX, targetY);
            Canvas canvas = new Canvas(rotationBp);
            canvas.drawBitmap(bp, mMatrix, mRotationPaint);
            return rotationBp;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bp;
    }

}
