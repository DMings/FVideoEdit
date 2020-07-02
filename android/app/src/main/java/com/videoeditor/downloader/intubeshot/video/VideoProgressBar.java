package com.videoeditor.downloader.intubeshot.video;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * @author DMing
 * @date 2020/1/8.
 * description:
 */
public class VideoProgressBar extends ProgressBar {

    private Handler mUiHandler = new Handler();

    public VideoProgressBar(Context context) {
        super(context);
    }

    public VideoProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isShown() {
        return super.isShown();
    }

    private Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            VideoProgressBar.super.setVisibility(VISIBLE);
        }
    };

    @Override
    public void setVisibility(int visibility) {
        if (visibility == VISIBLE) {
            mUiHandler.postDelayed(mShowRunnable, 500);
        } else {
            mUiHandler.removeCallbacks(mShowRunnable);
            super.setVisibility(visibility);
        }
    }
}
