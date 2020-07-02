package com.videoeditor.downloader.intubeshot.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * @author DMing
 * @date 2020/1/18.
 * description:
 */
public class MediumBoldTextView extends AppCompatTextView {
    public MediumBoldTextView(Context context) {
        super(context);
    }

    public MediumBoldTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MediumBoldTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TextPaint paint = getPaint();
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        super.onDraw(canvas);
    }
}