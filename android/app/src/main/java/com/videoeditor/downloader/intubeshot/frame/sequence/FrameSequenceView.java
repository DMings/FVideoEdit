package com.videoeditor.downloader.intubeshot.frame.sequence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.frame.OnTitleClickListener;
import com.videoeditor.downloader.intubeshot.frame.edit.EditFrameView;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.video.FVideoPlayerView;
import com.videoeditor.downloader.intubeshot.video.gl.FGLSurfaceTexture;

import java.util.List;

public class FrameSequenceView extends FrameLayout {

    private FrameSequenceRecyclerView mFrameSequenceRecyclerView;
    private OnFrameSequenceChangeListener mOnFrameSequenceChangeListener;
    private EditFrameView mVideoEditFrameView;
    private FVideoPlayerView mFVideoPlayerView;
    //
    private ImageView mIvStrokeScreenIcon;
    private TextView mTvStrokeScreenText;
    private ImageView mIvFullScreenIcon;
    private TextView mTvFullScreenText;
    private ImageView mIvBlurScreenIcon;
    private TextView mTvBlurScreenText;
    private ImageView mIvPadScreenIcon;
    private TextView mTvPadScreenText;

    public FrameSequenceView(@NonNull Context context) {
        this(context, null);
    }

    public FrameSequenceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameSequenceView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_sequence_frame, null);
        addView(view);
        initView(view);
        initEvent(view);
    }

    private void initView(View view) {
        mFrameSequenceRecyclerView = view.findViewById(R.id.fs_rv_frame);
        mVideoEditFrameView = view.findViewById(R.id.v_edit_frame);
        mIvStrokeScreenIcon = view.findViewById(R.id.iv_stroke_screen_icon);
        mTvStrokeScreenText = view.findViewById(R.id.tv_stroke_screen_text);
        mIvFullScreenIcon = view.findViewById(R.id.iv_full_screen_icon);
        mTvFullScreenText = view.findViewById(R.id.tv_full_screen_text);
        mIvBlurScreenIcon = view.findViewById(R.id.iv_blur_screen_icon);
        mTvBlurScreenText = view.findViewById(R.id.tv_blur_screen_text);
        mIvPadScreenIcon = view.findViewById(R.id.iv_pad_screen_icon);
        mTvPadScreenText = view.findViewById(R.id.tv_pad_screen_text);
    }

    private void initEvent(View view) {
        mVideoEditFrameView.setOnTitleClickListener(new OnTitleClickListener() {
            @Override
            public void onCommit() {
                mVideoEditFrameView.setVisibility(View.GONE);
                openFrame(mFVideoPlayerView);
                if (mOnFrameSequenceChangeListener != null) {
                    mOnFrameSequenceChangeListener.onSaveButtonChange(true);
                }
            }

            @Override
            public void onCancel() {
                mVideoEditFrameView.setVisibility(View.GONE);
                if (mOnFrameSequenceChangeListener != null) {
                    mOnFrameSequenceChangeListener.onSaveButtonChange(true);
                }
            }
        });
        view.findViewById(R.id.btn_cut).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
                for (VideoFrame videoFrame : videoFrameList) {
                    if (videoFrame.isCheck()) {
                        mVideoEditFrameView.setVisibility(View.VISIBLE);
                        mVideoEditFrameView.openFrame(mFVideoPlayerView, videoFrame);
                        if (mOnFrameSequenceChangeListener != null) {
                            mOnFrameSequenceChangeListener.onSaveButtonChange(false);
                        }
                        return;
                    }
                }
            }
        });
        view.findViewById(R.id.btn_delete).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
                if (videoFrameList.size() <= 1) {
                    showDialogMsg();
                    return;
                }
                for (int i = 0; i < videoFrameList.size(); i++) {
                    VideoFrame videoFrame = videoFrameList.get(i);
                    if (videoFrame.isCheck()) {
                        if (i == 0) {
                            if (videoFrameList.size() > 1) {
                                videoFrameList.get(1).setIsCheck(true);
                            }
                        } else {
                            videoFrameList.get(i - 1).setIsCheck(true);
                        }
                        videoFrameList.remove(i);
                        break;
                    }
                }
                VideoFrameManager.getInstance().updateVideoFrameListTime();
                openFrame(mFVideoPlayerView);
                for (VideoFrame videoFrame : videoFrameList) {
                    if (videoFrame.isCheck()) {
                        if (mOnFrameSequenceChangeListener != null) {
                            mOnFrameSequenceChangeListener.onFrameChange(videoFrame);
                        }
                        return;
                    }
                }
            }
        });
        mFrameSequenceRecyclerView.setDismissRunnable(new Runnable() {
            @Override
            public void run() {
                if (mOnFrameSequenceChangeListener != null) {
                    mOnFrameSequenceChangeListener.onCommit();
                }
            }
        });
        //
        findViewById(R.id.btn_stroke_screen).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setBtnVideoMode(FGLSurfaceTexture.MODE_STROKE_SCREEN);
            }
        });
        findViewById(R.id.btn_full_screen).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setBtnVideoMode(FGLSurfaceTexture.MODE_FULL_SCREEN);
            }
        });
        findViewById(R.id.btn_blur_screen).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setBtnVideoMode(FGLSurfaceTexture.MODE_BLUR_SCREEN);
            }
        });
        findViewById(R.id.btn_pad_screen).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setBtnVideoMode(FGLSurfaceTexture.MODE_PAD_SCREEN);
            }
        });
        if (mFVideoPlayerView != null) {
            setBtnVideoMode(mFVideoPlayerView.getVideoMode());
        }
    }

    private void setBtnVideoMode(int videoMode) {
        if (mFVideoPlayerView == null) {
            return;
        }
        mFVideoPlayerView.setVideoMode(videoMode);
        if (videoMode == FGLSurfaceTexture.MODE_STROKE_SCREEN) {
            setStrokeEnable(true);
            setFullEnable(false);
            setBlurEnable(false);
            setPadEnable(false);
        } else if (videoMode == FGLSurfaceTexture.MODE_BLUR_SCREEN) {
            setStrokeEnable(false);
            setFullEnable(false);
            setBlurEnable(true);
            setPadEnable(false);
        } else if (videoMode == FGLSurfaceTexture.MODE_PAD_SCREEN) {
            setStrokeEnable(false);
            setFullEnable(false);
            setBlurEnable(false);
            setPadEnable(true);
        } else {
            setStrokeEnable(false);
            setFullEnable(true);
            setBlurEnable(false);
            setPadEnable(false);
        }
    }

    private void setStrokeEnable(boolean enable) {
        if (enable) {
            mIvStrokeScreenIcon.setImageResource(R.drawable.ic_stroke_light_rect);
            mTvStrokeScreenText.setTextColor(getContext().getResources().getColor(R.color.colorAccent));
        } else {
            mIvStrokeScreenIcon.setImageResource(R.drawable.ic_stroke_dark_rect);
            mTvStrokeScreenText.setTextColor(getContext().getResources().getColor(R.color.colorText));
        }
    }

    private void setFullEnable(boolean enable) {
        if (enable) {
            mIvFullScreenIcon.setImageResource(R.drawable.ic_full_light_rect);
            mTvFullScreenText.setTextColor(getContext().getResources().getColor(R.color.colorAccent));
        } else {
            mIvFullScreenIcon.setImageResource(R.drawable.ic_full_dark_rect);
            mTvFullScreenText.setTextColor(getContext().getResources().getColor(R.color.colorText));
        }
    }

    private void setBlurEnable(boolean enable) {
        if (enable) {
            mIvBlurScreenIcon.setImageResource(R.drawable.ic_blur_light_rect);
            mTvBlurScreenText.setTextColor(getContext().getResources().getColor(R.color.colorAccent));
        } else {
            mIvBlurScreenIcon.setImageResource(R.drawable.ic_blur_dark_rect);
            mTvBlurScreenText.setTextColor(getContext().getResources().getColor(R.color.colorText));
        }
    }

    private void setPadEnable(boolean enable) {
        if (enable) {
            mIvPadScreenIcon.setImageResource(R.drawable.ic_pad_light_rect);
            mTvPadScreenText.setTextColor(getContext().getResources().getColor(R.color.colorAccent));
        } else {
            mIvPadScreenIcon.setImageResource(R.drawable.ic_pad_dark_rect);
            mTvPadScreenText.setTextColor(getContext().getResources().getColor(R.color.colorText));
        }
    }

    public void setFrameSequenceChangeListener(OnFrameSequenceChangeListener l) {
        mOnFrameSequenceChangeListener = l;
    }

    public void openFrame(FVideoPlayerView videoPlayerView) {
        mFVideoPlayerView = videoPlayerView;
        videoPlayerView.setBackPlayBtnGone(true);
        mVideoEditFrameView.setFVideoPlayerView(videoPlayerView);
        mFrameSequenceRecyclerView.openFrame(videoPlayerView.getVideoControlManager());
    }

    private void showDialogMsg() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.FMaterialAlertDialog);
        builder.setTitle("错误");
        builder.setMessage("至少有两个以上视频才能删除");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    public interface OnFrameSequenceChangeListener {
        void onCommit();

        void onFrameChange(VideoFrame videoFrame);

        void onSaveButtonChange(boolean visible);
    }

}
