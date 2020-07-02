package com.videoeditor.downloader.intubeshot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videoeditor.downloader.intubeshot.base.BaseActivity;
import com.videoeditor.downloader.intubeshot.frame.FrameRecyclerView;
import com.videoeditor.downloader.intubeshot.frame.OnSeekListener;
import com.videoeditor.downloader.intubeshot.frame.sequence.FrameSequenceView;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.utils.FStorageUtils;
import com.videoeditor.downloader.intubeshot.video.FVideoPlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.videoeditor.downloader.intubeshot.SelectVideoActivity.VIDEO_INFO;
import static com.videoeditor.downloader.intubeshot.SelectVideoActivity.VIDEO_INFO_LIST;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.CREATE_VIDEO_QUALITY;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.CREATE_VIDEO_SCREEN_MODE;


public class VideoEditActivity extends BaseActivity {

    public final static String VIDEO_PATH_LIST = "VIDEO_PATH_LIST";
    private FrameRecyclerView mFrameRv;
    private FVideoPlayerView mFVideoPlayerView;
    private FrameSequenceView mFrameSequenceView;
    private boolean mCanTouchSequence;
    private MaterialToolbar mMaterialToolbar;
    private Runnable mPendMenuPostRunnable;
    private boolean mIsMenuCreate = false;
    private final int REQUEST_CREATE = 888;
    private ArrayList<VideoInfo> mVideoInfoList = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);
        initView();
        initEvent();
    }

    private void initView() {
        mMaterialToolbar = findViewById(R.id.tool_bar);
        mFrameRv = findViewById(R.id.rv_frame);
        mFVideoPlayerView = findViewById(R.id.video_player);
        mFrameSequenceView = findViewById(R.id.fsv_frame);
        setSupportActionBar(mMaterialToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mMaterialToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initEvent() {
        mFrameRv.setOnSeekListener(new OnSeekListener() {
            @Override
            public void start() {
//                mFVideoPlayerView.seekStart();
            }

            @Override
            public void seeking(VideoFrame videoFrame, long seekTime) {
                mFVideoPlayerView.seek(videoFrame, seekTime);
            }

            @Override
            public void end(VideoFrame videoFrame, long seekTime) {
                mFVideoPlayerView.seek(videoFrame, seekTime);
            }
        });
        mFrameRv.setVideoControlManager(mFVideoPlayerView.getVideoControlManager());

        mFrameRv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCanTouchSequence) {
                    mFVideoPlayerView.pause();
                    mFVideoPlayerView.showPlayButton();
                    mFrameSequenceView.setVisibility(View.VISIBLE);
                    mFrameSequenceView.openFrame(mFVideoPlayerView);
                }
            }
        });
        mFrameSequenceView.setFrameSequenceChangeListener(new FrameSequenceView.OnFrameSequenceChangeListener() {
            @Override
            public void onCommit() {
                mFVideoPlayerView.pause();
                mFVideoPlayerView.showPlayButton();
                mFrameSequenceView.setVisibility(View.GONE);
                List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
                mFVideoPlayerView.setBackPlayBtnGone(videoFrameList.size() <= 1);
                mFrameRv.updateFrameSequence(
                        mFVideoPlayerView.getVideoControlManager().getVideoFrame(),
                        mFVideoPlayerView.getVideoControlManager().getCurrentPosition());
                mFrameRv.setVideoControlManager(mFVideoPlayerView.getVideoControlManager());
            }

            @Override
            public void onFrameChange(VideoFrame videoFrame) {
                mFVideoPlayerView.openFromSeek(videoFrame);
            }

            @Override
            public void onSaveButtonChange(boolean visible) {
                setMenuVisible(visible);
            }
        });
        runAction();
    }

    private void runAction() {
        ArrayList<String> filePathList = getIntent().getStringArrayListExtra(VideoEditActivity.VIDEO_PATH_LIST);
        if (filePathList == null) {
            finish();
            return;
        }
        List<File> fileList = new ArrayList<>();
        for (String filePath : filePathList) {
            File file = new File(filePath);
            if (file.exists()) {
                fileList.add(file);
            }
        }
        VideoFrameManager.getInstance().setVideoFileList(fileList);
        mFrameRv.open(new Runnable() {
            @Override
            public void run() {
                VideoFrame videoFrame = VideoFrameManager.getInstance().getFirstVideoFrame();
                mFVideoPlayerView.openFromSeek(videoFrame);
            }
        }, new Runnable() {
            @Override
            public void run() {
                mFVideoPlayerView.setBackPlayBtnGone(
                        VideoFrameManager.getInstance().getVideoFrameList().size() <= 1);
                mCanTouchSequence = true;
                setMenuVisible(true);
            }
        });
    }

    private void setMenuVisible(final boolean visible) {
        if (mIsMenuCreate) {
            mMaterialToolbar.getMenu().findItem(R.id.action_confirm_item).setVisible(visible);
        } else {
            mPendMenuPostRunnable = new Runnable() {
                @Override
                public void run() {
                    mMaterialToolbar.getMenu().findItem(R.id.action_confirm_item).setVisible(visible);
                }
            };
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.titlebar_menu, menu);
        mIsMenuCreate = true;
        if (mPendMenuPostRunnable != null) {
            mPendMenuPostRunnable.run();
            mPendMenuPostRunnable = null;
        } else {
            mMaterialToolbar.getMenu().findItem(R.id.action_confirm_item).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_confirm_item) {
            if (checkFreeStorage()) {
                showVideoQualityDialog();
            } else {
                showErrorMsg();
            }
        }
        return true;
    }

    private boolean checkFreeStorage() {
        boolean b = FStorageUtils.isExternalStorageEnable();
        if (b) {
            long freeKb = FStorageUtils.getExternalStorageFreeSpaceKb();
            long videoSizeKb = 0;
            List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
            for (VideoFrame videoFrame : videoFrameList) {
                videoSizeKb += videoFrame.getFile().length() / 1024;
            }
            long minNeedStorage = (long) (videoSizeKb * 2.2);
            return freeKb >= minNeedStorage;
        }
        return false;
    }

    private void showErrorMsg() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.FDeleteMaterialAlertDialog);
        builder.setTitle(R.string.ve_warning);
        builder.setMessage(R.string.ve_has_no_enough_storage);
        builder.setPositiveButton(R.string.ve_confirm, null);
        builder.show();
    }

    private void showVideoQualityDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.FMaterialAlertDialog);
        builder.setTitle(R.string.ve_create_video_size);
        final String[] items = new String[]{"1080P", getString(R.string.ve_720_recommend), "640P", "360P"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FLog.i("items: " + items[which]);
                startActivityForResult(new Intent(VideoEditActivity.this, CreateVideoActivity.class)
                                .putExtra(CREATE_VIDEO_QUALITY, items[which])
                                .putExtra(CREATE_VIDEO_SCREEN_MODE, mFVideoPlayerView.getVideoMode()),
                        REQUEST_CREATE);
            }
        });
        builder.setNegativeButton(R.string.ve_cancel, null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFVideoPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFVideoPlayerView.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mFVideoPlayerView.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFVideoPlayerView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE) {
            if (data != null && data.getParcelableExtra(VIDEO_INFO) != null) {
                VideoInfo videoInfo = data.getParcelableExtra(VIDEO_INFO);
//                FLog.i("onActivityResult videoInfo: " + videoInfo.getPath());
                mVideoInfoList.add(videoInfo);
                setResult(Activity.RESULT_OK, new Intent().putParcelableArrayListExtra(VIDEO_INFO_LIST, mVideoInfoList));
            }
        }
    }
}
