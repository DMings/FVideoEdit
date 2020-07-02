package com.videoeditor.downloader.intubeshot.loader;

import android.content.Context;
import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FrameLoader {

    private static volatile FrameLoader sFrameLoader;

    public static synchronized FrameLoader getInstance() {
        if (sFrameLoader == null) {
            synchronized (FrameLoader.class) {
                if (sFrameLoader == null) {
                    sFrameLoader = new FrameLoader();
                }
            }
        }
        return sFrameLoader;
    }

    private ExecutorService mExecutorService;
    private static Handler sUiHandler;

    private FrameLoader() {
//        Executors.newCachedThreadPool();
//        mExecutorService = new ThreadPoolExecutor(1, 1,
//                0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<Runnable>(),
//                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        mExecutorService = new ThreadPoolExecutor(0, 4,
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
    }

    public static FrameRequest with(Context context) {
        if (sUiHandler == null) {
            sUiHandler = new Handler(context.getMainLooper());
        }
        return new FrameRequest(sUiHandler);
    }

    public static ExecutorService getExecutorService() {
        return getInstance().mExecutorService;
    }

    public static void release() {
        getExecutorService().shutdownNow();
        sFrameLoader = null;
    }

}
