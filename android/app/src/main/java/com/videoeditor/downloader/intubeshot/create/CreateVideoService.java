package com.videoeditor.downloader.intubeshot.create;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.videoeditor.downloader.intubeshot.cmd.FFmpegCmd;
import com.videoeditor.downloader.intubeshot.cmd.VideoEditCmd;
import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.video.gl.FGLSurfaceTexture;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class CreateVideoService extends Service {

    public static final String CREATE_VIDEO_LIST = "CREATE_VIDEO_LIST";
    public static final String CREATE_VIDEO_QUALITY = "CREATE_VIDEO_QUALITY";
    public static final String CREATE_VIDEO_SCREEN_MODE = "CREATE_VIDEO_SCREEN_MODE";
    private final static String VIDEO_INFO = "VIDEO_INFO";
    public static final int MESSAGE_START_EDIT = 456;
    public static final int MESSAGE_STOP_EDIT = 678;
    public static final int MESSAGE_PROGRESS = 123;
    public static final int MESSAGE_RESULT = 234;
    private List<File> mFileList;
    private long mWholeTime;
    private long mCurTime;
    private boolean mIsConcatStart = false;
    private boolean mIsFinish = false;

    private static class ServiceHandler extends Handler {

        private final WeakReference<CreateVideoService> mCreateVideoServiceRef;
        private Messenger mClientMessenger;
        private Handler mUIHandler = new Handler();

        ServiceHandler(CreateVideoService createVideoService) {
            mCreateVideoServiceRef = new WeakReference<>(createVideoService);
        }

        @SuppressLint("DefaultLocale")
        private Runnable mUpdateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                CreateVideoService createVideoService = mCreateVideoServiceRef.get();
                if (createVideoService != null) {
                    int progress = createVideoService.getPercentProgress();
//                    FLog.i("update progress: " + progress);
                    if (mClientMessenger != null &&
                            mClientMessenger.getBinder() != null &&
                            mClientMessenger.getBinder().isBinderAlive()) {
                        Message msg = new Message();
                        msg.what = MESSAGE_PROGRESS;
                        msg.arg1 = progress;
                        try {
                            mClientMessenger.send(msg);
                            if (!createVideoService.mIsFinish) {
                                mUIHandler.postDelayed(mUpdateProgressRunnable, 60);
                            }
                        } catch (RemoteException e) {
                            System.exit(-1);
                        }
                    } else {
                        System.exit(-1);
                    }
                } else {
                    System.exit(-1);
                }
            }
        };

        @Override
        public void handleMessage(Message message) {
            CreateVideoService createVideoService = mCreateVideoServiceRef.get();
            switch (message.what) {
                case MESSAGE_START_EDIT:
                    FLog.i("receive message from client: MESSAGE_START_EDIT");
                    synchronized (mCreateVideoServiceRef) {
                        mClientMessenger = message.replyTo;
                        if (createVideoService != null) {
                            if (createVideoService.mIsFinish) {
                                System.exit(-1);
                                return;
                            }
                            ArrayList<CreateVideoInfo> createVideoInfoList = null;
                            String videoQuality = null;
                            int videoMode = 0;
                            if (message.getData() != null) {
                                Bundle bundle = message.getData();
                                bundle.setClassLoader(getClass().getClassLoader());
                                videoQuality = bundle.getString(CREATE_VIDEO_QUALITY);
                                videoMode = bundle.getInt(CREATE_VIDEO_SCREEN_MODE, FGLSurfaceTexture.MODE_STROKE_SCREEN);
                                createVideoInfoList = bundle.getParcelableArrayList(CREATE_VIDEO_LIST);
                            }
                            if (createVideoInfoList == null) {
                                createVideoInfoList = new ArrayList<>();
                            }
                            createVideoService.runCreateVideo(createVideoService, createVideoInfoList,
                                    videoQuality, videoMode, new CreateVideoCallback() {
                                        @Override
                                        public void onStart() {
                                            CreateVideoService createVideoService = mCreateVideoServiceRef.get();
                                            if (createVideoService != null && mClientMessenger != null) {
                                                mUIHandler.postDelayed(mUpdateProgressRunnable, 60);
                                                Message msg = new Message();
                                                msg.what = MESSAGE_PROGRESS;
                                                msg.arg1 = 0;
                                                try {
                                                    mClientMessenger.send(msg);
                                                } catch (RemoteException e) {
                                                    System.exit(-1);
                                                }
                                            } else {
                                                System.exit(-1);
                                            }
                                        }

                                        @Override
                                        public void onProgress(int progress) {
                                            // 这里是无用的
                                        }

                                        @Override
                                        public void onEnd(VideoInfo videoInfo) {
                                            synchronized (mCreateVideoServiceRef) {
                                                CreateVideoService createVideoService = mCreateVideoServiceRef.get();
                                                if (createVideoService != null && mClientMessenger != null) {
                                                    createVideoService.mIsFinish = true;
                                                    mUIHandler.removeCallbacks(mUpdateProgressRunnable);
                                                    Message msg = new Message();
                                                    msg.what = MESSAGE_RESULT;
                                                    Bundle bundle = msg.getData();
                                                    bundle.putParcelable(VIDEO_INFO, videoInfo);
                                                    msg.setData(bundle);
                                                    try {
                                                        mClientMessenger.send(msg);
                                                    } catch (RemoteException e) {
                                                        System.exit(-1);
                                                    }
                                                } else {
                                                    System.exit(-1);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onStop() {
                                            // 这里是无用的
                                        }
                                    });
                        }
                    }
                    break;
                case MESSAGE_STOP_EDIT:
                    FLog.i("receive message from client: MESSAGE_STOP_EDIT");
                    synchronized (mCreateVideoServiceRef) {
                        mUIHandler.removeCallbacks(mUpdateProgressRunnable);
                        if (createVideoService != null && mClientMessenger != null) {
                            if (createVideoService.mIsFinish) {
                                mUIHandler.removeCallbacks(mUpdateProgressRunnable);
                                Message msg = new Message();
                                msg.what = MESSAGE_RESULT;
                                Bundle bundle = msg.getData();
                                bundle.putParcelable(VIDEO_INFO, null);
                                msg.setData(bundle);
                                try {
                                    mClientMessenger.send(msg);
                                } catch (RemoteException e) {
                                    System.exit(-1);
                                }
                            } else {
                                createVideoService.mIsFinish = true;
                                FFmpegCmd.stop();
                            }
                        } else {
                            System.exit(-1);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(new ServiceHandler(this)).getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        FLog.i("onUnbind>>>>");
        return super.onUnbind(intent);
    }

    private void insertVideo(Context context, File videoFile, int w, int h, long durationTimeMs) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, videoFile.getName());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        values.put(MediaStore.Video.Media.SIZE, videoFile.length());
        values.put(MediaStore.Video.Media.DATE_MODIFIED, videoFile.lastModified());
        values.put(MediaStore.Video.Media.WIDTH, w);
        values.put(MediaStore.Video.Media.HEIGHT, h);
        values.put(MediaStore.Video.Media.DURATION, durationTimeMs);
        Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        FLog.i("uri: " + uri);
    }


    public void runCreateVideo(final Context context, final List<CreateVideoInfo> createVideoInfoList,
                               final String videoQuality, final int videoMode,
                               final CreateVideoCallback callback) {
        if (createVideoInfoList == null || createVideoInfoList.size() == 0) {
            callback.onEnd(null);
            return;
        }
        mFileList = new ArrayList<>();
//        long availMenBytes = getAvailMemory(context);
        for (CreateVideoInfo videoFrame : createVideoInfoList) {
            mWholeTime += videoFrame.getOffsetDurationMs();
        }
        mIsFinish = false;
        //                    if (i == 0) {
        //                        targetWidth = curWidth;
        //                        targetHeight = curHeight;
        //                        w = curWidth;
        //                        h = curHeight;
        //                    } else {
        //                        }
        Thread mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mIsConcatStart = false;
                if (callback != null) {
                    callback.onStart();
                }
                int targetWidth = 0;
                int targetHeight = 0;
                VideoEditCmd.clearFileCache(context);
                for (int i = 0; i < createVideoInfoList.size(); i++) {
                    CreateVideoInfo videoFrame = createVideoInfoList.get(i);
                    int curWidth = videoFrame.getRotateWidth();
                    int curHeight = videoFrame.getRotateHeight();
                    FLog.i("videoFrame.getWidth: " + curWidth +
                            " videoFrame.getHeight: " + curHeight);
                    if (i == 0) {
                        if (videoMode == FGLSurfaceTexture.MODE_FULL_SCREEN ||
                                videoMode == FGLSurfaceTexture.MODE_BLUR_SCREEN ||
                                videoMode == FGLSurfaceTexture.MODE_PAD_SCREEN) {
                            if ("1080P".equals(videoQuality)) {
                                targetWidth = 1080;
                                targetHeight = 1080;
                            } else if ("640P".equals(videoQuality)) {
                                targetWidth = 640;
                                targetHeight = 640;
                            } else if ("320P".equals(videoQuality)) {
                                targetWidth = 320;
                                targetHeight = 320;
                            } else {
                                targetWidth = 720;
                                targetHeight = 720;
                            }
                        } else {
                            float r = 1.0f * curWidth / curHeight;
                            int size;
                            if ("1080P".equals(videoQuality)) {
                                size = 1080;
                            } else if ("640P".equals(videoQuality)) {
                                size = 640;
                            } else if ("320P".equals(videoQuality)) {
                                size = 320;
                            } else {
                                size = 720;
                            }
                            if (curWidth > curHeight) {
                                targetHeight = size;
                                targetWidth = (int) (targetHeight * r + 0.5);
                            } else {
                                targetWidth = size;
                                targetHeight = (int) (targetWidth / r + 0.5);
                            }
                        }
                    }
                    Rect rect;
                    if (videoMode == FGLSurfaceTexture.MODE_BLUR_SCREEN ||
                            videoMode == FGLSurfaceTexture.MODE_PAD_SCREEN) {
                        rect = new Rect(0, 0, curWidth, curHeight);
                    } else {
                        rect = CreateVideoUtils.getVideoRect(
                                targetWidth, targetHeight,
                                curWidth, curHeight);
                    }
                    FLog.i("rect: " + rect.toString());
                    File outputFile = VideoEditCmd.changeVideoFormat(context,
                            videoMode,
                            videoFrame.getStartTime(),
                            videoFrame.getOffsetDurationMs(),
                            targetWidth,
                            targetHeight,
                            rect,
                            new File(videoFrame.getFilePath()));
                    synchronized (CreateVideoService.this) {
                        if (mIsFinish) {
                            if (callback != null) {
                                callback.onEnd(null);
                            }
                            return;
                        }
                    }
                    if (outputFile != null) {
                        mFileList.add(outputFile);
                    }
                    synchronized (CreateVideoService.this) {
                        FFmpegCmd.clearProgressTime();
                        mCurTime += videoFrame.getOffsetDurationMs();
                    }
                }
                FLog.i("start concat video!!!");
                synchronized (CreateVideoService.this) {
                    FFmpegCmd.clearProgressTime();
                    mIsConcatStart = true;
                }
                final File outputFile = VideoEditCmd.concatVideo(context, mFileList, targetWidth, targetHeight);
                if (outputFile != null) {
                    insertVideo(context, outputFile, targetWidth, targetHeight, mWholeTime);
                }
                VideoEditCmd.clearFileCache(context);
                synchronized (CreateVideoService.this) {
                    if (mIsFinish) {
                        if (callback != null) {
                            callback.onEnd(null);
                        }
                    } else {
                        if (callback != null) {
                            if (outputFile != null) {
                                VideoInfo info = new VideoInfo();
                                info.setId(-999);
                                info.setPath(outputFile.getAbsolutePath());
                                info.setTitle(outputFile.getName());
                                info.setDuration(mWholeTime);
                                info.setLastModified(outputFile.lastModified());
                                callback.onEnd(info);
                            } else {
                                callback.onEnd(null);
                            }
                        }
                    }
                }
            }
        });
        mThread.start();
    }

    public synchronized int updateCurTime() {
        long curTime = (long) (FFmpegCmd.getProgressTime() * 1000);
        int progress = (int) (100f * (mCurTime + curTime) / mWholeTime);
//        FLog.i("mCurTime: " + mCurTime + " mWholeTime: " + mWholeTime);
        if (!mIsConcatStart) {
            return (int) (progress * 0.92f);
        } else {
            return (int) (92 + 8.0f * curTime / mWholeTime);
        }
    }

    public int getPercentProgress() { // 0-100
        return updateCurTime();
    }

    @Override
    public void onDestroy() {
        FLog.i("service onDestroy>>>>>>>");
        mIsFinish = true;
        super.onDestroy();
    }
}
