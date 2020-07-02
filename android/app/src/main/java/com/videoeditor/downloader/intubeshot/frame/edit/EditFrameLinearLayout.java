package com.videoeditor.downloader.intubeshot.frame.edit;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.videoeditor.downloader.intubeshot.frame.OnSeekListener;
import com.videoeditor.downloader.intubeshot.loader.FrameLoader;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.utils.FUtils;
import com.videoeditor.downloader.intubeshot.video.control.VideoControlManager;

import java.util.ArrayList;
import java.util.List;

public class EditFrameLinearLayout extends FrameLayout {

    private List<ImageView> mImageViewList = new ArrayList<>();
    private OnSeekListener mOnSeekListener;
    //
    private int mViewHeight;
    private int mViewWidth;
    private int mViewPadding;
    private RectF mViewLeftRect = new RectF();
    private RectF mViewRightRect = new RectF();
    private RectF mViewCursorRect = new RectF();
    private final int LEFT_VIEW = 0;
    private final int RIGHT_VIEW = 1;
    private final int CURSOR_VIEW = 2;
    private Paint mPaint;
    private int mLineWidth;
    private RectF mCurRectView;
    private int mCurIndexView;
    public static final int MODE_CUT_CENTER = 0;
    public static final int MODE_CUT_LR = 1;
    public static final int MODE_CUT_TWO_SIDE = 2;
    private int mCutMode = MODE_CUT_CENTER;
    private final int CURSOR_LEFT_VIEW = 0;
    private final int CURSOR_RIGHT_VIEW = 1;
    private final int CURSOR_NONE_VIEW = -1;
    private int mCurCursorIndex;
    private int mLastCursorIndex;
    private long mSeekTime = -1;
    private RectF mSeekRectF = new RectF();
    private boolean mIsJumpRight;
    private ValueAnimator mCursorAnimator;
    //
    private Paint mTextPaint;
    private int mProgressTextWidth;
    private int mTotalTextWidth;
    private String mProgressText;
    private String mTotalText;
    //
    private VideoFrame mVideoFrame;
    private int mPaddingTop;
    //
    private Handler mHandler = new Handler();

    public EditFrameLinearLayout(Context context) {
        this(context, null);
    }

