package com.videoeditor.downloader.intubeshot.loader;

import android.text.TextUtils;

public class FrameKey {

    private String mTag;
    // 所有的Time都是基于startTime的偏移值
    private long mOffsetTime;
    private long mStartTime;

    public FrameKey() {
    }

    public FrameKey(String tag, long startTime, long offsetTime) {
        mTag = tag;
        mStartTime = startTime;
        mOffsetTime = offsetTime;
    }

    public static FrameKey parseName(String name) {
        FrameKey frameKey = new FrameKey();
        if (TextUtils.isEmpty(name)) {
            frameKey.setTag("null");
            frameKey.setOffsetTime(0);
        } else {
            String[] keyValue = name.split("_");
            if (keyValue.length != 2
                    || TextUtils.isEmpty(keyValue[0])
                    || TextUtils.isEmpty(keyValue[1])) {
                frameKey.setTag("null");
                frameKey.setOffsetTime(0);
            } else {
                try {
                    frameKey.setOffsetTime(Long.parseLong(keyValue[1]));
                    frameKey.setTag(keyValue[0]);
                } catch (NumberFormatException | NullPointerException e) {
                    frameKey.setTag("null");
                    frameKey.setOffsetTime(0);
                }
            }
        }
        return frameKey;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        this.mTag = tag;
    }

    public long getOffsetTime() {
        return mOffsetTime;
    }

    public long getRealTime() {
        return mStartTime + getOffsetTime();
    }

    public void setOffsetTime(long offsetTime) {
        this.mOffsetTime = offsetTime;
    }

    public String getName() {
        return mTag + "_" + getRealTime();
    }
}
