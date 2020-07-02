package com.videoeditor.downloader.intubeshot.loader;

import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;

import androidx.annotation.UiThread;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

public class FrameRequest {

    private FrameKey mFrameKey;
    private Handler mUiHandler;

    @UiThread
    FrameRequest(Handler uiHandler) {
        mUiHandler = uiHandler;
    }

    @UiThread
    public FrameRequest load(FrameKey frameKey) {
        mFrameKey = frameKey;
        return this;
    }

    @UiThread
    public void into(ImageView imageView) {
//        FLog.i("<<<FrameRequest>>>"+ mFrameKey.getName());
        // 如果之前有请求就清除一下
        clearRequest(imageView);
        // 在缓存中找到了，就不必进入线程池了
//        Bitmap bitmap = FrameLruCache.getInstance().getFrame(mFrameKey.getName());
//        if (bitmap != null) {
//            imageView.setImageBitmap(bitmap);
//            return;
//        }
        imageView.setImageDrawable(null);
//        imageView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
//            @Override
//            public void onViewAttachedToWindow(View v) {
////                FLog.i("<<<onViewAttachedToWindow"+ mFrameKey.getName());
//            }
//
//            @Override
//            public void onViewDetachedFromWindow(View v) {
//                FLog.i("setImageBitmap DetachedFromWindow>>>"+ mFrameKey.getName());
//                v.removeOnAttachStateChangeListener(this);
//                // 离开也清除
//                clearRequest((ImageView) v);
//            }
//        });
        runRequest(imageView);
    }

    @UiThread
    private void runRequest(ImageView imageView) {
        RequestTag requestTag = new RequestTag(mFrameKey.getName());
        imageView.setTag(requestTag);
        Future future = FrameLoader.getExecutorService().submit(new FrameRunnable(mUiHandler, imageView, mFrameKey));
        requestTag.setFuture(future);
    }

    @UiThread
    private void clearRequest(ImageView imageView) {
        RequestTag requestTag = getFrameRequest(imageView);
        if (requestTag != null && requestTag.getFuture() != null) {
            requestTag.getFuture().cancel(false);
        }
        imageView.setTag(null);
    }

    @UiThread
    private static RequestTag getFrameRequest(ImageView imageView) {
        Object tag = imageView.getTag();
        if (tag instanceof RequestTag) {
            return (RequestTag) tag;
        }
        return null;
    }

    public static class FrameRunnable implements Runnable {

        private WeakReference<ImageView> mImageViewReference;
        private Handler mUiHandler;
        private FrameKey mFrameKey;

        FrameRunnable(Handler uiHandler, ImageView imageView, FrameKey frameKey) {
            mUiHandler = uiHandler;
            mImageViewReference = new WeakReference<>(imageView);
            mFrameKey = frameKey;
        }

        @Override
        public void run() {
//            FLog.i("FrameRunnable-->" + mFrameKey.getName());
            // 先从缓存中找
            Bitmap bitmap = FrameLruCache.getInstance().getFrame(mFrameKey.getName());
            if (bitmap == null) {
                // 找不到请求seek
                bitmap = VideoFrameManager.getInstance().getFrame(mFrameKey.getName(), mFrameKey.getRealTime());
//                FLog.i("FrameRunnable-- seek ->bitmap " + bitmap + " -> " + mFrameKey.getName());
            }
            FrameLruCache.getInstance().putFrame(mFrameKey.getName(), bitmap);
            final Bitmap finalBp = bitmap;
            if (mImageViewReference.get() != null) {
                final RequestTag requestTag = getFrameRequest(mImageViewReference.get());
                // 判断当前View的key是不是请求的key，防止错乱
                if (requestTag != null && requestTag.getKey().equals(mFrameKey.getName())) {
//                FLog.i("setImageBitmap post->" + " Time: " + mFrameKey.getOffsetTime() +
//                        "  requestTag.getKey: " + requestTag.getKey());
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
//                        FLog.e("setImageBitmap post+>run=" + " Time: " + mFrameKey.getOffsetTime() +
//                                "  requestTag.getKey: " + requestTag.getKey());
                            if (mImageViewReference.get() != null) {
                                mImageViewReference.get().setImageBitmap(finalBp);
                            }
                        }
                    });
                } else {
//                if (requestTag != null) {
//                    FLog.i("setImageBitmap update er " + bitmap + " Time: " + mFrameKey.getOffsetTime() +
//                            "  requestTag.getKey: " + requestTag.getKey());
//                } else {
//                    FLog.e("setImageBitmap update er " + bitmap + " Time: " + mFrameKey.getOffsetTime() +
//                            "  requestTag.getKey: nll");
//                }
                }
            }
        }
    }

}
