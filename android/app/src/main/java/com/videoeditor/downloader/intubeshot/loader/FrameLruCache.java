package com.videoeditor.downloader.intubeshot.loader;

import android.graphics.Bitmap;
import android.util.LruCache;

import androidx.annotation.Nullable;

public class FrameLruCache {

    private LruCache<String, Bitmap> mLruCache;
    private final static int MAX_SIZE = 60 * 1024 * 1024; // B

    private static volatile FrameLruCache sFrameLruCache;

    public static synchronized FrameLruCache getInstance() {
        if (sFrameLruCache == null) {
            synchronized (FrameLruCache.class) {
                if (sFrameLruCache == null) {
                    sFrameLruCache = new FrameLruCache();
                }
            }
        }
        return sFrameLruCache;
    }

    private FrameLruCache() {
        mLruCache = new LruCache<String, Bitmap>(MAX_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public void putFrame(String key, Bitmap bitmap) {
        mLruCache.put(key, bitmap);
    }

    @Nullable
    public Bitmap getFrame(String key) {
        return mLruCache.get(key);
    }

}
