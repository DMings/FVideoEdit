package com.videoeditor.downloader.intubeshot.selector.glide;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;

import java.io.File;
import java.io.FileNotFoundException;

public class FDataFetcher implements DataFetcher<Bitmap> {

    private final VFrame mModel;

    FDataFetcher(VFrame model) {
        mModel = model;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
        File file = new File(mModel.getFilePath());
        if (file.exists()) {
            VideoFrame videoFrame = new VideoFrame(file);
            if (videoFrame.open(mModel.getWidth(), mModel.getHeight())) {
                Bitmap bitmap = videoFrame.getKeyFrameAtTimeMs(mModel.getTimeMs());
                videoFrame.release();
                callback.onDataReady(bitmap);
            } else {
                videoFrame.release();
                callback.onLoadFailed(new Exception("open failure!"));
            }
        } else {
            callback.onLoadFailed(new FileNotFoundException());
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<Bitmap> getDataClass() {
        return Bitmap.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
