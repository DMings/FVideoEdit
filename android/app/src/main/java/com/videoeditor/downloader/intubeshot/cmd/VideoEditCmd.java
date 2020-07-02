package com.videoeditor.downloader.intubeshot.cmd;

import android.content.Context;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.List;

public class VideoEditCmd {

    public static String getCmdTimeText(long timeMs) {
        float timeSec = timeMs * 1.0f / 1000;
        float decimalPart = timeSec - (long) timeSec;
        long second = (long) timeSec;
        second = second % 86400;//剩余秒数
        long hours = second / 3600;//转换小时数
        second = second % 3600;//剩余秒数
        long minutes = second / 60;//转换分钟
        second = second % 60;//剩余秒数

        String timeText = "";
        timeText += hours > 9 ? hours + ":" : (hours > 0 ? "0" : "") + hours + ":";
        timeText += minutes > 9 ? minutes + ":" : (minutes > 0 ? "0" : "") + minutes + ":";
        timeText += second > 9 ? second : (second > 0 ? "0" : "") + second;
        long mt = (int) (decimalPart * 10);
        timeText += "." + (mt > 9 ? 9 : mt);
        return timeText;
    }

    private static String getVideoFileName() {
        Calendar now = Calendar.getInstance();
        return "VE_" + now.get(Calendar.YEAR) +
                getTwoValue(now.get(Calendar.MONTH) + 1) +
                getTwoValue(now.get(Calendar.DAY_OF_MONTH)) +
                getTwoValue(now.get(Calendar.HOUR_OF_DAY)) +
                getTwoValue(now.get(Calendar.MINUTE)) + "_" +
                (now.get(Calendar.SECOND) * 60 + now.get(Calendar.MILLISECOND));
    }

