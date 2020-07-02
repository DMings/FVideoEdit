package com.videoeditor.downloader.intubeshot.selector.glide;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

public class FLoaderFactory implements ModelLoaderFactory<VFrame, Bitmap> {
    @NonNull
    @Override
    public ModelLoader<VFrame, Bitmap> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new FModelLoader();
    }

    @Override
    public void teardown() {

    }
}
