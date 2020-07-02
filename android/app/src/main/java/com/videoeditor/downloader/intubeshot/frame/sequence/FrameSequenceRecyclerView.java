package com.videoeditor.downloader.intubeshot.frame.sequence;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.video.control.VideoControlManager;

public class FrameSequenceRecyclerView extends RecyclerView {

    private FrameSequenceAdapter mFrameSequenceAdapter;
    private VideoControlManager mVideoControlManager;
    private Runnable mDismissRunnable;

    public FrameSequenceRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public FrameSequenceRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameSequenceRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        setLayoutManager(mLinearLayoutManager);
        mFrameSequenceAdapter = new FrameSequenceAdapter(getContext());
        DragItemTouchCallBack dragItemTouchCallBack = new DragItemTouchCallBack(new DragItemTouchCallBack.DragTouchCallBack() {
            @Override
            public void onItemMove(int fromPosition, int toPosition) {
                mFrameSequenceAdapter.onItemMove(fromPosition, toPosition);
            }
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(dragItemTouchCallBack);
        itemTouchHelper.attachToRecyclerView(this);
        setAdapter(mFrameSequenceAdapter);
        mFrameSequenceAdapter.setOnItemSelectListener(new FrameSequenceAdapter.OnItemSelectListener() {
            @Override
            public void onSelected(VideoFrame videoFrame) {
                if (videoFrame != null) { // 不相同
                    if (mVideoControlManager != null) {
                        mVideoControlManager.actionFrameFrame(videoFrame);
                    }
                } else { // 相同
                    if (mDismissRunnable != null) {
                        mDismissRunnable.run();
                    }
                }
            }
        });
    }

    public void openFrame(VideoControlManager control) {
        mVideoControlManager = control;
        mFrameSequenceAdapter.updateVideoFrameList();
        control.setOnProgressListener(null);// 拦截进度
        control.resetOnPlayEndListener();// 恢复结束判断
    }

    public void setDismissRunnable(Runnable runnable) {
        mDismissRunnable = runnable;
    }

}
