package com.videoeditor.downloader.intubeshot.selector.glide;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

public class FModelLoader implements ModelLoader<VFrame, Bitmap> {
    @Nullable
    @Override
    public LoadData<Bitmap> buildLoadData(@NonNull VFrame model, int width, int height, @NonNull Options options) {
        model.setWidth(width);
        model.setHeight(height);
        return new LoadData<>(new ObjectKey(model.getFilePath() + "-" + model.getTimeMs()), new FDataFetcher(model));
    }

    @Override
    public boolean handles(@NonNull VFrame model) {
        return true;
    }
}
