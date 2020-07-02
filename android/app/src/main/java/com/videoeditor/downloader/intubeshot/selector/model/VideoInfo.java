package com.videoeditor.downloader.intubeshot.selector.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by DMing on 2019/12/5.
 */
public class VideoInfo implements Parcelable {

    private long id;
    private String path;
    private Uri uri;
    private String title;
    private long duration;
    private long lastModified;
    //
    private boolean select;

    public VideoInfo(){

    }

    protected VideoInfo(Parcel in) {
        id = in.readLong();
        path = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
        title = in.readString();
        duration = in.readLong();
        lastModified = in.readLong();
        select = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(path);
        dest.writeParcelable(uri, flags);
        dest.writeString(title);
        dest.writeLong(duration);
        dest.writeLong(lastModified);
        dest.writeByte((byte) (select ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoInfo> CREATOR = new Creator<VideoInfo>() {
        @Override
        public VideoInfo createFromParcel(Parcel in) {
            return new VideoInfo(in);
        }

        @Override
        public VideoInfo[] newArray(int size) {
            return new VideoInfo[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    //
    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }
}
