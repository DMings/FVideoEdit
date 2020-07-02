package com.videoeditor.downloader.intubeshot.utils;

import android.util.Log;

import com.videoeditor.downloader.intubeshot.BuildConfig;

/**
 *
 * Created by DMing on 2017/9/19.
 */

public class FLog {

    private static String TAG = "DMFF";

    public static void i(String msg){
        if (BuildConfig.DEBUG) Log.i(TAG,msg);
    }

    public static void e(String msg){
        if (BuildConfig.DEBUG) Log.e(TAG,msg);
    }
}
