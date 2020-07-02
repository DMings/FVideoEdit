package com.videoeditor.downloader.intubeshot.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.utils.FUtils;
import com.videoeditor.downloader.intubeshot.video.control.IVideoControl;
import com.videoeditor.downloader.intubeshot.video.control.VideoControlManager;
import com.videoeditor.downloader.intubeshot.video.gl.FGLTextureView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author DMing
 * @date 2020/1/7.
 * description:
 */
public class SimpleVideoPlayerView extends FrameLayout {

    private View mVideoView;
    private VideoControlManager mVideoControlManager;
    private boolean mBackPressed;
    private TextView mCreateTimeTv;
    private TextView mVideoInfoTv;
    private TextView mVideoPathTv;

    public SimpleVideoPlayerView(@NonNull Context context) {
        this(context, null);
    }

    public SimpleVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = View.inflate(getContext(), R.layout.layout_simple_video, null);
//        View view = View.inflate(getContext(), R.layout.layout_preview_ijk_video, null);
        addView(view);
        initView();
        initEvent();
    }

    private void initView() {
        mVideoView = findViewById(R.id.player_view);
        mCreateTimeTv = findViewById(R.id.tv_create_time);
        mVideoInfoTv = findViewById(R.id.tv_video_info);
        mVideoPathTv = findViewById(R.id.tv_video_path);
        mVideoView = findViewById(R.id.player_view);
        FGLTextureView fglTextureView = (FGLTextureView) mVideoView;
        fglTextureView.setGLBackgroundColor(getResources().getColor(R.color.colorWhite));
        mVideoControlManager = new VideoControlManager(fglTextureView);
//        mVideoControlManager = new VideoControlManager((IjkVideoView) mVideoView);
    }

    public String getModifiedTime(File file) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        long modifiedTime = file.lastModified();
        Date d = new Date(modifiedTime);
        return format.format(d);
    }

    @SuppressLint("SetTextI18n")
    private void initEvent() {
        mVideoControlManager.setOnVideoStatusListener(new VideoControlManager.OnVideoStatusListener() {
            @Override
            public void onPrepare() {

            }

            @Override
            public void onSeek() {

            }

            @Override
            public void onPlay() {
                IVideoControl videoControl = mVideoControlManager.getVideoControl();
                File file = mVideoControlManager.getVideoFrame().getFile();
                mCreateTimeTv.setText(getContext().getString(R.string.ve_create_time) + getModifiedTime(file));
                if (videoControl.getDurationMs() != 0 && videoControl.getWidth() != 0 && videoControl.getHeight() != 0) {
                    mVideoInfoTv.setVisibility(VISIBLE);
                    mVideoInfoTv.setText(getContext().getString(R.string.ve_file_info) + getContext().getString(R.string.ve_file_resolution) +
                            videoControl.getWidth() + "x" + videoControl.getHeight() +
                            getContext().getString(R.string.ve_file_size) + FUtils.getFileSize(file.length()) +
                            getContext().getString(R.string.ve_file_time) + FUtils.getTimeText(videoControl.getDurationMs()) + "s");
                } else {
                    mVideoInfoTv.setVisibility(GONE);
                }
                mVideoPathTv.setText(getContext().getString(R.string.ve_file_path) + file.getPath());
            }

            @Override
            public void onPause() {

            }

            @Override
            public void onNull() {

            }
        });
    }

    public void openFromPlay(VideoFrame videoFrame) {
        mVideoControlManager.actionPlay(videoFrame);
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

