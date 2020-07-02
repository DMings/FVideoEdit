package com.videoeditor.downloader.intubeshot.frame;

import com.videoeditor.downloader.intubeshot.loader.VideoFrame;

/**
 * @author DMing
 * @date 2019/12/25.
 * description:
 */
public interface OnSeekListener {
    void start();

    void seeking(VideoFrame videoFrame, long seekTime);

    void end(VideoFrame videoFrame, long seekTime);
}