package com.videoeditor.downloader.intubeshot.create;

import android.graphics.Rect;

import com.videoeditor.downloader.intubeshot.utils.FLog;

public class CreateVideoUtils {

    public static Rect getVideoRect(int targetWidth, int targetHeight, int curWidth, int curHeight) {
        int t;
        int l;
        int w;
        int h;
        int mode = 0;
        if (targetHeight >= targetWidth && curWidth > curHeight) {
            h = curHeight;
            float r = 1.0f * targetWidth / targetHeight;
            w = (int) (h * r + 0.5);
            t = 0;
            l = (curWidth - w) / 2;
            mode = 10;
        } else if (targetWidth > targetHeight && curHeight > curWidth) {
            w = curWidth;
            float r = 1.0f * targetHeight / targetWidth;
            h = (int) (r * w + 0.5);
            t = (curHeight - h) / 2;
            l = 0;
            mode = 20;
        } else if (targetWidth > targetHeight && curWidth >= curHeight) {
            float tr = 1.0f * targetWidth / targetHeight;
            float cr = 1.0f * curWidth / curHeight;
            if (cr <= tr) {
                w = curWidth;
                h = (int) (w / tr + 0.5);
                t = (curHeight - h) / 2;
                l = 0;
                mode = 30;
            } else {
                h = curHeight;
                w = (int) (h * tr + 0.5);
                t = 0;
                l = (curWidth - w) / 2;
                mode = 31;
            }
        } else {
            float tr = 1.0f * targetHeight / targetWidth;
            float cr = 1.0f * curHeight / curWidth;
            if (tr <= cr) {
                w = curWidth;
                h = (int) (tr * w + 0.5);
                t = (curHeight - h) / 2;
                l = 0;
                mode = 40;
            } else {
                h = curHeight;
                w = (int) (h / tr + 0.5);
                t = 0;
                l = (curWidth - w) / 2;
                mode = 41;
            }
        }
//        FLog.i("mode: " + mode);
        return new Rect(l, t, l + w, t + h);
    }

}
