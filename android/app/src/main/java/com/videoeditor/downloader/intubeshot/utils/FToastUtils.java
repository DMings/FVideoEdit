package com.videoeditor.downloader.intubeshot.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

/**
 * Toast工具类
 */
public class FToastUtils {

    private static Context appContext;

    @IntDef({LENGTH_SHORT, LENGTH_LONG})
    @Retention(RetentionPolicy.SOURCE)
    @interface Duration {
    }

    /**
     * 初始化，尽量在application中
     *
     * @param context c
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * 显示Toast
     *
     * @param message 字符串
     */
    public static void show(String message) {
        show(message, LENGTH_SHORT);
    }

    /**
     * 显示Toast
     *
     * @param messageRes 资源
     */
    public static void show(@StringRes int messageRes) {
        show(appContext.getString(messageRes), LENGTH_SHORT);
    }

    /**
     * 显示Toast
     *
     * @param messageRes 资源
     * @param duration   时长
     */
    public static void show(@StringRes int messageRes, @Duration int duration) {
        show(appContext.getString(messageRes), duration);
    }

    /**
     * 显示Toast
     *
     * @param message  信息
     * @param duration 时长
     */
    @SuppressLint("ShowToast")
    @UiThread
    private static void show(String message, @Duration int duration) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (appContext == null) {
            return;
        }
        Toast.makeText(appContext, message, duration).show();
    }
}
