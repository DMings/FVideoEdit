package com.videoeditor.downloader.intubeshot.selector.glide;

public class VFrame {

    private String filePath;
    private int width;
    private int height;
    private long timeMs;

    public VFrame(String filePath) {
        this.filePath = filePath;
    }

    public VFrame(String filePath,long timeMs) {
        this.filePath = filePath;
        this.timeMs = timeMs;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getTimeMs() {
        return timeMs;
    }
}
