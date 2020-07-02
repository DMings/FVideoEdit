package com.videoeditor.downloader.intubeshot.video.control;

import com.videoeditor.downloader.intubeshot.loader.VideoFrame;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * @author DMing
 * @date 2020/1/18.
 * description:
 */
public interface IVideoControl {

    void setOnFirstFrameListener(VideoControl.OnFirstFrameListener onFirstFrameListener);

    void setOnCompletionListener(IMediaPlayer.OnCompletionListener completionListener);

    void setOnSeekCompleteListener(IMediaPlayer.OnSeekCompleteListener seekCompleteListener);

    boolean isActive();

    long getCurrentPosition();

    void setVideoMode(int videoMode);

    int getVideoMode();

    int getWidth();

    int getHeight();

    long getDurationMs();

    void openVideoFile(VideoFrame videoFrame, long seekAtStart, boolean playMode);

    boolean isInPlaybackState();

    boolean isPlayMode();

    void seekTo(final long realTimeMs);

    void start();

    void pause();

    void suspend();

    void resume();

    void release();

    void reset();

}
