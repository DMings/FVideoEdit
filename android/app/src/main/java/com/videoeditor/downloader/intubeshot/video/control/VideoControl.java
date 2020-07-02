package com.videoeditor.downloader.intubeshot.video.control;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.video.gl.FGLSurfaceTexture;
import com.videoeditor.downloader.intubeshot.video.gl.FGLTextureView;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class VideoControl implements IVideoControl {

    private IjkMediaPlayer mIjkMediaPlayer = null;
    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private int mCurrentState = STATE_IDLE;

    private FGLSurfaceTexture mGLSurfaceTexture;
    private FGLTextureView mFGLTextureView;
    private Context mContext;
    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private long mSeekAtRealStartTime;
    //
    private boolean mIsSurfaceTextureAvailable;
    private boolean mDelayOpen;

    private boolean mIsFirstFrameAvailable;
    private boolean mIsFirstFrameAvailableListener;
    private boolean mIsPlayMode;
    //
    private VideoFrame mVideoFrame;

    private OnFirstFrameListener mOnFirstFrameListener;

    VideoControl(FGLTextureView fglTextureView) {
        initView(fglTextureView);
        initData();
        initEvent();
    }

    private void initView(FGLTextureView fglTextureView) {
        mContext = fglTextureView.getContext();
        mFGLTextureView = fglTextureView;
        createPlayer();
    }

    private void initData() {
    }

    private void initEvent() {
        mFGLTextureView.setOnSurfaceTextureListener(new FGLTextureView.OnSurfaceTextureListener() {
            @Override
            public void onAvailable() {
                mIsSurfaceTextureAvailable = true;
                if (mDelayOpen) {
                    mDelayOpen = false;
                    openVideo();
                }
            }
        });
        mGLSurfaceTexture.setOnFrameListener(new FGLSurfaceTexture.OnFrameListener() {
            @Override
            public void onFrameAvailable(FGLSurfaceTexture glSurfaceTexture) {
                mFGLTextureView.glDraw(glSurfaceTexture, mIsFirstFrameAvailable);
            }
        });
        AudioManager am = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public void setVideoMode(int videoMode) {
        if (videoMode == FGLSurfaceTexture.MODE_PAD_SCREEN) {
            mFGLTextureView.setGLBackgroundColor(mContext.getResources().getColor(R.color.colorBlack));
        } else {
            mFGLTextureView.setGLBackgroundColor(mContext.getResources().getColor(R.color.colorWhite));
        }
        mGLSurfaceTexture.setVideoMode(mVideoFrame, videoMode);
        mFGLTextureView.glReDraw(mGLSurfaceTexture, mIsFirstFrameAvailable);
    }

    @Override
    public int getVideoMode() {
        return mGLSurfaceTexture.getVideoMode();
    }

    @Override
    public int getWidth() {
        return mIjkMediaPlayer.getVideoWidth();
    }

    @Override
    public int getHeight() {
        return mIjkMediaPlayer.getVideoHeight();
    }

    @Override
    public long getDurationMs() {
        return mIjkMediaPlayer.getDuration();
    }

    private void createPlayer() {
        mGLSurfaceTexture = new FGLSurfaceTexture();
        mIjkMediaPlayer = new IjkMediaPlayer();
        mIjkMediaPlayer.setSurface(null);
        mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
        mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
        mIjkMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mIjkMediaPlayer.setOnSyncMediaVideoRenderingStartListener(mOnSyncMediaVideoRenderingStartListener);
        mIjkMediaPlayer.setOnErrorListener(mErrorListener);
        mIjkMediaPlayer.setOnInfoListener(mInfoListener);
        mIjkMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mIjkMediaPlayer.setScreenOnWhilePlaying(true);
        mFGLTextureView.glPost(new Runnable() {
            @Override
            public void run() {
                mGLSurfaceTexture.createSurface(mContext);
            }
        });
    }

    @Override
    public void openVideoFile(VideoFrame videoFrame, long seekAtRealStartTime, boolean playMode) {
        mIsFirstFrameAvailable = false;
        mIsFirstFrameAvailableListener = false;
        mVideoFrame = videoFrame;
        mIsPlayMode = playMode;
        mSeekAtRealStartTime = seekAtRealStartTime;
        FLog.i("openVideoFile mSeekAtRealStartTime: " + mSeekAtRealStartTime);
        if (mIsSurfaceTextureAvailable) {
            mDelayOpen = false;
            openVideo();
        } else {
            mDelayOpen = true;
        }
    }

    private void openVideo() {
        if (mVideoFrame.getFile() == null) {
            return;
        }
        FLog.i("openVideo: " + mVideoFrame.getFile());
        configureIjkPlayer();
        mIjkMediaPlayer.setSurface(mGLSurfaceTexture.getSurface());
        try {
            mIjkMediaPlayer.setDataSource(mVideoFrame.getFile().getPath());
            mIjkMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            FLog.e("Unable to open content: " + mVideoFrame.getFile());
            mCurrentState = STATE_ERROR;
            mErrorListener.onError(mIjkMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            FLog.e("Unable to open content: " + mVideoFrame.getFile());
            mCurrentState = STATE_ERROR;
            mErrorListener.onError(mIjkMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            FLog.e("mPreparedListener------>>");
            mGLSurfaceTexture.setVideoConfigure(mVideoFrame, mp.getVideoWidth(), mp.getVideoHeight(),
                    mFGLTextureView.getWidth(), mFGLTextureView.getHeight());
            ensureVideoRenderingStart();
            // 开始
            start();
        }
    };

    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    FLog.e("mCompletionListener------>>");
                    mIsFirstFrameAvailable = true;
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mp);
                    }
                }
            };

    private IMediaPlayer.OnVideoSizeChangedListener
            mOnVideoSizeChangedListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            mGLSurfaceTexture.setVideoConfigure(mVideoFrame, mp.getVideoWidth(), mp.getVideoHeight(),
                    mFGLTextureView.getWidth(), mFGLTextureView.getHeight());
            mFGLTextureView.glReDraw(mGLSurfaceTexture, mIsFirstFrameAvailable);
        }
    };

    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                    FLog.i("Error: " + framework_err + "," + impl_err);
                    mCurrentState = STATE_ERROR;
                    mIsFirstFrameAvailable = false;
                    showErrorMsg(mContext.getString(R.string.ve_play_miss_error));
                    return true;
                }
            };

    private Runnable mOnSyncMediaVideoRenderingStartListener = new Runnable() {
        @Override
        public void run() {
//            if (mVideoFrame.isOpen()) {
//                mGLSurfaceTexture.setVideoConfigure(mVideoFrame,
//                        mVideoFrame.getRotateWidth(),
//                        mVideoFrame.getRotateHeight(),
//                        mVideoFrame.getRotateAngle(),
//                        mFGLTextureView.getWidth(), mFGLTextureView.getHeight());
//            }
            mIsFirstFrameAvailable = true;
            if (!mIsPlayMode) {
                pause();
            }
        }
    };

    private Runnable videoRenderingStartTimeout = new Runnable() {
        @Override
        public void run() {
            mFGLTextureView.glReDraw(mGLSurfaceTexture, true);
            if (mOnFirstFrameListener != null && !mIsFirstFrameAvailableListener) {
                FLog.e("mOnFirstFrameListener------>>");
                mIsFirstFrameAvailableListener = true;
                mOnFirstFrameListener.onAvailable();
            }
        }
    };

    // 确保当不调用VideoRenderingStart，手动超时调用
    private void ensureVideoRenderingStart() {
        mFGLTextureView.removeCallbacks(videoRenderingStartTimeout);
        mFGLTextureView.postDelayed(videoRenderingStartTimeout, 2500);
    }

    private void removeEnsureVideoRenderingStart() {
        mFGLTextureView.removeCallbacks(videoRenderingStartTimeout);
    }

    private IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
                    switch (arg1) {
                        case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            mCurrentState = STATE_ERROR;
                            showErrorMsg(mContext.getString(R.string.ve_play_complex_error));
                            mIsFirstFrameAvailable = false;
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                            FLog.i("MEDIA_INFO_AUDIO_RENDERING_START>: " + mIsPlayMode);
//                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            FLog.i("MEDIA_INFO_VIDEO_RENDERING_START: " + mIsPlayMode);
                            removeEnsureVideoRenderingStart();
                            videoRenderingStartTimeout.run();
                            break;
                        case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                        case IMediaPlayer.MEDIA_ERROR_TIMED_OUT:
                            FLog.i("MEDIA_INFO_NOT_SEEKABLE: " + arg1);
                            doSeekCompleteListener(mp);
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                            FLog.i("MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
                            mGLSurfaceTexture.setVideoRotation(mVideoFrame, arg2);
                            mFGLTextureView.glReDraw(mGLSurfaceTexture, mIsFirstFrameAvailable);
                            break;
//                        case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
//                            FLog.i("MEDIA_INFO_VIDEO_SEEK_RENDERING_START: " + arg2);
//                            break;
//                        case IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
//                            FLog.i("MEDIA_INFO_AUDIO_SEEK_RENDERING_START: " + arg2);
//                            break;
                        case IMediaPlayer.MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE:
                            FLog.i("MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE: " + arg2);
                            doSeekCompleteListener(mp);
                            break;
                    }
                    return true;
                }
            };

    private void showErrorMsg(String msg) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext, R.style.FDeleteMaterialAlertDialog);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.ve_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).finish();
                }
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void doSeekCompleteListener(IMediaPlayer mp) {
        if (mOnSeekCompleteListener != null) {
            mOnSeekCompleteListener.onSeekComplete(mp);
        }
    }

    @Override
    public boolean isActive() {
        return mIjkMediaPlayer != null &&
                mIsFirstFrameAvailable && isInPlaybackState();
    }

    @Override
    public boolean isPlayMode() {
        return mIsPlayMode;
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
        }
    }

    @Override
    public void suspend() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.pause();
        }
    }

    @Override
    public void resume() {
        if (isInPlaybackState() && isPlaying()) {
            start();
        }
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mIjkMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        if (isInPlaybackState()) {
            return mIjkMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(long realTimeMs) {
//        FLog.i("seekTo realTimeMs >" + realTimeMs + " mCurrentState: " + mCurrentState);
        if (isInPlaybackState()) {
            mIjkMediaPlayer.seekTo(realTimeMs);
        }
    }

    @Override
    public void reset() {
        mCurrentState = STATE_IDLE;
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.reset();
        }
    }

    @Override
    public void release() {
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.release();
            mIjkMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            AudioManager am = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.abandonAudioFocus(null);
            }
        }
        mGLSurfaceTexture.release();
    }

    @Override
    public void setOnSeekCompleteListener(IMediaPlayer.OnSeekCompleteListener seekCompleteListener) {
        mOnSeekCompleteListener = seekCompleteListener;
    }

    @Override
    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener completionListener) {
        mOnCompletionListener = completionListener;
    }

    private boolean isPlaying() {
        return (mIjkMediaPlayer != null &&
                mCurrentState == STATE_PLAYING);
    }

    @Override
    public boolean isInPlaybackState() {
        return (mIjkMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    private void configureIjkPlayer() {
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_ERROR);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);//关闭mediacodec硬解，使用软解
        ///
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 0);//最大缓冲大小,单位kb
//        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);   //是否限制输入缓存数
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "http-detect-range-support", 0);
        ///
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "loop", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "skip-calc-frame-rate", 1);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        if (mSeekAtRealStartTime > 0) {
            mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "seek-at-start", mSeekAtRealStartTime);
        }
        //
        if (!mIsPlayMode) {
            mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "an", 1);
        }
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "render-wait-start", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "video-pictq-size", 3);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 0);
//        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "vf0", "split[a][b];[a]scale=720:1280,crop=x=40:y=320:w=640:h=640,boxblur=10:5[main];[main][b]overlay=(W-w)/2");
    }

    public void setOnFirstFrameListener(OnFirstFrameListener onFirstFrameListener) {
        mOnFirstFrameListener = onFirstFrameListener;
    }

    public interface OnFirstFrameListener {
        void onAvailable();
    }
}