    public EditFrameLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditFrameLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mTextPaint = new Paint();
        float textHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8,
                this.getResources().getDisplayMetrics());
        mTextPaint.setColor(0xFF333333);
        mTextPaint.setTextSize(textHeight);
        mProgressText = FUtils.getTimeText(0.0f);
        mTotalText = FUtils.getTimeText(0.0f);
        mProgressTextWidth = (int) (mTextPaint.measureText(mProgressText) / 2);
        mTotalTextWidth = (int) (mTextPaint.measureText(mProgressText));
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setAntiAlias(true);
        //
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(outMetrics);
        } else {
            outMetrics.widthPixels = 720;
        }
        int itemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 58,
                getContext().getResources().getDisplayMetrics());
        int itemHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50,
                getContext().getResources().getDisplayMetrics());
        mViewPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                getContext().getResources().getDisplayMetrics());

        mViewWidth = outMetrics.widthPixels - mViewPadding * 2;

        LinearLayout linearLayout = new LinearLayout(getContext());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mViewWidth, itemHeight);
        layoutParams.setMargins(mViewPadding, 0, mViewPadding, 0);
        linearLayout.setLayoutParams(layoutParams);
        addView(linearLayout);

        mViewHeight = itemHeight;
        int num = mViewWidth / itemWidth;
        int decItemSize = mViewWidth % itemWidth;
        for (int i = 0; i < num; i++) {
            FrameLayout frameLayout = new FrameLayout(getContext());
            frameLayout.setLayoutParams(new LayoutParams(itemWidth, itemHeight));
            ImageView imageView = new ImageView(getContext());
            frameLayout.setLayoutParams(new LayoutParams(itemWidth, itemHeight));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            frameLayout.addView(imageView);
            linearLayout.addView(frameLayout);
            mImageViewList.add(imageView);
        }
        if (decItemSize > 0) {
            FrameLayout frameLayout = new FrameLayout(getContext());
            frameLayout.setLayoutParams(new LayoutParams(itemWidth, itemHeight));
            ImageView imageView = new ImageView(getContext());
            frameLayout.setLayoutParams(new LayoutParams(decItemSize, itemHeight));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            frameLayout.addView(imageView);
            linearLayout.addView(frameLayout);
            mImageViewList.add(imageView);
        }
        initRectView();
    }

    public boolean changeVideoFrameList() {
        long lt = (long) (1.0f * mViewLeftRect.left / mViewWidth * mVideoFrame.getOffsetDurationMs());
        long rt = (long) (1.0f * mViewRightRect.left / mViewWidth * mVideoFrame.getOffsetDurationMs());

        if (lt < 500 || rt < 500) {
            return false;
        }

        List<VideoFrame> videoFrameList = VideoFrameManager.getInstance().getVideoFrameList();
        if (mCutMode == MODE_CUT_CENTER) {
            mVideoFrame.setVideoTime(mVideoFrame.getStartTime() + lt, mVideoFrame.getStartTime() + rt);
        } else if (mCutMode == MODE_CUT_TWO_SIDE || mCutMode == MODE_CUT_LR) {
            for (int i = 0; i < videoFrameList.size(); i++) {
                if (videoFrameList.get(i).getUUID() != null && videoFrameList.get(i).getUUID().equals(mVideoFrame.getUUID())) { // 找到当前位置

                    long endTime = mVideoFrame.getEndTime();
                    mVideoFrame.setVideoTime(mVideoFrame.getStartTime(), lt);
                    mVideoFrame.handleVideoFrame(mVideoFrame.getUUID());
                    videoFrameList.set(i, mVideoFrame);

                    VideoFrame vf = mVideoFrame.getNewVideoFrame(mVideoFrame.getVideoTag() + (i + 1),
                            mCutMode == MODE_CUT_LR ? rt : lt, endTime);
                    videoFrameList.add(i + 1, vf);

                    if (mViewCursorRect.left >= mViewRightRect.left) {
                        mVideoFrame.setIsCheck(false);
                        vf.setIsCheck(true);
                    } else {
                        mVideoFrame.setIsCheck(true);
                        vf.setIsCheck(false);
                    }
                    break;
                }
            }
        }
        VideoFrameManager.getInstance().updateVideoFrameListTime();
        return true;
    }

    public void setProgressText(long timeSec) {
        mProgressText = FUtils.getTimeText(timeSec);
        mProgressTextWidth = (int) (mPaint.measureText(mProgressText) / 2);
    }

    public void setTotalText(long timeSec) {
        mTotalText = FUtils.getTimeText(timeSec);
        mTotalTextWidth = (int) (mPaint.measureText(mTotalText));
    }

    public void setOnSeekListener(OnSeekListener onSeekListener) {
        mOnSeekListener = onSeekListener;
    }

    public void setFControl(final VideoControlManager control) {
        control.setOnProgressListener(new VideoControlManager.OnProgressListener() {
            @Override
            public void onProgress(VideoFrame videoFrame, long offsetTime) {
                int pos = (int) (1.0f * offsetTime / mVideoFrame.getOffsetDurationMs() * mViewWidth);
//                FLog.i("mCurTime: " + offsetTime + " pos: " + pos);
                limitToMoveCursor(pos);
            }
        });
        // 在 onProgress 之后运行
        control.setOnPlayEndListener(new VideoControlManager.OnPlayEndListener() {
            @Override
            public long onStartTime(VideoFrame videoFrame) { // 偏移时间

                mCurCursorIndex = CURSOR_LEFT_VIEW;
                invalidate();

                float percent = 0;
                if (mCutMode == MODE_CUT_CENTER) {
                    percent = 1.0f * mViewLeftRect.left / mViewWidth;
                    mViewCursorRect.set(mViewLeftRect);
                } else {
                    mViewCursorRect.offsetTo(0, mPaddingTop);
                }
//                FLog.i("(long) (videoFrame.getOffsetDurationMs() * percent): " + (long) (videoFrame.getOffsetDurationMs() * percent));
                return (long) (videoFrame.getOffsetDurationMs() * percent);
            }

            @Override
            public VideoControlManager.PlayNext onEndNext(VideoFrame videoFrame, long realTime) {
                long endTime = videoFrame.getEndTime();
                if (mCutMode == MODE_CUT_CENTER) {
                    endTime = videoFrame.getStartTime() + (long) (1.0f * mViewRightRect.left / mViewWidth * videoFrame.getOffsetDurationMs());
                }
//                FLog.i("realTime: " + realTime + " endTime: " + endTime);
                if (realTime >= endTime) {
                    mIsJumpRight = false;
//                    if (nextVideoFrame == null) { // 没有下一个了，视频结束了
                    if (control.isPlaying()) {
                        progressPostEnd();
                    }
//                    }
                    if (mCutMode == MODE_CUT_LR || mCutMode == MODE_CUT_TWO_SIDE) {
                        return new VideoControlManager.PlayNext(null, 0);
                    } else {
                        long t = (long) (1.0f * mViewLeftRect.left / mViewWidth * videoFrame.getOffsetDurationMs());
                        return new VideoControlManager.PlayNext(null, t);
                    }
                } else {
                    if (mCutMode == MODE_CUT_LR && mCurCursorIndex == CURSOR_NONE_VIEW) {
                        int pos = (int) (1.0f * (realTime - videoFrame.getStartTime()) / videoFrame.getOffsetDurationMs() * mViewWidth);
                        if (pos > mViewLeftRect.left && !mIsJumpRight && mViewLeftRect.left != mViewRightRect.left) {
                            mIsJumpRight = true;
                            stopCursorAnimator();
                            long t = (long) (1.0f * mViewRightRect.left / mViewWidth * videoFrame.getOffsetDurationMs());
                            return new VideoControlManager.PlayNext(videoFrame, t);
                        }
                    }
                }
                return null; // 正常情况，不是结束状态
            }
        });
    }

    public void progressPostEnd() {
        if (mCutMode == MODE_CUT_CENTER) {
            postCursorAnimator(mViewCursorRect.left, mViewRightRect.left);
        } else {
            postCursorAnimator(mViewCursorRect.left, mViewWidth);
        }
    }

    public synchronized void postCursorAnimator(float lastX, float newX) {
        if (mCursorAnimator == null) {
            mCursorAnimator = new ValueAnimator();
            mCursorAnimator.setDuration(95);
            mCursorAnimator.setInterpolator(new LinearInterpolator());
            mCursorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mViewCursorRect.offsetTo((Float) animation.getAnimatedValue(), mPaddingTop);
                    float percent = 1.0f * mViewCursorRect.left / mViewWidth;
                    long time = (long) (mVideoFrame.getOffsetDurationMs() * percent);
                    setProgressText(time);
                    invalidate();
                }
            });
        } else {
            mCursorAnimator.cancel();
        }
        mCursorAnimator.setFloatValues(lastX, newX);
        mCursorAnimator.start();
    }

    public synchronized void stopCursorAnimator() {
        if (mCursorAnimator != null) {
            mCursorAnimator.cancel();
        }
        mCurCursorIndex = CURSOR_RIGHT_VIEW;
        mViewCursorRect.set(mViewRightRect);
    }

    public void reset() {
        stopCursorAnimator();
        mCutMode = MODE_CUT_CENTER;
        mCurCursorIndex = CURSOR_LEFT_VIEW;
        mLastCursorIndex = CURSOR_LEFT_VIEW;
        mViewLeftRect.offsetTo(0, mPaddingTop);
        mViewRightRect.offsetTo(mViewWidth, mPaddingTop);
        mViewCursorRect.offsetTo(0, mPaddingTop);
        setProgressText(0);
        invalidate();
    }

    public void open(VideoFrame videoFrame) {
        mVideoFrame = videoFrame;
        reset();
        //
        setTotalText(videoFrame.getOffsetDurationMs());
        invalidate();
        FrameLoader.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap;
                int itemCount = mImageViewList.size();
                if (itemCount == 1) {
                    bitmap = mVideoFrame.getKeyFrameAtTimeMsFromCache(mVideoFrame.getStartTime());
                    postImageBitmap(0, bitmap);
                } else if (itemCount == 2) {
                    bitmap = mVideoFrame.getKeyFrameAtTimeMsFromCache(mVideoFrame.getStartTime());
                    postImageBitmap(0, bitmap);
                    bitmap = mVideoFrame.getKeyFrameAtTimeMsFromCache(mVideoFrame.getEndTime());
                    postImageBitmap(1, bitmap);
                } else if (itemCount > 2) {
                    int c = itemCount - 1;
                    long durationMs = mVideoFrame.getOffsetDurationMs();
                    int timeSpan = (int) (durationMs / c);
                    for (int i = 0; i < c; i++) {
                        bitmap = mVideoFrame.getKeyFrameAtTimeMsFromCache(mVideoFrame.getStartTime() + timeSpan * i);
                        postImageBitmap(i, bitmap);
                    }
                    bitmap = mVideoFrame.getKeyFrameAtTimeMsFromCache(mVideoFrame.getEndTime());
                    postImageBitmap(c, bitmap);
                }
            }
        });
    }

    private void postImageBitmap(final int index, final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (index < mImageViewList.size()) {
                    mImageViewList.get(index).setImageBitmap(bitmap);
                }
            }
        });
    }

