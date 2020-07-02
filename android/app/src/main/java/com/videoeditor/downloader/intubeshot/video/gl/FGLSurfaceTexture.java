package com.videoeditor.downloader.intubeshot.video.gl;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import com.videoeditor.downloader.intubeshot.create.CreateVideoUtils;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.utils.FLog;

public class FGLSurfaceTexture {

    private boolean mIsRelease;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private int mTexture;
    private OnFrameListener mOnFrameListener;
    private int mWidth, mHeight;
    private int mViewWidth, mViewHeight;
    private int mRotateDegree;
    private float mFirstWidthRatio = 0;
    private float mFirstHeightRatio = 0;
    private int mFirstVideoWidth = 0;
    private int mFirstVideoHeight = 0;
    private float[] mVerMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
    };
    private float[] mBlurVerMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
    };
    protected float[] mTexCoordinate = new float[]{
            0, 0,
            0, 1,
            1, 1,
            1, 0,
    };
    public final static int MODE_STROKE_SCREEN = 0;
    public final static int MODE_FULL_SCREEN = 1;
    public final static int MODE_BLUR_SCREEN = 2;
    public final static int MODE_PAD_SCREEN = 3;
    private int mVideoMode = MODE_STROKE_SCREEN;

    public void setOnFrameListener(OnFrameListener listener) {
        mOnFrameListener = listener;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public int getTexture() {
        return mTexture;
    }

    public float[] getVerMatrix() {
        return mVerMatrix;
    }

    public float[] getBlurVerMatrix() {
        return mBlurVerMatrix;
    }

    public float[] getTexCoordinate() {
        return mTexCoordinate;
    }

    public Surface createSurface(Context context) {
        mIsRelease = false;
        DisplayMetrics outMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int size;
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getRealMetrics(outMetrics);
            size = outMetrics.widthPixels;
        } else {
            size = 720;
        }
        mTexture = FGLUtils.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTexture);
        mSurfaceTexture.setDefaultBufferSize(size, size);
        mSurface = new Surface(mSurfaceTexture);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {

            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mOnFrameListener != null) {
                    mOnFrameListener.onFrameAvailable(FGLSurfaceTexture.this);
                }
            }
        });
        return mSurface;
    }

    @Deprecated
    public void setVideoConfigure(VideoFrame videoFrame, int videoWidth, int videoHeight, int rotateDegree, int viewWidth, int viewHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            FLog.e("change setVideoSurface videoWidth: " + videoWidth + " videoHeight: " + videoHeight + " rotateDegree: " + rotateDegree);
            mWidth = videoWidth;
            mHeight = videoHeight;
            mRotateDegree = rotateDegree;
            mViewWidth = viewWidth;
            mViewHeight = viewHeight;
            handleRatio(videoFrame, mWidth, mHeight, mViewWidth, mViewHeight, mRotateDegree);
        }
    }

    public void setVideoConfigure(VideoFrame videoFrame, int videoWidth, int videoHeight, int viewWidth, int viewHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            FLog.e("change setVideoSurface videoWidth: " + videoWidth + " videoHeight: " + videoHeight);
            mWidth = videoWidth;
            mHeight = videoHeight;
            mViewWidth = viewWidth;
            mViewHeight = viewHeight;
            handleRatio(videoFrame, mWidth, mHeight, mViewWidth, mViewHeight, mRotateDegree);
        }
    }

    public void setVideoRotation(VideoFrame videoFrame, int degree) {
        FLog.e("setVideoRotation: " + degree);
        mRotateDegree = degree;
        handleRatio(videoFrame, mWidth, mHeight, mViewWidth, mViewHeight, mRotateDegree);
    }

    private void handleRatio(VideoFrame videoFrame, int videoWidth, int videoHeight, int viewWidth, int viewHeight, int degree) {
        int t;
        if (degree == 90 || degree == 270) {
            t = videoWidth;
            videoWidth = videoHeight;
            videoHeight = t;
        }
        float ratioY = 1;
        float ratioX = 1;
        float rTex = 1.0F * videoWidth / videoHeight;
        if (rTex < 1) {
            ratioX = 1.0F * (1.0F * videoWidth * viewHeight / (videoHeight * viewWidth));
        } else {
            ratioY = (float) (1.0 * videoHeight * viewWidth / videoWidth / viewHeight);
        }
        Matrix.setIdentityM(mVerMatrix, 0);
        mVerMatrix[0] = ratioX;
        mVerMatrix[5] = ratioY;
        Matrix.setIdentityM(mBlurVerMatrix, 0);
        mBlurVerMatrix[0] = ratioX;
        mBlurVerMatrix[5] = ratioY;
//        int d = (360 - degree) % 360;
//        Matrix.rotateM(mVerMatrix, 0, d, 0, 0, 1f);
        if (mVideoMode == MODE_STROKE_SCREEN) {
            dependOnFirstVideoFrame(videoFrame);
        } else if (mVideoMode == MODE_BLUR_SCREEN) {
            fullScreenVideo(mBlurVerMatrix);
        } else if (mVideoMode == MODE_PAD_SCREEN) {
            hollowOutVideo();
        } else {
            fullScreenVideo(mVerMatrix);
        }
    }

    private void dependOnFirstVideoFrame(VideoFrame curVideoFrame) {
        VideoFrame firstVideoFrame = VideoFrameManager.getInstance().getFirstVideoFrame();
        if (firstVideoFrame != null &&
                curVideoFrame != null &&
                curVideoFrame.getUUID() != null &&
                curVideoFrame.getUUID() != null) {
            // 如果不是第一个，则后面的依赖第一个的尺寸
            if (!curVideoFrame.getUUID().equals(firstVideoFrame.getUUID())) {
                int curWidth = curVideoFrame.getRotateWidth();
                int curHeight = curVideoFrame.getRotateHeight();
                int firstWidth = firstVideoFrame.getRotateWidth();
                int firstHeight = firstVideoFrame.getRotateHeight();
                if (mFirstWidthRatio == 0 || mFirstHeightRatio == 0) {
                    float ratioY = 1;
                    float ratioX = 1;
                    float rTex = 1.0F * firstWidth / firstHeight;
                    if (rTex < 1) {
                        ratioX = 1.0F * firstWidth / firstHeight;
                    } else {
                        ratioY = 1.0F * firstHeight / firstWidth;
                    }
                    mFirstWidthRatio = ratioX;
                    mFirstHeightRatio = ratioY;
                    mFirstVideoWidth = (int) (mViewWidth * mFirstWidthRatio);
                    mFirstVideoHeight = (int) (mViewHeight * mFirstHeightRatio);
//                    FLog.i("mFirstWidthRatio: " + mFirstWidthRatio + " mFirstHeightRatio: " + mFirstHeightRatio + " mFirstVideoWidth: " + mFirstVideoWidth + " mFirstVideoHeight: " + mFirstVideoHeight);
                }
                mVerMatrix[0] = mFirstWidthRatio;
                mVerMatrix[5] = mFirstHeightRatio;
                Rect rect = CreateVideoUtils.getVideoRect(mFirstVideoWidth, mFirstVideoHeight,
                        curWidth, curHeight);
//                FLog.i("dependOnFirstVideoFrame rect: " + rect.toString());
                float twr = 1.0f * rect.left / curWidth;
                float thr = 1.0f * rect.top / curHeight;
                float[] texCoordinate = FGLUtils.resolveRotate(mRotateDegree, twr, thr);
                System.arraycopy(texCoordinate, 0, mTexCoordinate, 0, mTexCoordinate.length);
            } else {
//                mFirstWidthRatio = ratioX;
//                mFirstHeightRatio = ratioY;
//                mFirstVideoWidth = (int) (mViewWidth * ratioX);
//                mFirstVideoHeight = (int) (mViewHeight * ratioY);
//                float ratioX = mVerMatrix[0];
//                float ratioY = mVerMatrix[5];
//                FLog.i("111 mFirstWidthRatio: " + ratioX + " mFirstHeightRatio: " + ratioY + " mFirstVideoWidth: " + (int) (mViewWidth * ratioX) + " mFirstVideoHeight: " + (int) (mViewHeight * ratioY));
                float[] texCoordinate = FGLUtils.resolveRotate(mRotateDegree);
                System.arraycopy(texCoordinate, 0, mTexCoordinate, 0, mTexCoordinate.length);
            }
        } else {
            float[] texCoordinate = FGLUtils.resolveRotate(mRotateDegree);
            System.arraycopy(texCoordinate, 0, mTexCoordinate, 0, mTexCoordinate.length);
        }

    }

    private void fullScreenVideo(float[] verMatrix) {
        float[] texCoordinate = FGLUtils.resolveRotate(mRotateDegree);
        System.arraycopy(texCoordinate, 0, mTexCoordinate, 0, mTexCoordinate.length);
        float ratioX = verMatrix[0];
        float ratioY = verMatrix[5];
        if (ratioX >= ratioY) {
            verMatrix[0] = 1 / ratioY;
            verMatrix[5] = 1;
        } else {
            verMatrix[0] = 1;
            verMatrix[5] = 1 / ratioX;
        }
    }

    private void hollowOutVideo() {
        float[] texCoordinate = FGLUtils.resolveRotate(mRotateDegree);
        System.arraycopy(texCoordinate, 0, mTexCoordinate, 0, mTexCoordinate.length);
    }

    public void setVideoMode(VideoFrame videoFrame, int videoMode) {
        mVideoMode = videoMode;
        handleRatio(videoFrame, mWidth, mHeight, mViewWidth, mViewHeight, mRotateDegree);
    }

    public int getVideoMode() {
        return mVideoMode;
    }

    public void release() {
        mIsRelease = true;
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    public boolean isRelease() {
        return mIsRelease;
    }

    public interface OnFrameListener {
        void onFrameAvailable(FGLSurfaceTexture glSurfaceTexture);
    }
}
