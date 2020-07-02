package com.videoeditor.downloader.intubeshot.frame.edit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.frame.OnSeekListener;
import com.videoeditor.downloader.intubeshot.frame.OnTitleClickListener;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.video.FVideoPlayerView;


public class EditFrameView extends FrameLayout {

    private EditFrameLinearLayout mFrameLL;
    private TabLayout mTLCutMode;
    private OnTitleClickListener mOnTitleClickListener;

    public EditFrameView(@NonNull Context context) {
        this(context, null);
    }

    public EditFrameView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditFrameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_edit_frame, null);
        addView(view);
        mFrameLL = view.findViewById(R.id.ll_frame);
        mTLCutMode = view.findViewById(R.id.tl_cut_mode);
        mTLCutMode.addOnTabSelectedListener(mOnTabSelectedListener);
        view.findViewById(R.id.iv_commit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnTitleClickListener != null) {
                    if (!mFrameLL.changeVideoFrameList()) {
                        showDialogMsg();
                    } else {
                        mOnTitleClickListener.onCommit();
                    }
                }
            }
        });
        view.findViewById(R.id.iv_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnTitleClickListener != null) {
                    mOnTitleClickListener.onCancel();
                }
            }
        });
    }

    private void showDialogMsg() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.FMaterialAlertDialog);
        builder.setTitle(R.string.ve_error);
        builder.setMessage(R.string.ve_cut_must_more_0_5);
        builder.setPositiveButton(R.string.ve_confirm, null);
        builder.show();
    }

    public void setOnTitleClickListener(OnTitleClickListener l) {
        mOnTitleClickListener = l;
    }

    public void setFVideoPlayerView(final FVideoPlayerView videoPlayerView) {
        mFrameLL.setOnSeekListener(new OnSeekListener() {
            @Override
            public void start() {
//                videoPlayerView.seekStart();
            }

            @Override
            public void seeking(VideoFrame videoFrame, long seekTime) {
                videoPlayerView.seek(videoFrame, seekTime);
            }

            @Override
            public void end(VideoFrame videoFrame, long seekTime) {
                videoPlayerView.seek(videoFrame, seekTime);
            }
        });

    }

    public void openFrame(FVideoPlayerView videoPlayerView, VideoFrame videoFrame) {
        TabLayout.Tab tab = mTLCutMode.getTabAt(0);
        if (tab != null) {
            tab.select();
        }
        videoPlayerView.setBackPlayBtnGone(true);
        mFrameLL.setFControl(videoPlayerView.getVideoControlManager());
        mFrameLL.open(videoFrame);
    }

    private TabLayout.OnTabSelectedListener mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (tab.getPosition() == 0) {
                mFrameLL.changeCutMode(EditFrameLinearLayout.MODE_CUT_CENTER);
            } else if (tab.getPosition() == 1) {
                mFrameLL.changeCutMode(EditFrameLinearLayout.MODE_CUT_LR);
            } else {
                mFrameLL.changeCutMode(EditFrameLinearLayout.MODE_CUT_TWO_SIDE);
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == GONE) {
            mFrameLL.reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mTLCutMode.removeOnTabSelectedListener(mOnTabSelectedListener);
        super.onDetachedFromWindow();
    }
}
