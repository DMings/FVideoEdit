package com.videoeditor.downloader.intubeshot;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.circularreveal.CircularRevealCompat;
import com.google.android.material.circularreveal.CircularRevealFrameLayout;
import com.google.android.material.circularreveal.CircularRevealWidget;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.videoeditor.downloader.intubeshot.base.BaseActivity;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.selector.PickerManager;
import com.videoeditor.downloader.intubeshot.selector.adapter.GridDivider;
import com.videoeditor.downloader.intubeshot.selector.adapter.VideoAdapter;
import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.utils.FToastUtils;
import com.videoeditor.downloader.intubeshot.video.SimpleVideoPlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

/**
 * Created by DMing on 2019/12/5.
 */
public class SelectVideoActivity extends BaseActivity {

    private VideoAdapter mVideoAdapter;
    private PickerManager mPickerManager;
    private ProgressBar mPbAlbum;
    private RecyclerView mRvAlbum;
    //    private Disposable mImgDisposable;
    private FloatingActionButton mCommitFAB;
    private CircularRevealFrameLayout mCrFrameLayout;
    private DisplayMetrics mDisplayMetrics;
    private MaterialToolbar mMaterialToolbar;
    private Runnable mPendMenuPostRunnable;
    private boolean mIsMenuCreate = false;
    private TextView mTvTitle;
    private Animator mCurAnimator;
    private SimpleVideoPlayerView mSimpleVideoPlayerView;
    //
    private final int REQUEST_EDIT = 666;
    private final int REQUEST_PERMISSION = 456;
    public final static String VIDEO_INFO = "VIDEO_INFO";
    public final static String VIDEO_INFO_LIST = "VIDEO_INFO_LIST";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_video);
        initView();
        boolean hasPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                hasPermission = true;
            }
        } else {
            hasPermission = true;
        }
        if (hasPermission) {
            initEvent();
        }
    }

    private void initView() {
        mMaterialToolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(mMaterialToolbar);
        FToastUtils.init(this);
        mPickerManager = new PickerManager();
        mRvAlbum = findViewById(R.id.rvAlbum);
        mRvAlbum.setLayoutManager(new GridLayoutManager(this, 4));
        mVideoAdapter = new VideoAdapter();
        mRvAlbum.addItemDecoration(new GridDivider(this));
        mRvAlbum.setAdapter(mVideoAdapter);
        mPbAlbum = findViewById(R.id.pbAlbum);
        mCommitFAB = findViewById(R.id.fac_commit);
        mTvTitle = findViewById(R.id.tv_title);
        mCommitFAB.setBackgroundTintList(createColorStateList(
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.floatButtonNormal)));
        mCrFrameLayout = findViewById(R.id.cr_fl);
        mSimpleVideoPlayerView = findViewById(R.id.video_player);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(mDisplayMetrics);
        } else {
            mDisplayMetrics.widthPixels = 720;
            mDisplayMetrics.heightPixels = 1080;
        }
        setVideoCountLayout(0);
        mCommitFAB.setEnabled(false);
    }

    private void initEvent() {
        mVideoAdapter.setOnItemClickListener(new VideoAdapter.OnItemClickListener() {

            @SuppressLint("DefaultLocale")
            @Override
            public void onVideoSizeChange(int size) {
                setVideoCountLayout(size);
            }

            @Override
            public void onItemLongClickDown(View view, VideoInfo videoInfo) {
                viewCircularReveal(view, videoInfo, false);
            }

            @Override
            public void onItemLongClickUp(View view, VideoInfo videoInfo) {
                viewCircularReveal(view, videoInfo, true);
            }
        });
        mRvAlbum.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == SCROLL_STATE_IDLE) {
                    mVideoAdapter.longPressUp();
                }
            }
        });
        mCommitFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(SelectVideoActivity.this, VideoEditActivity.class)
                        .putExtra(VideoEditActivity.VIDEO_PATH_LIST, mVideoAdapter.getSelectVideoList()), REQUEST_EDIT);
            }
        });
        mMaterialToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setVideoCountLayout(0);
                mVideoAdapter.clearSelectVideoList();
            }
        });
        handleVideoInfoList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.titlebar_delete, menu);
        mIsMenuCreate = true;
        if (mPendMenuPostRunnable != null) {
            mPendMenuPostRunnable.run();
            mPendMenuPostRunnable = null;
        } else {
            mMaterialToolbar.getMenu().findItem(R.id.action_delete_item).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_item) {
            showDeleteDialog();
        }
        return true;
    }

    private void setMenuVisible(final boolean visible) {
        if (mIsMenuCreate) {
            mMaterialToolbar.getMenu().findItem(R.id.action_delete_item).setVisible(visible);
        } else {
            mPendMenuPostRunnable = new Runnable() {
                @Override
                public void run() {
                    mMaterialToolbar.getMenu().findItem(R.id.action_delete_item).setVisible(visible);
                }
            };
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void setVideoCountLayout(int count) {
        if (count > 0) {
            mTvTitle.setText(String.format(getString(R.string.ve_has_select), count));
            setMenuVisible(true);
            mMaterialToolbar.setNavigationIcon(R.drawable.ic_toolbar_back);
            mCommitFAB.setEnabled(true);
        } else {
            mTvTitle.setText(R.string.app_name);
            setMenuVisible(false);
            mMaterialToolbar.setNavigationIcon(null);
            mCommitFAB.setEnabled(false);
        }
    }

    private void showDeleteDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.FDeleteMaterialAlertDialog);
        builder.setTitle(R.string.ve_warning);
        builder.setMessage(R.string.ve_delete_tip);
        builder.setNegativeButton(R.string.ve_cancel, null);
        builder.setPositiveButton(R.string.ve_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<String> filePathList = new ArrayList<>(mVideoAdapter.getSelectVideoList());
                mVideoAdapter.deleteSelectVideoList();
                for (String filePath : filePathList) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        if (file.delete()) {
                            FLog.i("has delete");
                        }
                    }
                }
                setVideoCountLayout(0);
            }
        });
        builder.show();
    }

    private void viewCircularReveal(View view, final VideoInfo videoInfo, final boolean isOut) {
        int centerX;
        int centerY;
        if (isOut) {
            centerX = mCrFrameLayout.getRevealInfo() != null ? (int) mCrFrameLayout.getRevealInfo().centerX : mDisplayMetrics.widthPixels / 2;
            centerY = mCrFrameLayout.getRevealInfo() != null ? (int) mCrFrameLayout.getRevealInfo().centerY : mDisplayMetrics.heightPixels / 2;
        } else {
            centerX = (view.getLeft() + view.getRight()) / 2;
            centerY = (view.getTop() + view.getBottom()) / 2 + mMaterialToolbar.getBottom();
        }

        int ty = centerY;
        int by = mDisplayMetrics.heightPixels - centerY;
        int lx = centerX;
        int rx = mDisplayMetrics.widthPixels - centerX;
        int y = Math.max(ty, by);
        int x = Math.max(lx, rx);
        float radius = (float) Math.hypot(y, x);
//        FLog.i("x: " + x + " y: " + y + " radius: " + radius);

        if (mCurAnimator != null) {
            mCurAnimator.cancel();
        }

        CircularRevealWidget.RevealInfo revealInfo = new CircularRevealWidget.RevealInfo(centerX,
                centerY,
                isOut ? radius : 0);
        mCrFrameLayout.setRevealInfo(revealInfo);
        mCurAnimator =
                CircularRevealCompat.createCircularReveal(mCrFrameLayout,
                        revealInfo.centerX,
                        revealInfo.centerY,
                        isOut ? 0 : radius);
        mCurAnimator.setDuration(350);
        mCurAnimator.setInterpolator(new AccelerateInterpolator());
        mCurAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mCrFrameLayout.setVisibility(View.VISIBLE);
                if (!isOut) {
                    mSimpleVideoPlayerView.openFromPlay(new VideoFrame(new File(videoInfo.getPath())));
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isOut) {
                    mCrFrameLayout.setVisibility(View.INVISIBLE);
                    mSimpleVideoPlayerView.reset();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (isOut) {
                    mCrFrameLayout.setVisibility(View.INVISIBLE);
                    mSimpleVideoPlayerView.reset();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mCurAnimator.start();
    }

    private ColorStateList createColorStateList(int normal, int unable) {
        int[] colors = new int[]{normal, unable};
        int[][] states = new int[2][];
        states[0] = new int[]{android.R.attr.state_enabled};
        states[1] = new int[]{};
        return new ColorStateList(states, colors);
    }

    private void handleVideoInfoList() {
//        mImgDisposable = mPickerManager.getImageList(this)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<List<VideoInfo>>() {
//                    @Override
//                    public void accept(final List<VideoInfo> albumList) throws Throwable {
//                        mPbAlbum.setVisibility(View.GONE);
//                        mVideoAdapter.setVideoList(albumList);
//                    }
//                }, new Consumer<Throwable>() {
//                    @Override
//                    public void accept(Throwable throwable) throws Throwable {
//                        FLog.i("loading throwable>>>" + throwable);
//                        mPbAlbum.setVisibility(View.GONE);
//                    }
//                });
        mPickerManager.handleImageList(this, new PickerManager.OnDataAvailableListener() {
            @Override
            public void onAvailable(final List<VideoInfo> mVideoList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ((!SelectVideoActivity.this.isFinishing()) && (!SelectVideoActivity.this.isDestroyed())) {
                            mPbAlbum.setVisibility(View.GONE);
                            mVideoAdapter.setVideoList(mVideoList);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT) {
            if (data != null && data.getParcelableArrayListExtra(VIDEO_INFO_LIST) != null) {
                ArrayList<VideoInfo> videoInfoList = data.getParcelableArrayListExtra(VIDEO_INFO_LIST);
//                FLog.i("onActivityResult ArrayList<>: " + videoInfoList.size());
                mVideoAdapter.addVideoList(videoInfoList);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int perIndex = 0;
        if (requestCode == REQUEST_PERMISSION) {
            boolean hasPermission = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    hasPermission = false;
                    break;
                }
                perIndex++;
            }
            if (hasPermission) {
                initEvent();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean b = shouldShowRequestPermissionRationale(perIndex == 0 ? Manifest.permission.READ_EXTERNAL_STORAGE :
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    String msg;
                    if (b) {
                        msg = getString(R.string.ve_show_request_permission);
                    } else {
                        msg = getString(R.string.ve_show_request_never_permission);
                    }
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.FDeleteMaterialAlertDialog);
                    builder.setTitle(R.string.ve_warning);
                    builder.setMessage(msg);
                    builder.setPositiveButton(R.string.ve_quit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    builder.setCancelable(false);
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSimpleVideoPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSimpleVideoPlayerView.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mSimpleVideoPlayerView.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSimpleVideoPlayerView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mImgDisposable != null) {
//            mImgDisposable.dispose();
//        }
    }
}
