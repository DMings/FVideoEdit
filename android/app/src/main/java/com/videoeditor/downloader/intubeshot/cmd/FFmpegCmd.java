package com.videoeditor.downloader.intubeshot.cmd;


public class FFmpegCmd {

    static {
        System.loadLibrary("cmd");
    }

    /**
     * cmd 执行，每个参数是一个字符串，传入的是数组
     *
     * @return 0 代表成功，其余是失败，不过一般是没失败的，参数出错直接就给崩溃了
     * （内部触发了一些信号量，暂时找不到相关代码，暂无法改造）
     */
    public static native int execute(String[] commands);

    /**
     * 进度获取是以预估视频合成的时长做前提
     * 假如需要合成的是100s，这里读取的是当前合成的是多少秒，例如3s
     */
    public static native float getProgressTime();

    /**
     * 清除进度时间，置零
     */
    public static native void clearProgressTime();

    /**
     * 强行停止
     */
    public static native void stop();

}
