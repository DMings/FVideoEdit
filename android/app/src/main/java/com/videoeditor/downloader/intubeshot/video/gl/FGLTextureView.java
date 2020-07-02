package com.videoeditor.downloader.intubeshot.video.gl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.TextureView;

import com.videoeditor.downloader.intubeshot.utils.FLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FGLTextureView extends TextureView {

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private EglHelper mEglHelper;
    private OESFilter mOESFilter;
    private BlurFilter mBlurFilter;
    private List<Runnable> mPendRunnableList = new ArrayList<>();
    private Runnable mPendDrawRunnable;
    private volatile boolean mIsThreadQuit;
    private OnSurfaceTextureListener mOnSurfaceTextureListener;
    private float[] mClearColor = new float[4]; // ARGB

    public FGLTextureView(Context context) {
        this(context, null);
    }

    public FGLTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FGLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandlerThread = new HandlerThread("GL");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mEglHelper = new EglHelper();
        initEvent();
        mClearColor[0] = 1.0f;
        mClearColor[1] = 1.0f * 0xF2 / 0xFF;
        mClearColor[2] = 1.0f * 0xF2 / 0xFF;
        mClearColor[3] = 1.0f * 0xF2 / 0xFF;
    }

    public void setGLBackgroundColor(int color) { // ARGB
        mClearColor[0] = 1.0f;
        mClearColor[1] = 1.0f * Color.red(color);
        mClearColor[2] = 1.0f * Color.green(color);
        mClearColor[3] = 1.0f * Color.blue(color);
    }

    public void setOnSurfaceTextureListener(OnSurfaceTextureListener onSurfaceTextureListener) {
        mOnSurfaceTextureListener = onSurfaceTextureListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getMode(widthMeasureSpec)));
    }

    public void initEvent() {
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
                FLog.i("onSurfaceTextureAvailable>> width: " + width + " height: " + height);
                threadPost(new Runnable() {
                    @Override
                    public void run() {
                        reBuildEGL(surface);
                        if (mOnSurfaceTextureListener != null) {
                            mOnSurfaceTextureListener.onAvailable();
                        }
                    }
                });
            }

            @Override
            public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, int width, int height) {
                FLog.i("onSurfaceTextureSizeChanged>> width: " + width + " height: " + height);
                threadPost(new Runnable() {
                    @Override
                    public void run() {
                        reBuildEGL(surface);
                    }
                });
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                threadPost(new Runnable() {
                    @Override
                    public void run() {
                        mOESFilter.onDestroy();
                        mOESFilter = null;
                        mBlurFilter.onDestroy();
                        mBlurFilter = null;
                        mEglHelper.destroyEgl();
                    }
                });
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
//        getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(final SurfaceHolder holder) {
//                FLog.i("surfaceCreated>>");
//                threadPost(new Runnable() {
//                    @Override
//                    public void run() {
//                        reBuildEGL(holder.getSurface());
//                        if (mOnSurfaceTextureListener != null) {
//                            mOnSurfaceTextureListener.onAvailable();
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
//                FLog.i("onSurfaceTextureSizeChanged>> width: " + width + " height: " + height);
//                threadPost(new Runnable() {
//                    @Override
//                    public void run() {
//                        reBuildEGL(holder.getSurface());
//                    }
//                });
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                threadPost(new Runnable() {
//                    @Override
//                    public void run() {
//                        mOESFilter.onDestroy();
//                        mOESFilter = null;
//                        mBlurFilter.onDestroy();
//                        mBlurFilter = null;
//                        mEglHelper.destroyEgl();
//                    }
//                });
//            }
//        });
    }

    public void reBuildEGL(Object surface) {
        if (mOESFilter != null) {
            mOESFilter.onDestroy();
        }
        if (mBlurFilter != null) {
            mBlurFilter.onDestroy();
        }
        mEglHelper.destroyEgl();
        mEglHelper.initEgl(null, surface);
        mEglHelper.glBindThread();
        mOESFilter = new OESFilter(getContext());
        mBlurFilter = new BlurFilter(getContext());
        GLES20.glClearColor(mClearColor[1], mClearColor[2], mClearColor[3], mClearColor[0]);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mEglHelper.swapBuffers();
        Iterator<Runnable> it = mPendRunnableList.iterator();
        while (it.hasNext()) {
            Runnable runnable = it.next();
            runnable.run();
            it.remove();
        }
        if (mPendDrawRunnable != null) {
            Runnable runnable = mPendDrawRunnable;
            mPendDrawRunnable = null;
            runnable.run();
        }
    }

    public void glDraw(final FGLSurfaceTexture glSurfaceTexture, boolean canDraw) {
        if (canDraw) {
            glInnerDraw(glSurfaceTexture, true);
        } else {
            glDrawNothing(glSurfaceTexture);
        }
    }

    public void glReDraw(FGLSurfaceTexture glSurfaceTexture, boolean canDraw) {
        if (canDraw) {
            glInnerDraw(glSurfaceTexture, false);
        }
    }

    public void glDrawNothing(final FGLSurfaceTexture glSurfaceTexture) {
        if (mEglHelper.isEglCreate()) {
            threadPost(new Runnable() {
                @Override
                public void run() {
                    glSurfaceTexture.getSurfaceTexture().updateTexImage();
                }
            });
        } else { // 未建立EGL，先挂起
            mPendRunnableList.add(new Runnable() {
                @Override
                public void run() {
                    glSurfaceTexture.getSurfaceTexture().updateTexImage();
                }
            });
        }
    }

    private void glInnerDraw(final FGLSurfaceTexture glSurfaceTexture, final boolean updateTexture) {
        if (mEglHelper.isEglCreate()) {
            threadPost(new Runnable() {
                @Override
                public void run() {
                    if (updateTexture) {
                        glSurfaceTexture.getSurfaceTexture().updateTexImage();
                    }
                    GLES20.glClearColor(mClearColor[1], mClearColor[2], mClearColor[3], mClearColor[0]);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    if (glSurfaceTexture.getVideoMode() == FGLSurfaceTexture.MODE_BLUR_SCREEN) {
                        mBlurFilter.onDraw(glSurfaceTexture.getTexture(), glSurfaceTexture.getBlurVerMatrix(),
                                glSurfaceTexture.getTexCoordinate(), 0, 0, getWidth(), getHeight());
                    }
                    mOESFilter.onDraw(glSurfaceTexture.getTexture(), glSurfaceTexture.getVerMatrix(),
                            glSurfaceTexture.getTexCoordinate(), 0, 0, getWidth(), getHeight());
                    mEglHelper.swapBuffers();
                }
            });
        } else { // 未建立EGL，先挂起
            if (updateTexture) {
                mPendRunnableList.add(new Runnable() {
                    @Override
                    public void run() {
                        glSurfaceTexture.getSurfaceTexture().updateTexImage();
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        if (glSurfaceTexture.getVideoMode() == FGLSurfaceTexture.MODE_BLUR_SCREEN) {
                            mBlurFilter.onDraw(glSurfaceTexture.getTexture(), glSurfaceTexture.getBlurVerMatrix(),
                                    glSurfaceTexture.getTexCoordinate(), 0, 0, getWidth(), getHeight());
                        }
                        mOESFilter.onDraw(glSurfaceTexture.getTexture(), glSurfaceTexture.getVerMatrix(),
                                glSurfaceTexture.getTexCoordinate(), 0, 0, getWidth(), getHeight());
                        mEglHelper.swapBuffers();
                    }
                });
            } else {
                mPendDrawRunnable = new Runnable() {
                    @Override
                    public void run() {
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        if (glSurfaceTexture.getVideoMode() == FGLSurfaceTexture.MODE_BLUR_SCREEN) {
                            mBlurFilter.onDraw(glSurfaceTexture.getTexture(), glSurfaceTexture.getBlurVerMatrix(),
                                    glSurfaceTexture.getTexCoordinate(), 0, 0, getWidth(), getHeight());
                        }
                        mOESFilter.onDraw(glSurfaceTexture.getTexture(), glSurfaceTexture.getVerMatrix(),
                                glSurfaceTexture.getTexCoordinate(), 0, 0, getWidth(), getHeight());
                        mEglHelper.swapBuffers();
                    }
                };
            }
        }
    }

//    public void glDrawClear() {
//        glPostDraw(new Runnable() {
//            @Override
//            public void run() {
//                GLES20.glClearColor(mClearColor[1], mClearColor[2], mClearColor[3], mClearColor[0]);
//                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//                mEglHelper.swapBuffers();
//            }
//        });
//    }

    public void glPost(final Runnable runnable) {
        if (mEglHelper.isEglCreate()) {
            threadPost(runnable);
        } else { // 未建立EGL，先挂起
            mPendRunnableList.add(runnable);
        }
    }

//    public void glPostDraw(Runnable runnable) {
//        if (mEglHelper.isEglCreate()) {
//            threadPost(runnable);
//        } else { // 未建立EGL，先挂起
//            mPendDrawRunnable = runnable;
//        }
//    }

    public void release() {
        mPendRunnableList.clear();
        mIsThreadQuit = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mHandlerThread.quit();
                mPendRunnableList.clear();
            }
        });
    }

    private void threadPost(Runnable runnable) {
        if (mIsThreadQuit) {
            return;
        }
        mHandler.post(runnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    public interface OnSurfaceTextureListener {
        void onAvailable();
    }
}
