package com.videoeditor.downloader.intubeshot.utils;

import android.util.Log;

import java.security.MessageDigest;
import java.text.DecimalFormat;

public class FUtils {

    private static final String slat = "&%5123***&&%%$$#@";

    public static String encrypt(String dataStr) {
        try {
            dataStr = dataStr + slat;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes("UTF8"));
            byte s[] = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "AABBQAQA";
    }


    /**
     * 返回byte的数据大小对应的文本
     */
    public static String getFileSize(long size) {
        FLog.i("getFileSize size: " + size);
        DecimalFormat formater = new DecimalFormat("####.00");
        if (size < 1024) {
            return size + "bytes";
        } else if (size < 1024 * 1024) {
            float kbsize = size / 1024f;
            return formater.format(kbsize) + "KB";
        } else if (size < 1024 * 1024 * 1024) {
            float mbsize = size / 1024f / 1024f;
            return formater.format(mbsize) + "MB";
        } else if (size < 1024 * 1024 * 1024 * 1024L) {
            float gbsize = size / 1024f / 1024f / 1024f;
            return formater.format(gbsize) + "GB";
        } else {
            return "size: error";
        }
    }

    public static String getTimeText(float timeSec) {
        float decimalPart = timeSec - (long) timeSec;
        long second = (long) timeSec;
        long days = second / 86400;//转换天数
        second = second % 86400;//剩余秒数
        long hours = second / 3600;//转换小时数
        second = second % 3600;//剩余秒数
        long minutes = second / 60;//转换分钟
        second = second % 60;//剩余秒数

        String timeText = "";
        timeText += days > 0 ? days + " " : "";
        timeText += hours > 0 ? hours + ":" : "";
        timeText += minutes > 9 ? minutes + ":" : (hours > 0 ? "0" : "") + minutes + ":";
        timeText += second > 9 ? second : "0" + second;
        long mt = (int) (decimalPart * 10);
        timeText += "." + (mt > 9 ? 9 : mt);
        return timeText;
    }

    public static String getTimeText(long timeMs) {
        float timeSec = timeMs * 1.0f / 1000;
        float decimalPart = timeSec - (long) timeSec;
        long second = (long) timeSec;
        long days = second / 86400;//转换天数
        second = second % 86400;//剩余秒数
        long hours = second / 3600;//转换小时数
        second = second % 3600;//剩余秒数
        long minutes = second / 60;//转换分钟
        second = second % 60;//剩余秒数

        String timeText = "";
        timeText += days > 0 ? days + " " : "";
        timeText += hours > 0 ? hours + ":" : "";
        timeText += minutes > 9 ? minutes + ":" : (hours > 0 ? "0" : "") + minutes + ":";
        timeText += second > 9 ? second : "0" + second;
        long mt = (int) (decimalPart * 10);
        timeText += "." + (mt > 9 ? 9 : mt);
        return timeText;
    }

    public static String getSecTimeText(long timeMs) {
        float timeSec = timeMs * 1.0f / 1000;
        float decimalPart = timeSec - (long) timeSec;
        long second = (long) timeSec;
        long days = second / 86400;//转换天数
        second = second % 86400;//剩余秒数
        long hours = second / 3600;//转换小时数
        second = second % 3600;//剩余秒数
        long minutes = second / 60;//转换分钟
        second = second % 60;//剩余秒数

        String timeText = "";
        timeText += days > 0 ? days + " " : "";
        timeText += hours > 0 ? hours + ":" : "";
        timeText += minutes > 9 ? minutes + ":" : (hours > 0 ? "0" : "") + (minutes > 0 ? (minutes + ":") : "");
        timeText += second > 9 ? second : "0" + second;
        long mt = (int) (decimalPart * 10);
        timeText += "." + (mt > 9 ? 9 : mt);
        return timeText;
    }

    public static float getSecDecimal(long timeMs) {
        return ((long) (timeMs * 1.0f / 100)) * 1.0f / 10;
    }

}
