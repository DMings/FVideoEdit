package com.videoeditor.downloader.intubeshot.loader;

import java.util.concurrent.Future;

public class RequestTag {

    private String mKey;
    private Future mFuture;

    public RequestTag(String key) {
        this.mKey = key;
    }

    public void setFuture(Future future) {
        this.mFuture = future;
    }

    public Future getFuture() {
        return mFuture;
    }

    public String getKey() {
        return mKey;
    }
}