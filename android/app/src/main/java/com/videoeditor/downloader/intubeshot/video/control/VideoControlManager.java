package com.videoeditor.downloader.intubeshot.video.control;

import android.os.Handler;
import android.os.Looper;

import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.video.gl.FGLTextureView;

import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public class VideoControlManager {

    private IVideoControl mVideoControl;
    private Runnable mSeekPendRunnable;
    //
    private VideoFrame mVideoFrame;
    private Handler mHandler;
    private OnProgressListener mOnProgressListener;
    // all possible internal states
    private static final int F_STATE_IDLE = 1;
    private static final int F_STATE_PREPARE = 2;
    private static final int F_STATE_PLAY = 3;
    private static final int F_STATE_PAUSE = 4;
    private static final int F_STATE_SEEK = 5;
    private static final int F_STATE_COMPLETE = 6;
    private static final int F_STATE_ERROR = -1;
    private int mCurrentState = F_STATE_IDLE;
    //
    private OnVideoStatusListener mOnVideoStatusListener;
    private OnPlayEndListener mOnPlayEndListener;

    public VideoControlManager(FGLTextureView fglTextureView) {
        mVideoControl = new VideoControl(fglTextureView);
        mHandler = new Handler(Looper.getMainLooper());
        mOnPlayEndListener = mOnNormalPlayEndListener;
        initEvent();
    }

    private void initEvent() {
        mVideoControl.setOnFirstFrameListener(new VideoControl.OnFirstFrameListener() {
            @Override
            public void onAvailable() {
                doPrepareFirstFrame();
            }
        });
        mVideoControl.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(IMediaPlayer mp) {
                doSeekComplete(false);
            }
        });
        mVideoControl.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                doPlayOrSeekComplete(mp.getDuration());
            }
        });
    }

    private OnPlayEndListener mOnNormalPlayEndListener = new OnPlayEndListener() {
        @Override
        public long onStartTime(VideoFrame videoFrame) {
            return 0;
        }

        @Override
        public PlayNext onEndNext(VideoFrame videoFrame, long realTime) {
            return null; // 正常情况，不是结束状态
        }
    };

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
//                FLog.i("mVideoControl.getCurrentPosition(): " + mVideoControl.getCurrentPosition() + " > " + mVideoFrame.getPreVideoTime());
            if (mVideoControl.isActive() && mCurrentState == F_STATE_PLAY) {
                long offsetTimeMs = mVideoFrame.getOffsetTime(mVideoControl.getCurrentPosition());
                mHandler.postDelayed(mShowProgress, 100);
                if (mOnProgressListener != null) {
                    mOnProgressListener.onProgress(mVideoFrame, offsetTimeMs);
                }
                doPlayOrSeekComplete(mVideoControl.getCurrentPosition());
            } else {
                mHandler.postDelayed(mShowProgress, 100);
            }
        }
    };

    private void doPlayOrSeekComplete(long realTime) {
        if (mCurrentState == F_STATE_PLAY) {
            PlayNext playNext = mOnPlayEndListener.onEndNext(mVideoFrame, realTime); // 判断是否需要结束
            if (playNext != null) {
//                FLog.i("doPlayComplete>>>: " + mIsFrameSeeking);
                if (playNext.getVideoFrame() == null) { // 播放结束
                    mSeekPendRunnable = null;
                    setVideoStatus(F_STATE_COMPLETE);
                    mHandler.removeCallbacks(mShowProgress);
                    mVideoControl.pause();
                } else { // 有下一个视频，仍然可以继续播放，不算真正结束
                    mSeekPendRunnable = null;
                    openFromPlay(playNext.getVideoFrame(), playNext.getOffsetStartTime());
                }
            } //else 正常状态，结束才不是空
        } else if (mCurrentState == F_STATE_PREPARE) {
            setVideoStatus(F_STATE_COMPLETE);
        } else {
            doSeekComplete(false);
        }
    }

    private void doSeekComplete(boolean isPrepare) {
//        FLog.i("doSeekComplete>>> isPrepare " + isPrepare + " mCurrentState: " + mCurrentState);
        if (mCurrentState == F_STATE_SEEK || isPrepare) {
            if (mSeekPendRunnable != null) {
                setVideoStatus(F_STATE_PAUSE);
                if (!isPrepare) {
                    mSeekPendRunnable.run();
                }
                mSeekPendRunnable = null;
            } else {
                setVideoStatus(F_STATE_PAUSE);
            }
        }
    }

    private void doPrepareFirstFrame() {
        if (mCurrentState == F_STATE_PREPARE) {
//            FLog.i("doPrepareFirstFrame>>> isPlayMode: " + mVideoControl.isPlayMode());
            if (mVideoControl.isPlayMode()) {
                setVideoStatus(F_STATE_PLAY);
            } else {
                doSeekComplete(true);
            }
        } else {
            if (mCurrentState != F_STATE_PAUSE) {
                FLog.i("doPrepareFirstFrame err!!!" + mCurrentState);
                if (mVideoControl.isPlayMode()) {
                    setVideoStatus(F_STATE_PLAY);
                } else {
                    setVideoStatus(F_STATE_PAUSE);
                }
            }
        }
    }

    private void doSeek(VideoFrame videoFrame, long offsetTimeMs) {
        if (mCurrentState == F_STATE_PAUSE) {
            if (videoFrame.getFile().getAbsolutePath().equals(mVideoFrame.getFile().getAbsolutePath())) { //相同的video，处于不同的顺序而已
                if (isPlayMode()) { // 如果切换为seek Frame态
                    openFromSeek(mVideoFrame, offsetTimeMs);
                } else {
                    setVideoStatus(F_STATE_SEEK);
                    mVideoControl.seekTo(videoFrame.getRealTime(offsetTimeMs));
                }
            } else {
                mVideoFrame = videoFrame;
                openFromSeek(mVideoFrame, offsetTimeMs);
            }
        } else if (mCurrentState == F_STATE_PLAY || mCurrentState == F_STATE_COMPLETE) {
            openFromSeek(mVideoFrame, offsetTimeMs);
        }
    }

    private void doReset() {
        setVideoStatus(F_STATE_IDLE);
        mSeekPendRunnable = null;
        mVideoControl.reset();
    }

    private void openVideo(boolean isSeekFrame, VideoFrame videoFrame, long offsetTimeMs) {
        if (isSeekFrame) {
            doReset();
            setVideoStatus(F_STATE_PREPARE);
            mVideoControl.openVideoFile(videoFrame, videoFrame.getRealTime(offsetTimeMs), false);
        } else {
            doReset();
            setVideoStatus(F_STATE_PREPARE);
            mVideoControl.openVideoFile(videoFrame, videoFrame.getRealTime(offsetTimeMs), true);
        }
        mVideoFrame = videoFrame;
        selectCurVideoFrame(videoFrame);
        if (!isSeekFrame) {
            mHandler.removeCallbacks(mShowProgress);
            mHandler.post(mShowProgress);
        }
    }

    private void openFromSeek(VideoFrame videoFrame, long offsetTimeMs) {
        openVideo(true, videoFrame, offsetTimeMs);
    }

    private void openFromPlay(VideoFrame videoFrame, long offsetTimeMs) {
        openVideo(false, videoFrame, offsetTimeMs);
    }

    private void play(boolean isRePlay, VideoFrame videoFrame) {
        if (isRePlay) {
            long startOffsetTime = mOnPlayEndListener.onStartTime(videoFrame);
            openFromPlay(videoFrame, startOffsetTime);
        } else {
            if (mCurrentState == F_STATE_PAUSE || mCurrentState == F_STATE_COMPLETE) {
                if (mVideoControl.isPlayMode()) {
                    if (mCurrentState == F_STATE_COMPLETE) { // 播放完成了，要重新开始
                        setVideoStatus(F_STATE_PLAY);
                        long startOffsetTime = mOnPlayEndListener.onStartTime(videoFrame);
                        openFromPlay(videoFrame, startOffsetTime);
                    } else {
                        setVideoStatus(F_STATE_PLAY);
                        mVideoControl.start();
                    }
                } else {
                    openFromPlay(videoFrame, videoFrame.getOffsetTime(mVideoControl.getCurrentPosition()));
                }
            }
        }
    }

    // 供外部调用的action
    public void actionFrameFrame(VideoFrame videoFrame) {
        mVideoFrame = videoFrame;
        long startOffsetTime = mOnPlayEndListener.onStartTime(videoFrame);
        openFromSeek(mVideoFrame, startOffsetTime);
    }

    public void actionRePlay() {
        VideoFrame videoFrame = VideoFrameManager.getInstance().getFirstVideoFrame();
        actionPlay(videoFrame);
    }

    public void actionPlay(VideoFrame videoFrame) {
        mVideoFrame = videoFrame;
        play(true, mVideoFrame);
    }

    public void actionPlay() {
        if (mVideoFrame == null) { // 第一次进入就是空
            mVideoFrame = VideoFrameManager.getInstance().getFirstVideoFrame();
        }
        play(false, mVideoFrame);
    }

    public void actionPause() {
        if (mCurrentState == F_STATE_PLAY || mCurrentState == F_STATE_COMPLETE) {
            setVideoStatus(F_STATE_PAUSE);
            mVideoControl.pause();
        }
    }

    public void actionSeek(final VideoFrame videoFrame, final long offsetTimeMs) {
        FLog.i("actionSeek mCurrentState: " + mCurrentState);
        if (mCurrentState == F_STATE_PAUSE ||
                mCurrentState == F_STATE_PLAY ||
                mCurrentState == F_STATE_COMPLETE ||
                mCurrentState == F_STATE_SEEK) {
            if (mCurrentState == F_STATE_SEEK) { // 已经在seek了，先搁置
                mSeekPendRunnable = new Runnable() {
                    @Override
                    public void run() {
                        doSeek(videoFrame, offsetTimeMs);
                    }
                };
            } else {
                doSeek(videoFrame, offsetTimeMs);
            }
        }
    }

    public void actionSuspend() {
        mSeekPendRunnable = null;
        mHandler.removeCallbacks(mShowProgress);
        mVideoControl.suspend();
    }

    public void actionResume() {
        mSeekPendRunnable = null;
        if (mCurrentState == F_STATE_PLAY) {
            mHandler.removeCallbacks(mShowProgress);
            mHandler.post(mShowProgress);
        }
        mVideoControl.resume();
    }

    public void actionReset() {
        doReset();
        mHandler.removeCallbacks(mShowProgress);
    }

    public void actionRelease() {
        mSeekPendRunnable = null;
        mVideoControl.release();
        setVideoStatus(F_STATE_IDLE);
        mHandler.removeCallbacks(mShowProgress);
    }

    // 监听器、状态之类一些无关紧要的东西

    private void setVideoStatus(int videoStatus) {
        mCurrentState = videoStatus;
        if (videoStatus == F_STATE_IDLE) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onNull();
            }
        } else if (videoStatus == F_STATE_PREPARE) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onPrepare();
            }
        } else if (videoStatus == F_STATE_PLAY) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onPlay();
            }
        } else if (videoStatus == F_STATE_PAUSE) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onPause();
            }
        } else if (videoStatus == F_STATE_SEEK) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onSeek();
            }
        } else if (videoStatus == F_STATE_COMPLETE) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onPause();
            }
        } else if (videoStatus == F_STATE_ERROR) {
            if (mOnVideoStatusListener != null) {
                mOnVideoStatusListener.onNull();
            }
        }
    }

    public IVideoControl getVideoControl() {
        return mVideoControl;
    }

    public void setVideoMode(int videoMode) {
        mVideoControl.setVideoMode(videoMode);
    }

    public int getVideoMode() {
        return mVideoControl.getVideoMode();
    }

    public void setOnVideoStatusListener(OnVideoStatusListener onVideoStatusListener) {
        mOnVideoStatusListener = onVideoStatusListener;
        setVideoStatus(F_STATE_IDLE);
    }

    public boolean isPlaying() {
        return mCurrentState == F_STATE_PLAY;
    }

    public boolean isPlayMode() {
        return mVideoControl.isPlayMode();
    }

    public boolean isActive() {
        return mVideoControl.isActive();
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        mOnProgressListener = onProgressListener;
    }

    private void selectCurVideoFrame(VideoFrame curVideoFrame) {
        List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
        for (VideoFrame videoFrame : videoFrameList) {
            videoFrame.setIsCheck(false);
        }
        curVideoFrame.setIsCheck(true);
    }

    public void setOnPlayEndListener(OnPlayEndListener onPlayEndListener) {
        mOnPlayEndListener = onPlayEndListener;
    }

    public void resetOnPlayEndListener() {
        mOnPlayEndListener = mOnNormalPlayEndListener;
    }

    public VideoFrame getVideoFrame() {
        return mVideoFrame;
    }

    public long getCurrentPosition() {
        return mVideoControl.getCurrentPosition();
    }

    public static class PlayNext {
        private VideoFrame videoFrame;
        private long offsetStartTime;

        public PlayNext(VideoFrame videoFrame, long offsetStartTime) {
            this.videoFrame = videoFrame;
            this.offsetStartTime = offsetStartTime;
        }

        VideoFrame getVideoFrame() {
            return videoFrame;
        }

        long getOffsetStartTime() {
            return offsetStartTime;
        }
    }

    public interface OnProgressListener {
        void onProgress(VideoFrame videoFrame, long offsetTime);
    }

    public interface OnPlayEndListener {
        long onStartTime(VideoFrame videoFrame);

        PlayNext onEndNext(VideoFrame videoFrame, long realTime);
    }

    public interface OnVideoStatusListener {
        void onPrepare();

        void onSeek();

        void onPlay();

        void onPause();

        void onNull();
    }

}
