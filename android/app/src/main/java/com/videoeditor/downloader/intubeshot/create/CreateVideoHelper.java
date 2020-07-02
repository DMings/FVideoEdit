package com.videoeditor.downloader.intubeshot.create;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;
import com.videoeditor.downloader.intubeshot.utils.FLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.videoeditor.downloader.intubeshot.SelectVideoActivity.VIDEO_INFO;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.MESSAGE_PROGRESS;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.MESSAGE_RESULT;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.MESSAGE_START_EDIT;
import static com.videoeditor.downloader.intubeshot.create.CreateVideoService.MESSAGE_STOP_EDIT;

/**
 * @author DMing
 * @date 2020/1/13.
 * description:
 */
public class CreateVideoHelper {

    private Messenger mServiceMessenger;
    private ClientHandler mClientHandler = new ClientHandler(this);
    private Messenger mClientMessenger = new Messenger(mClientHandler);
    private String mVideoQuality;
    private int mVideoMode;
    private CreateVideoCallback mCreateVideoCallback;
    private boolean mIsFinish = false;
    private boolean mIsBound = false;
    private boolean mIsRelease = false;

    public CreateVideoHelper(String videoQuality, int videoMode) {
        mVideoQuality = videoQuality;
        mVideoMode = videoMode;
    }

    public void start(Context context, CreateVideoCallback createVideoCallback) {
        mCreateVideoCallback = createVideoCallback;
        Intent intent = new Intent(context, CreateVideoService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    public void stop(Context context) {
        if (mServiceMessenger != null &&
                mServiceMessenger.getBinder() != null &&
                mServiceMessenger.getBinder().isBinderAlive()) {
            Message message = Message.obtain(mClientHandler, MESSAGE_STOP_EDIT);
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                if (mCreateVideoCallback != null) {
                    mCreateVideoCallback.onEnd(null);
                }
            }
        } else {
            stopService(context);
            if (mCreateVideoCallback != null) {
                mCreateVideoCallback.onEnd(null);
            }
        }
    }

    private void stopService(Context context) {
        if (mIsBound) {
            context.unbindService(mConnection);
            mIsBound = false;
        }
        context.stopService(new Intent(context, CreateVideoService.class));
    }

    public static class ClientHandler extends Handler {

        private WeakReference<CreateVideoHelper> mCreateVideoHelperRef;

        ClientHandler(CreateVideoHelper createVideoHelper) {
            mCreateVideoHelperRef = new WeakReference<>(createVideoHelper);
        }

        @Override
        public void handleMessage(Message msg) {
            CreateVideoHelper createVideoHelper = mCreateVideoHelperRef.get();
            if (createVideoHelper == null) {
                return;
            }
            switch (msg.what) {
                case MESSAGE_PROGRESS:
                    int progress = msg.arg1;
                    if (createVideoHelper.mCreateVideoCallback != null) {
                        createVideoHelper.mCreateVideoCallback.onProgress(progress);
                    }
                    break;
                case MESSAGE_RESULT:
                    VideoInfo videoInfo = null;
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        bundle.setClassLoader(getClass().getClassLoader());
                        videoInfo = bundle.getParcelable(VIDEO_INFO);
                    }
                    if (createVideoHelper.mCreateVideoCallback != null) {
                        createVideoHelper.mCreateVideoCallback.onEnd(videoInfo);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                iBinder.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mServiceMessenger = new Messenger(iBinder);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Message message = Message.obtain(null, MESSAGE_START_EDIT);
                    List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
                    ArrayList<CreateVideoInfo> createVideoInfoList = new ArrayList<>();
                    for (VideoFrame videoFrame : videoFrameList) {
                        if (mIsFinish || mIsRelease) {
                            return;
                        }
                        CreateVideoInfo createVideoInfo = new CreateVideoInfo();
                        createVideoInfo.setWidth(videoFrame.getRotateWidth());
                        createVideoInfo.setHeight(videoFrame.getRotateHeight());
                        createVideoInfo.setStartTime(videoFrame.getStartTime());
                        createVideoInfo.setOffsetDurationMs(videoFrame.getOffsetDurationMs());
                        createVideoInfo.setVideoFilePath(videoFrame.getFile().getAbsolutePath());
                        createVideoInfoList.add(createVideoInfo);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString(CreateVideoService.CREATE_VIDEO_QUALITY, mVideoQuality);
                    bundle.putInt(CreateVideoService.CREATE_VIDEO_SCREEN_MODE, mVideoMode);
                    bundle.putParcelableArrayList(CreateVideoService.CREATE_VIDEO_LIST, createVideoInfoList);
                    message.setData(bundle);
                    message.replyTo = mClientMessenger;
                    try {
                        if (mCreateVideoCallback != null) {
                            mCreateVideoCallback.onStart();
                        }
                        mServiceMessenger.send(message);
                    } catch (RemoteException e) {
                        if (mCreateVideoCallback != null) {
                            mCreateVideoCallback.onEnd(null);
                        }
                    }
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            FLog.e("onServiceDisconnected-->binder died");
            if (!mIsFinish) {
                mIsFinish = true;
                if (mCreateVideoCallback != null) {
                    mCreateVideoCallback.onStop();
                }
            }
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {

        @Override
        public void binderDied() {
            FLog.e("binderDied--> died");
            if (!mIsFinish) {
                mIsFinish = true;
                if (mCreateVideoCallback != null) {
                    mCreateVideoCallback.onStop();
                }
            }
            if (mServiceMessenger == null || mServiceMessenger.getBinder() == null)
                return;
            mServiceMessenger.getBinder().unlinkToDeath(mDeathRecipient, 0);
            mServiceMessenger = null;
        }
    };


    public void release(Context context) {
        mIsRelease = true;
        stopService(context);
    }

}
