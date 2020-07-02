package com.videoeditor.downloader.intubeshot.create;

import android.os.Parcel;
import android.os.Parcelable;

public class CreateVideoInfo implements Parcelable {

    private int mWidth;
    private int mHeight;
    private long mStartTime;
    private long mOffsetDurationMs;
    private String mVideoFilePath;

    CreateVideoInfo() {

    }

    protected CreateVideoInfo(Parcel in) {
        mWidth = in.readInt();
        mHeight = in.readInt();
        mStartTime = in.readLong();
        mOffsetDurationMs = in.readLong();
        mVideoFilePath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeLong(mStartTime);
        dest.writeLong(mOffsetDurationMs);
        dest.writeString(mVideoFilePath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CreateVideoInfo> CREATOR = new Creator<CreateVideoInfo>() {
        @Override
        public CreateVideoInfo createFromParcel(Parcel in) {
            return new CreateVideoInfo(in);
        }

        @Override
        public CreateVideoInfo[] newArray(int size) {
            return new CreateVideoInfo[size];
        }
    };

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public void setStartTime(long startTime) {
        mStartTime = startTime;
    }

    public void setOffsetDurationMs(long offsetDurationMs) {
        mOffsetDurationMs = offsetDurationMs;
    }

    public void setVideoFilePath(String videoFilePath) {
        mVideoFilePath = videoFilePath;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public int getRotateWidth() {
        return mWidth;
    }

    public int getRotateHeight() {
        return mHeight;
    }

    public long getOffsetDurationMs() {
        return mOffsetDurationMs;
    }

    public String getFilePath() {
        return mVideoFilePath;
    }
}
