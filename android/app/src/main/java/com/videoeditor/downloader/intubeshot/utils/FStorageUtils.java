package com.videoeditor.downloader.intubeshot.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

public class FStorageUtils {

    /**
     * 判断外部储存是否可用
     *
     * @return true : 可用<br>false : 不可用
     */
    public static boolean isExternalStorageEnable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 获取外部储存剩余空间
     *
     * @return 外部储存剩余空间
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static long getExternalStorageFreeSpaceKb() {
        if (!isExternalStorageEnable()) return 0;
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize, availableBlocks;
        availableBlocks = stat.getAvailableBlocksLong();
        blockSize = stat.getBlockSizeLong();
        return availableBlocks * blockSize / 1024; // K
    }

}
