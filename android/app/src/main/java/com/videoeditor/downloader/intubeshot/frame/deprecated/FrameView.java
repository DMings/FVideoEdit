package com.videoeditor.downloader.intubeshot.frame.deprecated;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.videoeditor.downloader.intubeshot.utils.FLog;

@Deprecated
public class FrameView extends AppCompatImageView {

    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Matrix mMatrix = new Matrix();
    private int mDegree;

    public FrameView(Context context) {
        super(context);
    }

    public FrameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FrameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDegree(int degree) {
        mDegree = degree;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        mBitmap = bm;
        invalidate();
        final int width = mBitmap.getWidth();
        final int height = mBitmap.getHeight();
        FLog.i("width: " + width + " height: " + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null) {
            return;
        }
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        final int vWidth = getWidth();
        final int vHeight = getHeight();
        mMatrix.reset();
        float cX = (float) width / 2;
        float cY = (float) height / 2;
        mMatrix.setRotate(mDegree, cX, cY); // 中心点旋转
        //
        float targetX = 0;
        float targetY = 0;
        targetX = -1.0f * (width - vWidth) / 2;
        targetY = -1.0f * (height - vHeight) / 2;
        float centerX = cX + targetX;
        float centerY = cY + targetY;
        mMatrix.postTranslate(targetX, targetY);
        //
        if (mDegree == 90 || mDegree == 270) {
            int t = width;
            width = height;
            height = t;
        }

        float scale;
        float scaleH = 1.0f * vHeight / height;
        float scaleW = 1.0f * vWidth / width;
        if (scaleH > scaleW) {
            scale = 1.0f / scaleW;
        } else {
            scale = 1.0f / scaleH;
        }
        FLog.i("scale: " + scale);
        mMatrix.postScale(scale, scale, centerX, centerY); // 中心点放大
        //


//        float targetX = 0;
//        float targetY = 0;
//        if (mDegree == 90 || mDegree == 270) {
//            if (width < height) {
//                targetX = (float) height / 2 - (float) width / 2;
//                targetY = 0 - targetX;
//            } else {
//                targetY = (float) width / 2 - (float) height / 2;
//                targetX = 0 - targetY;
//            }
//        }
//        mMatrix.postTranslate(targetX, targetY);
//        FLog.i("111 targetX: " + targetX + " targetY: " + targetY);
//        //
//        targetX = -1.0f * (width) / 2;
//        targetY = -1.0f * (height) / 2;
//        mMatrix.postTranslate(targetX, targetY);
//        FLog.i("222 targetX: " + targetX + " targetY: " + targetY);
        //
//        //


        canvas.drawBitmap(mBitmap, mMatrix, mPaint);
    }
}
