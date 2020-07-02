package com.videoeditor.downloader.intubeshot.frame;

import com.videoeditor.downloader.intubeshot.loader.FrameKey;

/**
 * 所有的Time都是基于startTime的偏移值
 */
public class FrameEntity {
    public final static int ITEM_HEADER = 0;
    public final static int ITEM_CONTENT = 1;
    public final static int ITEM_FOOTER = 2;

    private int mType = ITEM_CONTENT;
    private float mPercentSize = 1.0f;
    private FrameKey mFrameKey;
    private String UUID;

    public FrameEntity(int type) {
        mType = type;
    }

    public FrameEntity(String uuid, FrameKey frameKey) {
        UUID = uuid;
        mFrameKey = frameKey;
    }

    public FrameKey getFrameKey() {
        return mFrameKey;
    }

    public String getUUID() {
        return UUID;
    }

    public String getKey() {
        return mFrameKey.getName();
    }

    public long getOffsetTime() {
        return mFrameKey.getOffsetTime();
    }

    public long getRealTime() {
        return mFrameKey.getRealTime();
    }

    public int getType() {
        return mType;
    }

    public float getPercentSize() {
        return mPercentSize;
    }

    public void setPercentSize(float percentSize) {
        mPercentSize = percentSize;
    }
}
