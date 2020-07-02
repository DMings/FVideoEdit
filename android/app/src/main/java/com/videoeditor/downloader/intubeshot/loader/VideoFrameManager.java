package com.videoeditor.downloader.intubeshot.loader;

import android.graphics.Bitmap;

import com.videoeditor.downloader.intubeshot.frame.FrameEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoFrameManager {

    public final static int MAX_WIDTH = 160;
    public final static int MAX_HEIGHT = 160;
    public static final int TIME_SPAN = 3000;

    private static volatile VideoFrameManager sVideoFrameManager;

    public static synchronized VideoFrameManager getInstance() {
        if (sVideoFrameManager == null) {
            synchronized (VideoFrameManager.class) {
                if (sVideoFrameManager == null) {
                    sVideoFrameManager = new VideoFrameManager();
                }
            }
        }
        return sVideoFrameManager;
    }

    private List<VideoFrame> mVideoFrameList = new ArrayList<>();
    private List<File> mFileList = new ArrayList<>();

    private VideoFrameManager() {
    }

    public void setVideoFileList(List<File> fileList) {
        if (fileList == null) {
            return;
        }
        mFileList.clear();
        for (File file : fileList) {
            if (file.exists()) {
                mFileList.add(file);
            }
        }
    }

    public VideoFrame getFirstVideoFrame() {
        if (mVideoFrameList.size() > 0) {
            return mVideoFrameList.get(0);
        }
        return null;
    }

    public void openFrameList(final OnFrameListener onFrameListener) {
        FrameLoader.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                long totalVideoTime = 0;
                int videoCount = 0;
                List<File> fileList = new ArrayList<>();
                for (int i = 0; i < mFileList.size(); i++) {
                    VideoFrame videoFrame = new VideoFrame(mFileList.get(i));
                    boolean isOpen = videoFrame.open(MAX_WIDTH, MAX_HEIGHT);
                    if (isOpen) {
                        mVideoFrameList.add(videoFrame);
                        if (videoCount == 0) {
                            if (onFrameListener != null) {
                                onFrameListener.start(videoFrame);
                            }
                        }
                        videoFrame.setPreVideoTime(totalVideoTime);
                        videoFrame.handleVideoFrame(videoFrame.getVideoTag() + i);
                        totalVideoTime += videoFrame.getOffsetDurationMs();
                        if (onFrameListener != null) {
                            onFrameListener.onFrameChange(videoFrame);
                        }
                        videoCount++;
                    } else {
                        videoFrame.release();
                        fileList.add(mFileList.get(i));
                    }
                }
                if (onFrameListener != null) {
                    onFrameListener.end(fileList);
                }
            }
        });
    }

    public Bitmap getFrame(String key, long timeUs) {
        FrameKey frameKey = FrameKey.parseName(key);
        for (VideoFrame videoFrame : mVideoFrameList) {
            if (videoFrame.getVideoTag().equals(frameKey.getTag())) {
                return videoFrame.getKeyFrameAtTimeMs(timeUs);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.RGB_565);
        bitmap.eraseColor(0);
        return bitmap;
    }

    public VideoFrame getVideoFrame(String UUID) {
        for (VideoFrame videoFrame : mVideoFrameList) {
            if (videoFrame.getUUID() != null && videoFrame.getUUID().equals(UUID)) {
                return videoFrame;
            }
        }
        return null;
    }

    public VideoFrame getNextVideoFrame(String UUID) {
        if (UUID == null) {
            if (mVideoFrameList.size() > 0) {
                return mVideoFrameList.get(0);
            }
            return null;
        }
        for (int i = 0; i < mVideoFrameList.size(); i++) {
            VideoFrame vf = mVideoFrameList.get(i);
            if (vf.getUUID() != null && vf.getUUID().equals(UUID)) {
                if (i + 1 < mVideoFrameList.size()) {
                    return mVideoFrameList.get(i + 1);
                }
                return null;
            }
        }
        return null;
    }

    public synchronized List<VideoFrame> getVideoFrameList() {
        return mVideoFrameList;
    }

    public synchronized void updateVideoFrameListTime() {
        long totalVideoTime = 0;
        for (int i = 0; i < mVideoFrameList.size(); i++) { // 矫正时间偏移
            mVideoFrameList.get(i).setPreVideoTime(totalVideoTime);
            totalVideoTime += mVideoFrameList.get(i).getOffsetDurationMs();
        }
    }

    public synchronized List<FrameEntity> getAllFrameEntityList() {
        List<FrameEntity> frameEntityList = new ArrayList<>();
        for (int i = 0; i < mVideoFrameList.size(); i++) {
            frameEntityList.addAll(mVideoFrameList.get(i).getFrameEntityList());
        }
        return frameEntityList;
    }

    public long getVideoTimeMs() {
        long time = 0;
        for (VideoFrame videoFrame : mVideoFrameList) {
            time += videoFrame.getOffsetDurationMs();
        }
        return time;
    }

    public void release() {
        for (VideoFrame videoFrame : mVideoFrameList) {
            videoFrame.release();
        }
        mFileList.clear();
        mVideoFrameList.clear();
    }

    public interface OnFrameListener {

        void start(VideoFrame videoFrame);

        void onFrameChange(VideoFrame videoFrame);

        void end(List<File> fileList);

    }
}
