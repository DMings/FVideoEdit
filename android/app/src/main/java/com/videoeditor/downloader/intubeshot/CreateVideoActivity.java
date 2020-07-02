package com.videoeditor.downloader.intubeshot;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videoeditor.downloader.intubeshot.base.BaseActivity;
import com.videoeditor.downloader.intubeshot.create.CreateVideoCallback;
import com.videoeditor.downloader.intubeshot.create.CreateVideoHelper;
import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;
import com.videoeditor.downloader.intubeshot.video.gl.FGLSurfaceTexture;

import static com.videoeditor.downloader.intubeshot.SelectVideoActivity.VIDEO_INFO;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.CREATE_VIDEO_QUALITY;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.CREATE_VIDEO_SCREEN_MODE;

public class CreateVideoActivity extends BaseActivity {

    private MaterialToolbar mMaterialToolbar;
    private TextView mTvProgress;
    private TextView mTvResult;
    private ProgressBar mProgressBar;
    private ProgressBar mErrorProgressBar;
    private CreateVideoHelper mCreateVideoHelper;
    private boolean mIsFinish = true;
    private ValueAnimator mAnimator;
    private Runnable mPendMenuPostRunnable;
    private boolean mIsMenuCreate = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_video);
        mMaterialToolbar = findViewById(R.id.tool_bar);
        mTvProgress = findViewById(R.id.tv_progress);
        mTvResult = findViewById(R.id.tv_result);
        mProgressBar = findViewById(R.id.progress_bar);
        mErrorProgressBar = findViewById(R.id.error_progress_bar);
        initEvent();
        runCreateVideo();
    }

    private void initEvent() {
        mCreateVideoHelper = new CreateVideoHelper(getIntent().getStringExtra(CREATE_VIDEO_QUALITY),
                getIntent().getIntExtra(CREATE_VIDEO_SCREEN_MODE, FGLSurfaceTexture.MODE_STROKE_SCREEN));
        mMaterialToolbar.setNavigationIcon(R.drawable.ic_toolbar_back);
        setNavigationOnClickListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.titlebar_home, menu);
        mIsMenuCreate = true;
        if (mPendMenuPostRunnable != null) {
            mPendMenuPostRunnable.run();
            mPendMenuPostRunnable = null;
        } else {
            mMaterialToolbar.getMenu().findItem(R.id.action_home_item).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_home_item) {
            startActivity(new Intent(this, SelectVideoActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        return true;
    }

    private void setMenuVisible() {
        if (mIsMenuCreate) {
            mMaterialToolbar.getMenu().findItem(R.id.action_home_item).setVisible(true);
        } else {
            mPendMenuPostRunnable = new Runnable() {
                @Override
                public void run() {
                    mMaterialToolbar.getMenu().findItem(R.id.action_home_item).setVisible(true);
                }
            };
        }
    }

    private void setNavigationOnClickListener() {
        mMaterialToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCancelVideoDialog();
            }
        });
    }

    private void setError() {
        stopAnimator();
        mErrorProgressBar.setIndeterminate(false);
        mErrorProgressBar.setVisibility(View.VISIBLE);
        mErrorProgressBar.setProgress(mProgressBar.getProgress());
        mProgressBar.setVisibility(View.INVISIBLE);
        mTvProgress.setText(R.string.ve_chang_video_stop);
    }

    private void setCancel() {
        stopAnimator();
        mErrorProgressBar.setVisibility(View.VISIBLE);
        mErrorProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.INVISIBLE);
        mTvProgress.setText(R.string.ve_continue_cancel);
    }

    private void setSuccess(String filePath) {
        postAnimator(100);
        mErrorProgressBar.setVisibility(View.INVISIBLE);
        mTvProgress.setText(R.string.ve_video_save);
        mTvResult.setVisibility(View.VISIBLE);
        mTvResult.setText(filePath);
        setMenuVisible();
    }

    @SuppressLint("DefaultLocale")
    public void postAnimator(int to) {
        if (mAnimator == null) {
            mAnimator = new ValueAnimator();
            mAnimator.setDuration(55);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int progress = (int) animation.getAnimatedValue();
                    mProgressBar.setProgress(progress);
                    mTvProgress.setText(String.format(getString(R.string.ve_change_video_progress), progress));
                }
            });
        } else {
            mAnimator.cancel();
        }
        mAnimator.setIntValues(mProgressBar.getProgress(), to);
        mAnimator.start();
    }

    public synchronized void stopAnimator() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    @SuppressLint("DefaultLocale")
    private void runCreateVideo() {
        mIsFinish = false;
        mTvProgress.setText(getString(R.string.ve_change_video_ing));
        mProgressBar.setIndeterminate(true);
        mCreateVideoHelper.start(this, new CreateVideoCallback() {

            @Override
            public void onStart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setIndeterminate(false);
                        mProgressBar.setProgress(0);
                        mTvProgress.setText(String.format(getString(R.string.ve_change_video_progress), 0));
                    }
                });
            }

            @Override
            public void onProgress(final int progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        postAnimator(progress);
                    }
                });
            }

            @Override
            public void onEnd(final VideoInfo videoInfo) {
                mIsFinish = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopAnimator();
                        mMaterialToolbar.setNavigationIcon(null);
                        if (getSupportActionBar() == null) {
                            setSupportActionBar(mMaterialToolbar);
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                        setNavigationOnClickListener();
                        if (videoInfo != null) {
                            setResultVideoInfo(videoInfo);
                            setSuccess(videoInfo.getPath());
                        } else {
                            setError();
                        }
                    }
                });
            }

            @Override
            public void onStop() {
                mIsFinish = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopAnimator();
                        finish();
                    }
                });
            }
        });
    }

    private void setResultVideoInfo(VideoInfo videoInfo) {
        setResult(Activity.RESULT_OK, new Intent().putExtra(VIDEO_INFO, videoInfo));
    }

    @Override
    public void onBackPressed() {
        showCancelVideoDialog();
    }

    private void showCancelVideoDialog() {
        if (!mIsFinish) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.FDeleteMaterialAlertDialog);
            builder.setTitle(R.string.ve_warning);
            builder.setMessage(R.string.ve_cancel_video_change);
            builder.setNegativeButton(R.string.ve_cancel, null);
            builder.setPositiveButton(R.string.ve_confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mIsFinish) {
                        finish();
                    } else {
                        setCancel();
                        mCreateVideoHelper.stop(CreateVideoActivity.this);
                    }
                }
            });
            builder.show();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        mCreateVideoHelper.release(this);
        super.onDestroy();
    }
}
