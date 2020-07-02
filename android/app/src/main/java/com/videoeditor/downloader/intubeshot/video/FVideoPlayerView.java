package com.videoeditor.downloader.intubeshot.video;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.video.control.VideoControlManager;
import com.videoeditor.downloader.intubeshot.video.gl.FGLTextureView;

public class FVideoPlayerView extends FrameLayout {

    private View mVideoView;
    private VideoControlManager mVideoControlManager;
    private boolean mBackPressed;
    private ImageView mPlayBtn;
    private ImageView mBackPlayBtn;
    private ProgressBar mWaitPB;
    private boolean mIsBackPlayBtnGone = true;

    public FVideoPlayerView(@NonNull Context context) {
        this(context, null);
    }

    public FVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = View.inflate(getContext(), R.layout.layout_preview_video, null);
        addView(view);
        initView();
        initEvent();
    }

    private void initView() {
        mPlayBtn = findViewById(R.id.btn_play);
        mBackPlayBtn = findViewById(R.id.btn_back_start);
        mWaitPB = findViewById(R.id.pb_wait);
        mVideoView = findViewById(R.id.player_view);
        mVideoControlManager = new VideoControlManager((FGLTextureView) mVideoView);
    }

    private void initEvent() {
        mVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoControlManager.isActive()) {
                    mVideoControlManager.actionPause();
                }
            }
        });
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoControlManager.isActive()) {
                    mVideoControlManager.actionPlay();
                }
            }
        });
        mBackPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoControlManager.isActive()) {
                    mVideoControlManager.actionRePlay();
                }

            }
        });
        mVideoControlManager.setOnVideoStatusListener(new VideoControlManager.OnVideoStatusListener() {
            @Override
            public void onPrepare() {
                showWait();
//                FLog.i("onPrepare>>>");
            }

            @Override
            public void onSeek() {
                showWait();
//                FLog.i("onSeek>>>");
            }

            @Override
            public void onPlay() {
                dismissBtnWait();
//                FLog.i("onPlay>>>");
            }

            @Override
            public void onPause() {
                showPlayButton();
//                FLog.i("onPause>>>");
            }

            @Override
            public void onNull() {
                dismissBtnWait();
            }
        });
    }

    public void setVideoMode(int videoMode) {
        mVideoControlManager.setVideoMode(videoMode);
    }

    public int getVideoMode() {
        return mVideoControlManager.getVideoMode();
    }

//    public void seekStart() {
//        mVideoControlManager.actionPause();
//        FLog.i("seekStart--->");
//    }

//    public void seeking(VideoFrame videoFrame, long seekTime) {
    public void seek(VideoFrame videoFrame, long seekTime) {
        if (mVideoControlManager.isActive()) {
            mVideoControlManager.actionSeek(videoFrame, seekTime);
        }else {
            FLog.i("seek isActive:  false");
        }
    }

//    public void seekEnd(VideoFrame videoFrame, long seekTime) {
//        FLog.i("seeking--->");
//        seeking(videoFrame, seekTime);
//    }

    private void showWait() {
        mPlayBtn.setVisibility(View.INVISIBLE);
        mBackPlayBtn.setVisibility(View.INVISIBLE);
        mWaitPB.setVisibility(View.VISIBLE);
    }

    public void showPlayButton() {
        mPlayBtn.setVisibility(View.VISIBLE);
        if (mIsBackPlayBtnGone) {
            mBackPlayBtn.setVisibility(View.INVISIBLE);
        } else {
            mBackPlayBtn.setVisibility(View.VISIBLE);
        }
        mWaitPB.setVisibility(View.INVISIBLE);
    }

    private void dismissBtnWait() {
        mPlayBtn.setVisibility(View.INVISIBLE);
        mBackPlayBtn.setVisibility(View.INVISIBLE);
        mWaitPB.setVisibility(View.INVISIBLE);
    }

    public void setBackPlayBtnGone(boolean isGone) {
        mIsBackPlayBtnGone = isGone;
        showPlayButton();
    }

    public VideoControlManager getVideoControlManager() {
        return mVideoControlManager;
    }

    public void openFromSeek(VideoFrame videoFrame) {
        mVideoControlManager.actionFrameFrame(videoFrame);
    }

    public void pause() {
        mVideoControlManager.actionPause();
    }

    public void onResume() {
        mVideoControlManager.actionResume();
    }

    public void onPause() {
        mVideoControlManager.actionSuspend();
    }

    public void onBackPressed() {
        mBackPressed = true;
    }

    public void onStop() {
        if (mBackPressed) {
            mVideoControlManager.actionRelease();
        }
    }

    public void reset() {
        mVideoControlManager.actionReset();
    }

}