    private static String getTwoValue(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return "" + value;
        }
    }

    public static void deleteFileAll(File file) {
        if (file.exists()) {
            File[] files = file.listFiles();
            int len = files.length;
            for (int i = 0; i < len; i++) {
                if (files[i].isDirectory()) {
                    deleteFileAll(files[i]);
                } else {
                    files[i].delete();
                }
            }
            file.delete();
        }
    }

    public static void clearFileCache(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir != null) {
            deleteFileAll(cacheDir);
        }
    }

    private static File concatVideoFile(Context context, List<File> fileList) {
        File concatFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "concatFile.text");
        try {
            PrintStream stream = new PrintStream(concatFile);
            for (File file : fileList) {
                stream.println("file " + file.getAbsolutePath());
            }
            stream.close();
            return concatFile;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File concatVideo(Context context, List<File> fileList, int w, int h) {
        File concatFile = concatVideoFile(context, fileList);
        if (concatFile != null) {
            Calendar now = Calendar.getInstance();
            now.getTimeInMillis();
            File outputFile = new File(Environment.getExternalStorageDirectory(), "intubeshot/" + getVideoFileName() + ".mp4");
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            String command = "ffmpeg" + " -d -f concat -safe 0 -i " + concatFile + " -y -s " + w + "x" + h + " -codec copy " + outputFile;
            String[] commands = command.split(" ");
            int ret = FFmpegCmd.execute(commands);
            Log.i("DMFF", "concatVideo cmd ret: " + ret);
            return outputFile;
        }
        return null;
    }

    public static File changeVideoFormat(Context context,
                                         int videoMode,
                                         long startTime, long durationTime,
                                         int w, int h,
                                         Rect rect, File file) {

        File outputFile = new File(context.getExternalCacheDir(), "h_" + file.getName());
        String st = getCmdTimeText(startTime);
        String vt = getCmdTimeText(durationTime);

        String filter;
        if (videoMode == 2) { // blur
            int t;
            int l;
            int cropSize;
            String location;
            int overlayWidth;
            int overlayHeight;
            if (rect.width() > rect.height()) {
                t = 0;
                l = (rect.width() - rect.height()) / 2;
                cropSize = rect.height();
                location = "0:(H-h)/2";
                overlayWidth = cropSize;
                overlayHeight = (int) (1.0f * overlayWidth * rect.height() / rect.width());
            } else {
                t = (rect.height() - rect.width()) / 2;
                l = 0;
                cropSize = rect.width();
                location = "(W-w)/2:0";
                overlayHeight = cropSize;
                overlayWidth = (int) (1.0f * overlayHeight * rect.width() / rect.height());
            }
            if (rect.width() == overlayWidth && rect.height() == overlayHeight) {
                filter = "";
                Log.i("DMFF", "same dont split overlay");
            } else {
                filter = " -vf split[m][o];" +
                        "[m]crop=x=" + l + ":y=" + t + ":w=" + cropSize + ":h=" + cropSize + ",boxblur=5:1[main];" +
                        "[o]scale=" + overlayWidth + ":" + overlayHeight + "[over];" +
                        "[main][over]overlay=" + location;
            }
        } else if (videoMode == 3) {
            int maxSize;
            int l = 0;
            int t = 0;
            if (rect.width() > rect.height()) {
                maxSize = rect.width();
                t = (maxSize - rect.height()) / 2;
            } else {
                maxSize = rect.height();
                l = (maxSize - rect.width()) / 2;
            }
            filter = " -vf pad=" + maxSize + ":" + maxSize + ":" + l + ":" + t + ":black";
        } else { // normal
            filter = " -vf crop=" + rect.width() + ":" + rect.height() + ":" + rect.left + ":" + rect.top;
        }

        String command = "ffmpeg -ss " + st + " -t " + vt + " -accurate_seek -i " + file + " -y -s " + w + "x" + h +
//                " -vf rotate=" + rotate +
                filter +
                " -max_muxing_queue_size 4096" +
                " -r 25 -c:v mpeg4 -c:a aac -qscale 6 -avoid_negative_ts 1 " + outputFile;

        String[] commands = command.split(" ");
        int ret = FFmpegCmd.execute(commands);
        Log.i("DMFF", "changeMp4 cmd ret: " + ret);
        return outputFile;
    }

    /**
     * -d 启动调试模式，会有错误提示和很多日志，切勿在生成视频下使用，否则，logcat爆掉崩溃
     * 输出文件必须不为空
     * 命令由空格分隔，注意空格数量
     * 参数输错，程序退出
     */
    public static void test() {
        File file3 = new File(Environment.getExternalStorageDirectory(), "1/test456.mp4");// 源
        File file = new File(Environment.getExternalStorageDirectory(), "1/testblur.mp4");//从源中裁剪的
        File file2 = new File(Environment.getExternalStorageDirectory(), "1/animation.mp4");//第二个视频
        File file4 = new File(Environment.getExternalStorageDirectory(), "1/h264.mp4");//第三个视频
        File gifFile = new File(Environment.getExternalStorageDirectory(), "Download/ggg.gif");//生成的gif
        File outputFile = new File(Environment.getExternalStorageDirectory(), "1/blur999.mp4");//生成的文件
        File png = new File(Environment.getExternalStorageDirectory(), "Download/b.png");//传入的图片

        File fileList = new File(Environment.getExternalStorageDirectory(), "1/fileList.txt");//合并文件
//        if (outputFile.exists()) {
//            outputFile.delete();
//        }
        if (gifFile.exists()) {
            gifFile.delete();
        }
        // 视频合并
        // fileList.txt：
        // file /storage/emulated/0/Download/output2.mp4
        // file /storage/emulated/0/Download/animation.mp4
//        String command = "ffmpeg" + " -f concat -safe 0 -i " + fileList + " -y -codec copy " + outputFile;
//        String command = "ffmpeg" + " -f concat -safe 0 -i " + fileList + " -max_muxing_queue_size 4096 -c:v mpeg4 -c:a aac -qscale 1 " + outputFile;
        // 转gif
//        String command = "ffmpeg -i " +file+ " -ss 0:0:0 -t 10 -s 320x240 -pix_fmt rgb24 " + gifFile;
        // 转图片
//        String command = "ffmpeg -i " +file+ " -f image2 -t 0.1 -s 320x240 /storage/emulated/0/Download/image-%3d.jpg " + outputFile;
        // 视频加水印
//        String command = "ffmpeg -i " + file + " -i " + png + " -filter_complex overlay " + outputFile;
        // 转换帧率
//        String command = "ffmpeg -i " + file + " -r 1 "+ outputFile;
        // 缩小尺寸
//        String command = "ffmpeg -i " + file + " -vf scale=320:-1 "+ outputFile;
        // 旋转
//        String command = "ffmpeg -i " + file + " -metadata:s:v rotate=90 -codec copy " + outputFile;
        // 裁剪
//        String command = "ffmpeg  -ss 00:00:10 -t 00:00:10 -accurate_seek -i " + file3 + " -codec copy -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg  -ss 00:00:10 -t 00:00:10 -accurate_seek -i " + file3 + " -c:v libx264 -c:a aac -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg -ss 00:00:10.500 -t 00:00:10.500 -accurate_seek -i " + file3 + " -r 25 -c:v mpeg4 -c:a aac -qscale 1 -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg -i " + file4 + " -max_muxing_queue_size 4096 -r 25 -c:v mpeg4 -c:a aac -qscale 1 -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg -i " + file + " -y -max_muxing_queue_size 4096 -s 544*960 -r 25 -c:v libx264 -c:a aac -qscale 1 -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg -i " + file + " -y -max_muxing_queue_size 4096 -s 600*600 -vf crop=400:400:0:0 -r 25 -c:v mpeg4 -c:a aac -qscale 1 -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg -d -encoders";

//        String command = "ffmpeg -i " + file3 + " -y -vf split[a][b];[a]pad=2*iw[1];[b]hflip[2];[1][2]overlay=w:0 " + outputFile;
        String command = "ffmpeg -i " + file + " -y -vf split[a][b];[a]scale=720:1280,crop=x=40:y=320:w=640:h=640,boxblur=5:1[main];[main][b]overlay=(W-w)/2 -max_muxing_queue_size 4096" +
                " -r 25 -c:v mpeg4 -c:a aac -qscale 1 -avoid_negative_ts 1 " + outputFile;
//        String command = "ffmpeg -d -i " + file + " -y -vf boxblur=10:5 " + outputFile;
//        String command = "ffmpeg -d -h filter=boxblur";

        String[] commands = command.split(" ");
        int ret = FFmpegCmd.execute(commands);
        Log.i("DMFF", "cmd ret: " + ret);

    }

    public static void test2() {
        File file = new File(Environment.getExternalStorageDirectory(), "1/qudiao.mp4");// 源
        File outputFile = new File(Environment.getExternalStorageDirectory(), "1/wuyinpin.mp4");//生成的文件
        String command = "ffmpeg" + " -i " + file + " -max_muxing_queue_size 4096 -an -c:v mpeg4 -qscale 1 " + outputFile;
        String[] commands = command.split(" ");
        int ret = FFmpegCmd.execute(commands);
        Log.i("DMFF", "cmd ret: " + ret);
    }

    public static void test3() {
        int w = 640;
        int h = 640;
        long startTime = 0;
        long durationTime = 10000;
        String st = getCmdTimeText(startTime);
        String vt = getCmdTimeText(durationTime);

        File file = new File(Environment.getExternalStorageDirectory(), "1/video2.mp4");// 源
        // 960 n 540
        File outputFile = new File(Environment.getExternalStorageDirectory(), "1/recreate.mp4");//生成的文件
        String filter = " -vf pad=960:960:0:210:black";
        String command = "ffmpeg -ss " + st + " -t " + vt + " -accurate_seek -i " + file + " -y -s " + w + "*" + h +
                filter +
                " -max_muxing_queue_size 4096" +
                " -r 25 -c:v mpeg4 -c:a aac -qscale 1 -avoid_negative_ts 1 " + outputFile;
        String[] commands = command.split(" ");
        int ret = FFmpegCmd.execute(commands);
        Log.i("DMFF", "cmd ret: " + ret);
    }

}
