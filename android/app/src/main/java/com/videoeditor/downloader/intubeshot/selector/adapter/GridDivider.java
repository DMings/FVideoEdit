package com.videoeditor.downloader.intubeshot.selector.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by DMing on 2018/1/18.
 */

public class GridDivider extends RecyclerView.ItemDecoration {

    private int mDividerSize = 2;
    private Paint mPaint;

    public GridDivider(Context context) {
//        final float scale = context.getResources().getDisplayMetrics().density;
//        mDividerSize = (int) (valueDp * scale + 0.5f);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);
        mPaint.setColor(0xFFeeeeee);
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        GridLayoutManager gridLayoutManager = (GridLayoutManager) parent.getLayoutManager();
        if (gridLayoutManager != null) {
            outRect.set(mDividerSize, mDividerSize, mDividerSize, mDividerSize);
        } else {
            outRect.set(mDividerSize, mDividerSize, mDividerSize, mDividerSize);
        }
//        if (parent.getLayoutManager() instanceof GridLayoutManager) {
//            GridLayoutManager gridLayoutManager = (GridLayoutManager) parent.getLayoutManager();
//            int spanCount = gridLayoutManager.getSpanCount();
//            if (spanCount == 3) {
////                int childCount = parent.getAdapter().getItemCount();
//                int itemPosition = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
//                int half = mDividerSize / 4;
//                int spanSize = mDividerSize;
////                if (isLastRaw(itemPosition, spanCount, childCount)) {
////                    spanWidth = mDividerSize;
////                }
//                if (itemPosition > 3){
//                    spanSize = half * 2;
//                }
//                spanSize = half * 2;
//                if (itemPosition % spanCount == 0) {
//                    outRect.set(mDividerSize, spanSize, half, 0);
//                } else if (itemPosition % spanCount == 1) {
//                    outRect.set(half, spanSize, half, 0);
//                } else {
//                    outRect.set(half, spanSize, mDividerSize, 0);
//                }
//            }
//        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
//        super.onDraw(c, parent, state);
//        final int top = parent.getPaddingTop();
//        final int bottom = parent.getHeight() - parent.getPaddingBottom();
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
//            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int left = child.getLeft();
            final int right = child.getRight();
            final int top = child.getTop();
            final int bottom = child.getBottom();
            c.drawRect(left - 1, top - 1, right, bottom, mPaint);
        }
    }

    //    private boolean isLastRaw(int pos, int spanCount,int childCount){
//        int a = childCount % spanCount;//最后的一行多出来几个
//        if (a == 0){ //刚好一行，然后就...
//            a = 3;
//        }
//        childCount = childCount - a;
//        return (pos >= childCount);// 如果是最后一行，则不需要绘制底部
//    }
}