package com.videoeditor.downloader.intubeshot.frame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.utils.FUtils;
import com.videoeditor.downloader.intubeshot.video.control.VideoControlManager;

import java.io.File;
import java.util.List;

public class FrameRecyclerView extends RecyclerView {

    private Paint mPaint;
    private int mLineHalfWidth;
    private int mLinePadding;
    private RectF mLineRectF;
    private int mProgressTextWidth;
    private int mTotalTextWidth;
    private String mProgressText;
    private String mTotalText;
    private boolean mIsTouch;
    // 颗粒度为0.1s
    private long mCurOffsetTime;
    // 总的时间
    private long mCurTime;
    private long mSeekTime = -1;
    private OnSeekListener mOnSeekListener;
    private VideoFrameManager mVideoFrameManager;
    private FrameAdapter mFrameAdapter;
    private Handler mHandler;
    private int mPaddingWidth;
    //
    private OnClickListener mOnClickListener;
    //
    private VideoControlManager mVideoControlManager;

    public FrameRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public FrameRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mLineRectF = new RectF();
        mPaint = new Paint();
        mLineHalfWidth = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3,
                this.getResources().getDisplayMetrics()) / 2);
        mLinePadding = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3,
                this.getResources().getDisplayMetrics()));
        mPaddingWidth = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                this.getResources().getDisplayMetrics()));
        float textHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8,
                this.getResources().getDisplayMetrics());
        mPaint.setTextSize(textHeight);

        mProgressText = FUtils.getTimeText(0.0f);
        mTotalText = FUtils.getTimeText(0.0f);

        mProgressTextWidth = (int) (mPaint.measureText(mProgressText) / 2);
        mTotalTextWidth = (int) (mPaint.measureText(mProgressText));
        mPaint.setFakeBoldText(true);
        mPaint.setAntiAlias(true);
        //
        setItemViewCacheSize(3);
        getRecycledViewPool().setMaxRecycledViews(FrameEntity.ITEM_FOOTER, 1);
        getRecycledViewPool().setMaxRecycledViews(FrameEntity.ITEM_HEADER, 1);
        mFrameAdapter = new FrameAdapter(getContext());
        setAdapter(mFrameAdapter);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        setLayoutManager(mLinearLayoutManager);
    }

    private void initData() {
        mHandler = new Handler();
        mVideoFrameManager = VideoFrameManager.getInstance();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                FLog.i("FrameRecyclerView: " + event.getAction());
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mIsTouch = true;
                    mOnSeekListener.start();
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_POINTER_UP) {
                    mIsTouch = false;
                }
                return gestureDetector.onTouchEvent(event);
            }
        });
        addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                VideoFrame videoFrame = computeFocusFrame();
                if (videoFrame == null) {
                    return;
                }
                mCurTime = videoFrame.getPreVideoTime() + mCurOffsetTime;
                setProgressText(FUtils.getSecDecimal(mCurTime));
                if (newState == SCROLL_STATE_IDLE) {
                    mIsTouch = false;
                }
                boolean enableTouch = false;
                if (mVideoControlManager != null) { // play状态touch是false的
                    enableTouch = !mVideoControlManager.isPlayMode(); // 不是play就是帧状态就是用touch，play enableTouch是false
                } // 如果是帧状态就要判断是否在触摸中，因为这个结束只要scroll调用就会空闲
                if (enableTouch && newState == SCROLL_STATE_IDLE) { // 惯性滚动结束
                    FLog.i("=========onScrollStateChanged SCROLL_STATE_IDLE=========");
                    if (mOnSeekListener != null) {
                        long seekTime = (long) (FUtils.getSecDecimal(mCurOffsetTime) * 1000);
                        if (mSeekTime != seekTime) {
                            mSeekTime = seekTime;
//                            FLog.i("onScrolled end>mSeekTime: " + mSeekTime);
                            mOnSeekListener.seeking(videoFrame, mCurOffsetTime);
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                VideoFrame videoFrame = computeFocusFrame();
                if (videoFrame == null) {
                    return;
                }
                mCurTime = videoFrame.getPreVideoTime() + mCurOffsetTime;
                setProgressText(FUtils.getSecDecimal(mCurTime));
                if (mIsTouch) {
                    if (mOnSeekListener != null) {
                        long seekTime = (long) (FUtils.getSecDecimal(mCurOffsetTime) * 1000);
                        if (mSeekTime != seekTime) {
                            mSeekTime = seekTime;
//                            FLog.i("onScrolled>mSeekTime: " + mSeekTime);
                            mOnSeekListener.seeking(videoFrame, mCurOffsetTime);
                        }
                    }
                }
            }
        });
    }

    private GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(FrameRecyclerView.this);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    });

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    private VideoFrame computeFocusFrame() {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
        FrameAdapter frameAdapter = (FrameAdapter) getAdapter();
        if (linearLayoutManager == null || frameAdapter == null) {
            return null;
        }
        if (frameAdapter.getItemCount() <= 2) {
            return null;
        }
        int firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
        View view = linearLayoutManager.findViewByPosition(firstPosition);
        if (view == null) {
            return null;
        }
        // 当前view的视频整体时间偏移值
        long curOffsetTime;
        // 标记没有下一个正常的view
        boolean isEnd = false;
        // view的时间偏移值
        int offsetTime;
        // 宽度百分比
        float perPercentWidth;
        //
        float firstWidth;
        // 当前view的位置
        int pos;
        // 当前view的前一个位置
        int prePos;
        // 当前view的宽度偏移
        float x = 0;
        // 当前view的前一个宽度偏移
        float preX = 0;
        //
        FrameEntity frameEntity;
        if (firstPosition > 0) {
            firstWidth = getFrameItemWidth(frameAdapter, firstPosition) + view.getLeft();// 屏幕上第一个的宽度
            pos = firstPosition;
            prePos = firstPosition;
            x = firstWidth;
            preX = firstWidth;
            // 不断计算，计算出到达中线的view,pos就是该view的索引
            while (x <= frameAdapter.getHFWidth()) {
                prePos = pos;
                pos++;
                preX = x;
                // pos不断++比较危险，做一下边界判断
                if (checkEnd(frameAdapter, pos)) {
                    isEnd = true;
                    break;
                }
                x += getFrameItemWidth(frameAdapter, pos);
            }
            if (!isEnd) {
                // 计算中间先距离未去到中间线view的距离，
                // 得到的距离就是这个中间view的偏移值
                // 这个偏移值就是当前的时间！！！
                perPercentWidth = 1.0f * (frameAdapter.getHFWidth() - preX) /
                        getFrameItemWidth(frameAdapter, pos);
                offsetTime = (int) (perPercentWidth * getFrameItemTimeSpan(frameAdapter, pos));
                curOffsetTime = getFrameItemTime(frameAdapter, pos) + offsetTime;
                frameEntity = frameAdapter.getFrameEntity(pos);
            } else {
                offsetTime = getFrameItemTimeSpan(frameAdapter, prePos);
                curOffsetTime = getFrameItemTime(frameAdapter, prePos) + offsetTime;
                frameEntity = frameAdapter.getFrameEntity(prePos);
            }
        } else {
            firstWidth = -view.getLeft();
            pos = 0;
            prePos = 0;
            while (x <= firstWidth) {
                prePos = pos;
                pos++;
                preX = x;
                // pos不断++比较危险，做一下边界判断
                if (checkEnd(frameAdapter, pos)) {
                    isEnd = true;
                    break;
                }
                x += getFrameItemWidth(frameAdapter, pos);
            }
            if (!isEnd) {
                // 滑动到第二个view以上，第一个view是透明，作为偏移的头
                // 利用偏移头（即是屏幕的一般）减去中间线前面的一个view，就可以知道时间的偏移
                // 中间线的那个view的起始距离中间线的百分比宽度
                perPercentWidth = 1.0f * (firstWidth - preX) /
                        getFrameItemWidth(frameAdapter, pos);
                // 中间线的那个view本身的时间间隔 * 百分比宽度，得出自身的时间偏移
                offsetTime = (int) (perPercentWidth * getFrameItemTimeSpan(frameAdapter, pos));
                // 中间线的那个view本身的起始时间 + 自身的时间偏移，就是当前的时间
                curOffsetTime = getFrameItemTime(frameAdapter, pos) + offsetTime;
                // 中间线的那个view对应的数组中的实体
                frameEntity = frameAdapter.getFrameEntity(pos);
            } else {
                // 未滑动过以第一个view
                // 利用第一个view的偏移，就知道整体时间的滑动情况
                offsetTime = getFrameItemTimeSpan(frameAdapter, prePos);
                curOffsetTime = getFrameItemTime(frameAdapter, prePos) + offsetTime;
                frameEntity = frameAdapter.getFrameEntity(prePos);
            }
        }
//              FLog.i("offsetTime: " + offsetTime + " curOffsetTime: " + curOffsetTime + " isEnd: " + isEnd);
        mCurOffsetTime = curOffsetTime;
        if (frameEntity == null) {
            return null;
        }
        return mVideoFrameManager.getVideoFrame(frameEntity.getUUID());
    }

    private boolean checkEnd(FrameAdapter frameAdapter, int pos) {
        if (pos >= frameAdapter.getItemCount()) {
            return true;
        }
        return frameAdapter.getFrameEntity(pos).getType() != FrameEntity.ITEM_CONTENT;
    }

    public int getFrameItemWidth(FrameAdapter frameAdapter, int pos) {
        return (int) (frameAdapter.getFrameEntity(pos).getPercentSize() * frameAdapter.getItemWidth());
    }

    public int getFrameItemTimeSpan(FrameAdapter frameAdapter, int pos) {
        return (int) (frameAdapter.getFrameEntity(pos).getPercentSize() * VideoFrameManager.TIME_SPAN);
    }

    public long getFrameItemTime(FrameAdapter frameAdapter, int pos) {
        return frameAdapter.getFrameEntity(pos).getOffsetTime();
    }

    public void setVideoControlManager(VideoControlManager videoControlManager) {
        mVideoControlManager = videoControlManager;
        videoControlManager.setOnProgressListener(new VideoControlManager.OnProgressListener() {
            @Override
            public void onProgress(VideoFrame videoFrame, long offsetTime) {
                float toScrollX = getFrameScrollX(videoFrame.getPreVideoTime() + offsetTime);
                int offsetX = (int) (toScrollX - getFrameScrollX(mCurTime) + 0.5);
//                FLog.i("mCurTime: " + mCurTime + " time: " + time + " mScrollX: " + getFrameScrollX(mCurTime) + "  toScrollX: " + toScrollX + "  offsetX: " + offsetX);
                smoothScrollBy(offsetX, 0);
            }
        });
        videoControlManager.setOnPlayEndListener(new VideoControlManager.OnPlayEndListener() {
            @Override
            public long onStartTime(VideoFrame videoFrame) {
                return 0;
            }

            @Override
            public VideoControlManager.PlayNext onEndNext(VideoFrame videoFrame, long realTime) {
                if (realTime >= videoFrame.getEndTime()) {
                    VideoFrame nextVideoFrame = VideoFrameManager.getInstance().getNextVideoFrame(videoFrame.getUUID());
                    if (nextVideoFrame == null) { // 没有下一个了，视频结束了
                        if (mVideoControlManager.isPlaying()) {
                            progressPostEnd();
                        }
                    }
                    return new VideoControlManager.PlayNext(nextVideoFrame, 0);
                }
                return null; // 正常情况，不是结束状态
            }
        });
    }

    public void progressPostEnd() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mFrameAdapter.getItemCount() > 0) {
                    smoothScrollToPosition(mFrameAdapter.getItemCount() - 1);
                }
            }
        });
    }

    public void open(final Runnable onFirstVideoFrameAvailable, final Runnable onEndFrame) {
        mVideoFrameManager.openFrameList(new VideoFrameManager.OnFrameListener() {
            @Override
            public void start(VideoFrame videoFrame) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFrameAdapter.clear();
                        mFrameAdapter.addHeader();
                        onFirstVideoFrameAvailable.run();
                    }
                });
            }

            @Override
            public void onFrameChange(final VideoFrame videoFrame) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFrameAdapter.addVideoFrameList(videoFrame.getFrameEntityList());
                        float totalTime = FUtils.getSecDecimal(mVideoFrameManager.getVideoTimeMs());
                        setTotalText(totalTime);
                        invalidate();
                    }
                });
            }

            @Override
            public void end(final List<File> fileList) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFrameAdapter.addFooter();
                        onEndFrame.run();
                        showVideoImportFailDialog(fileList);
                    }
                });
            }
        });
    }

    private void showVideoImportFailDialog(List<File> fileList) {
        if (fileList.size() > 0) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.FMaterialAlertDialog);
            builder.setTitle(String.format(getContext().getString(R.string.ve_import_video_failed), fileList.size()));
            final String[] items = new String[fileList.size()];
            for (int i = 0; i < fileList.size(); i++) {
                items[i] = i + 1 + "." + fileList.get(i).getName();
            }
            builder.setItems(items, null);
            builder.setPositiveButton(R.string.ve_confirm, null);
            builder.show();
        }
    }

    public void updateFrameSequence(VideoFrame videoFrame, long realTime) {
        mFrameAdapter.updateFrameSequence(mVideoFrameManager.getAllFrameEntityList());
        if (videoFrame != null) {
//            FLog.i("realTime: " + realTime + " videoFrame: " + videoFrame.getPreVideoTime());
            int toScrollX = (int) getFrameScrollX(videoFrame.getPreVideoTime() + videoFrame.getOffsetTime(realTime));
            scrollToPosition(0);
            smoothScrollBy(toScrollX, 0);
        }
        float totalTime = FUtils.getSecDecimal(mVideoFrameManager.getVideoTimeMs());
        setTotalText(totalTime);
        invalidate();
    }

    public float getMsWidth() {
        return 1.0f * mFrameAdapter.getItemWidth() / VideoFrameManager.TIME_SPAN;
    }

    public float getFrameScrollX(long time) {
        return time * getMsWidth();
    }

    public void setOnSeekListener(OnSeekListener onSeekListener) {
        mOnSeekListener = onSeekListener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int top = getPaddingTop() - mLinePadding;
        int bottom = getPaddingBottom() - mLinePadding;
        mLineRectF.set(getWidth() / 2 - mLineHalfWidth,
                top, getWidth() / 2 + mLineHalfWidth, getHeight() - bottom);
    }

    public void setProgressText(float timeSec) {
        mProgressText = FUtils.getTimeText(timeSec);
        mProgressTextWidth = (int) (mPaint.measureText(mProgressText) / 2);
    }

    public void setTotalText(float timeSec) {
        mTotalText = FUtils.getTimeText(timeSec);
        mTotalTextWidth = (int) (mPaint.measureText(mTotalText));
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
        mPaint.setColor(0xFFdddddd);
        c.drawRoundRect(mLineRectF,
                mLineHalfWidth, mLineHalfWidth, mPaint);
        mPaint.setColor(0xFF666666);
        c.drawText(mProgressText, getWidth() / 2 - mProgressTextWidth, getHeight() - mPaddingWidth, mPaint);
        c.drawText(mTotalText, getWidth() - mTotalTextWidth - mLineHalfWidth * 2 - mPaddingWidth, getHeight() - mPaddingWidth, mPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        mVideoFrameManager.release();
        super.onDetachedFromWindow();
    }
}