//    public void open(VideoFrame videoFrame) {
//        mVideoFrame = videoFrame;
//        reset();
//        //
//        setTotalText(videoFrame.getOffsetDurationMs());
//        invalidate();
//        int itemCount = mImageViewList.size();
//        if (itemCount == 1) {
//            setFrameView(0, videoFrame.getStartTime());
//        } else if (itemCount == 2) {
//            setFrameView(0, videoFrame.getStartTime());
//            setFrameView(1, videoFrame.getEndTime());
//        } else if (itemCount > 2) {
//            int c = itemCount - 1;
//            long durationMs = videoFrame.getOffsetDurationMs();
//            int timeSpan = (int) (durationMs / c);
//            for (int i = 0; i < c; i++) {
//                setFrameView(i, videoFrame.getStartTime() + timeSpan * i);
//            }
//            setFrameView(c, videoFrame.getEndTime());
//        }
//    }
//
//    private void setFrameView(int viewIndex, long time) {
//        if (viewIndex < mImageViewList.size()) {
//            Glide.with(getContext())
//                    .load(new VFrame(mVideoFrame.getFile().getPath(), time))
//                    .placeholder(R.drawable.bg_color_white)
//                    .centerCrop()
//                    .into(mImageViewList.get(viewIndex));
//        }
//    }

    private void initRectView() {
        mPaddingTop = getPaddingTop();
        int vWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30,
                getContext().getResources().getDisplayMetrics());
        mLineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getContext().getResources().getDisplayMetrics());
        mViewLeftRect.set(0, mPaddingTop, vWidth, mViewHeight + mPaddingTop);
        mViewRightRect.set(mViewPadding + mViewWidth - vWidth / 2, mPaddingTop, mViewPadding + mViewWidth + (vWidth >> 1), mViewHeight + mPaddingTop);
        mViewCursorRect.set(0, mPaddingTop, vWidth, mViewHeight + mPaddingTop);
        mSeekRectF.set(mViewCursorRect);
        //
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mLineWidth);
        setWillNotDraw(false);
    }

    public void changeCutMode(int cutMode) {
        if (cutMode == MODE_CUT_LR) {
            mCutMode = MODE_CUT_LR;
            //
            mCurCursorIndex = CURSOR_LEFT_VIEW;
            mLastCursorIndex = CURSOR_LEFT_VIEW;
            mViewLeftRect.offsetTo(mViewWidth / 3, mPaddingTop);
            mViewRightRect.offsetTo(mViewWidth * 2 / 3, mPaddingTop);
            mViewCursorRect.offsetTo(0, mPaddingTop);
        } else if (cutMode == MODE_CUT_TWO_SIDE) {
            mCutMode = MODE_CUT_TWO_SIDE;
            //
            mCurCursorIndex = CURSOR_LEFT_VIEW;
            mLastCursorIndex = CURSOR_LEFT_VIEW;
            mViewLeftRect.offsetTo(mViewWidth / 2, mPaddingTop);
            mViewRightRect.offsetTo(mViewWidth, mPaddingTop);
            mViewCursorRect.offsetTo(mViewWidth / 2, mPaddingTop);
        } else {
            mCutMode = MODE_CUT_CENTER;
            //
            mCurCursorIndex = CURSOR_LEFT_VIEW;
            mLastCursorIndex = CURSOR_LEFT_VIEW;
            mViewLeftRect.offsetTo(0, mPaddingTop);
            mViewRightRect.offsetTo(mViewWidth, mPaddingTop);
            mViewCursorRect.offsetTo(0, mPaddingTop);
        }

        mOnSeekListener.start();
        mIsJumpRight = false;
        float percent = 1.0f * mViewCursorRect.left / mViewWidth;
        long time = (long) (mVideoFrame.getOffsetDurationMs() * percent);
        FLog.i("changeCutMode end time: " + time);
        mSeekTime = time;
        mOnSeekListener.end(mVideoFrame, time);
        invalidate();
    }

    private int findTouchView(float x) {
        if (mViewRightRect.left <= x && x <= mViewRightRect.right) {
            return RIGHT_VIEW;
        } else if (mViewLeftRect.left <= x && x <= mViewLeftRect.right) {
            return LEFT_VIEW;
        } else {
            return CURSOR_VIEW;
        }
    }

    private RectF getTouchView(int index) {
        if (index == RIGHT_VIEW) {
            return mViewRightRect;
        } else if (index == LEFT_VIEW) {
            return mViewLeftRect;
        } else {
            return mViewCursorRect;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionIndex() > 1) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                FLog.i("MotionEvent.ACTION_DOWN");
                mCurIndexView = findTouchView(event.getX());
                mCurRectView = getTouchView(mCurIndexView);
                limitToMove(event);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                limitToMove(event);
                invalidate();
                break;
        }
        return true;
    }

    private void limitToMoveCursor(float x) {
        float lastCursorX = mViewCursorRect.left;
        mSeekRectF.offsetTo(x, mPaddingTop);
        if (mSeekRectF.left < 0) {
            mSeekRectF.offsetTo(0, mPaddingTop);
        } else if (mSeekRectF.left > mViewWidth) {
            mSeekRectF.offsetTo(mViewWidth, mPaddingTop);
        }
        if (mCutMode == MODE_CUT_CENTER) {
            if (mSeekRectF.left < mViewLeftRect.left) {
                mSeekRectF.offsetTo(mViewLeftRect.left, mPaddingTop);
            } else if (mSeekRectF.left > mViewRightRect.left) {
                mSeekRectF.offsetTo(mViewRightRect.left, mPaddingTop);
            }
        } else if (mCutMode == MODE_CUT_LR) {
            int lastCursorIndex = mCurCursorIndex;
            if (mSeekRectF.left <= mViewLeftRect.left) {
                mCurCursorIndex = CURSOR_LEFT_VIEW;
            } else if (mSeekRectF.left >= mViewRightRect.left) {
                mCurCursorIndex = CURSOR_RIGHT_VIEW;
            } else {
                mCurCursorIndex = CURSOR_NONE_VIEW;
            }
            if (lastCursorIndex != mCurCursorIndex) {
                mLastCursorIndex = lastCursorIndex;
            }
//            FLog.i("mCurCursorIndex>>>" + mCurCursorIndex + " mLastCursorIndex->>>" + mLastCursorIndex);
            if (mLastCursorIndex == CURSOR_LEFT_VIEW && mCurCursorIndex == CURSOR_NONE_VIEW) {
                if (mSeekRectF.left > mViewLeftRect.left) {
                    mSeekRectF.offsetTo(mViewLeftRect.left, mPaddingTop);
                }
            } else if (mLastCursorIndex == CURSOR_RIGHT_VIEW && mCurCursorIndex == CURSOR_NONE_VIEW) {
                if (mSeekRectF.left < mViewRightRect.left) {
                    mSeekRectF.offsetTo(mViewRightRect.left, mPaddingTop);
                }
            }
        }
//        FLog.i("limitToMoveCursor lastCursorX: " + lastCursorX + " mSeekRectF: " + mSeekRectF.left + " mViewCursorRect: " + mViewCursorRect.left);
        postCursorAnimator(lastCursorX, mSeekRectF.left);
    }

    private void limitToMove(MotionEvent event) {
        mCurRectView.offsetTo(event.getX() - mViewPadding, mPaddingTop);
        if (mCurRectView.left < 0) {
            mCurRectView.offsetTo(0, mPaddingTop);
        } else if (mCurRectView.left > mViewWidth) {
            mCurRectView.offsetTo(mViewWidth, mPaddingTop);
        }
        if (mCutMode == MODE_CUT_CENTER || mCutMode == MODE_CUT_LR) {
            if (mCurIndexView == LEFT_VIEW) {
                if (mViewLeftRect.centerX() > mViewRightRect.centerX()) {
                    mCurRectView.offsetTo(mViewRightRect.left, mPaddingTop);
                }
            } else if (mCurIndexView == RIGHT_VIEW) {
                if (mViewRightRect.centerX() < mViewLeftRect.centerX()) {
                    mCurRectView.offsetTo(mViewLeftRect.left, mPaddingTop);
                }
            }
        }
        if (mCurIndexView != CURSOR_VIEW) {
            mViewCursorRect.set(mCurRectView);
        }
        if (mCutMode == MODE_CUT_CENTER) {
            if (mViewCursorRect.left < mViewLeftRect.left) {
                mViewCursorRect.offsetTo(mViewLeftRect.left, mPaddingTop);
            } else if (mViewCursorRect.left > mViewRightRect.left) {
                mViewCursorRect.offsetTo(mViewRightRect.left, mPaddingTop);
            }
        } else if (mCutMode == MODE_CUT_LR) {
            int lastCursorIndex = mCurCursorIndex;
            if (mViewCursorRect.left <= mViewLeftRect.left) {
                mCurCursorIndex = CURSOR_LEFT_VIEW;
            } else if (mViewCursorRect.left >= mViewRightRect.left) {
                mCurCursorIndex = CURSOR_RIGHT_VIEW;
            } else {
                mCurCursorIndex = CURSOR_NONE_VIEW;
            }
            if (lastCursorIndex != mCurCursorIndex) {
                mLastCursorIndex = lastCursorIndex;
            }
//            FLog.i("mCurCursorIndex>>>" + mCurCursorIndex + " mLastCursorIndex->>>" + mLastCursorIndex);
            if (mLastCursorIndex == CURSOR_LEFT_VIEW && mCurCursorIndex == CURSOR_NONE_VIEW) {
                if (mViewCursorRect.left > mViewLeftRect.left) {
                    mViewCursorRect.offsetTo(mViewLeftRect.left, mPaddingTop);
                }
            } else if (mLastCursorIndex == CURSOR_RIGHT_VIEW && mCurCursorIndex == CURSOR_NONE_VIEW) {
                if (mViewCursorRect.left < mViewRightRect.left) {
                    mViewCursorRect.offsetTo(mViewRightRect.left, mPaddingTop);
                }
            }
        }
        if (mOnSeekListener != null) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mOnSeekListener.start();
                float percent = 1.0f * mCurRectView.left / mViewWidth;
                long time = (long) (mVideoFrame.getOffsetDurationMs() * percent);
                if (mSeekTime != time) {
                    mSeekTime = time;
                    mOnSeekListener.seeking(mVideoFrame, time);
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float percent = 1.0f * mCurRectView.left / mViewWidth;
                long time = (long) (mVideoFrame.getOffsetDurationMs() * percent);
                if (mSeekTime != time) {
                    mSeekTime = time;
                    mOnSeekListener.seeking(mVideoFrame, time);
                }
            } else {
                mIsJumpRight = false;
                float percent = 1.0f * mCurRectView.left / mViewWidth;
                long time = (long) (mVideoFrame.getOffsetDurationMs() * percent);
                if (mSeekTime != time) {
                    mSeekTime = time;
                    mOnSeekListener.end(mVideoFrame, time);
                }
            }
        }
        float percent = 1.0f * mViewCursorRect.left / mViewWidth;
        long time = (long) (mVideoFrame.getOffsetDurationMs() * percent);
        setProgressText(time);
    }

    private void drawLRView(Canvas canvas, RectF rectF) {
        canvas.drawRect(rectF.centerX() - (mLineWidth >> 1),
                rectF.top,
                rectF.centerX() + (mLineWidth >> 1),
                rectF.bottom, mPaint); // top
        canvas.drawCircle(rectF.centerX(), rectF.centerY(), mLineWidth * 1.5f, mPaint);
    }

    private void drawCursorView(Canvas canvas, RectF rectF) {
        canvas.drawRect(rectF.centerX() - (mLineWidth >> 1),
                rectF.top,
                rectF.centerX() + (mLineWidth >> 1),
                rectF.bottom, mPaint);
    }

    private void drawCutCenter(Canvas canvas) {
        canvas.drawRect(mViewLeftRect.centerX(),
                mViewLeftRect.top,
                mViewRightRect.centerX(),
                mLineWidth + mViewLeftRect.top, mPaint);
        canvas.drawRect(mViewLeftRect.centerX(),
                mViewLeftRect.bottom - mLineWidth,
                mViewRightRect.centerX(),
                mViewLeftRect.bottom, mPaint);

    }

    private void drawCutCenterBg(Canvas canvas) {
        canvas.drawRect(mViewPadding,
                mViewLeftRect.top,
                mViewLeftRect.centerX(),
                mViewLeftRect.bottom, mPaint);
        canvas.drawRect(mViewRightRect.centerX() + (mLineWidth >> 1),
                mViewRightRect.top,
                mViewPadding + mViewWidth,
                mViewRightRect.bottom, mPaint);
    }

    private void drawCutLR(Canvas canvas) {
        canvas.drawRect(mViewPadding, // l top
                mViewLeftRect.top,
                mViewLeftRect.centerX(),
                mLineWidth + mViewLeftRect.top, mPaint);
        canvas.drawRect(mViewPadding, // l bottom
                mViewLeftRect.bottom - mLineWidth,
                mViewLeftRect.centerX(),
                mViewLeftRect.bottom, mPaint);
        canvas.drawRect(mViewPadding - mLineWidth / 2, // l left
                mViewLeftRect.top,
                mViewPadding + mLineWidth / 2,
                mViewLeftRect.bottom, mPaint);
        //
        canvas.drawRect(mViewRightRect.centerX(), // r top
                mViewRightRect.top,
                mViewPadding + mViewWidth,
                mLineWidth + mViewRightRect.top, mPaint);
        canvas.drawRect(mViewRightRect.centerX(), // r bottom
                mViewRightRect.bottom - mLineWidth,
                mViewPadding + mViewWidth,
                mViewRightRect.bottom, mPaint);
        canvas.drawRect(mViewPadding + mViewWidth - mLineWidth / 2, // r right
                mViewLeftRect.top,
                mViewPadding + mViewWidth + (mLineWidth >> 1),
                mViewLeftRect.bottom, mPaint);
    }

    private void drawCutLRBg(Canvas canvas) {
        canvas.drawRect(mViewLeftRect.centerX(),
                mViewLeftRect.top,
                mViewRightRect.centerX(),
                mViewLeftRect.bottom, mPaint);
        //
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mPaint.setColor(0xFFdddddd);
        drawCursorView(canvas, mViewCursorRect);
        if (mCutMode == MODE_CUT_CENTER) {
            mPaint.setColor(0x77000000);
            drawCutCenterBg(canvas);
            mPaint.setColor(0xFF32B378);
            drawCutCenter(canvas);
            drawLRView(canvas, mViewRightRect);
        } else if (mCutMode == MODE_CUT_LR) {
            mPaint.setColor(0x66000000);
            drawCutLRBg(canvas);
            mPaint.setColor(0xFF32B378);
            drawLRView(canvas, mViewRightRect);
            drawCutLR(canvas);
        } else {
            mPaint.setColor(0xFF32B378);
        }
        drawLRView(canvas, mViewLeftRect);

        canvas.drawText(mProgressText, (getWidth() >> 1) - mProgressTextWidth * 1.2f, mPaddingTop * 0.8f, mTextPaint);
        canvas.drawText(mTotalText, getWidth() - mTotalTextWidth - mViewPadding - 3, mPaddingTop * 0.8f, mTextPaint);
    }

}
