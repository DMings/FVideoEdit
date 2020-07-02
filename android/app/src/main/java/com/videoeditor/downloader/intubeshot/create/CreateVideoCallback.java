package com.videoeditor.downloader.intubeshot.create;

import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;

/**
 * @author DMing
 * @date 2020/1/15.
 * description:
 */
public interface CreateVideoCallback {
    void onStart();

    void onProgress(int progress);

    void onEnd(VideoInfo videoInfo);

    void onStop();
}
