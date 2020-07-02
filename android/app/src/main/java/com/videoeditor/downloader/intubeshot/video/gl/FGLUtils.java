package com.videoeditor.downloader.intubeshot.video.gl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.videoeditor.downloader.intubeshot.utils.FLog;

import javax.microedition.khronos.opengles.GL10;

public class FGLUtils {

    public static int[] createFBO(int width, int height) {
        int[] mFrameBuffer = new int[1];
        int[] mFrameBufferTexture = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
        GLES20.glGenTextures(1, mFrameBufferTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTexture[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0);
            GLES20.glDeleteTextures(1, mFrameBufferTexture, 0);
            FLog.e("create framebuffer failed");
            return null;
        }
        FLog.i("Java create framebuffer success: (" +
                width + ", " + height + "), FB: " + mFrameBuffer[0] + " , Tex: " + mFrameBufferTexture[0]);
        return new int[]{mFrameBuffer[0], mFrameBufferTexture[0]};
    }

    public static int createOESTexture() {
        int[] tex = new int[1];
        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0);
        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        //设置纹理过滤参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        //解除纹理绑定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }

    public static int createTexture() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return tex[0];
    }

    public static void glCheckErr() {
        int err = GLES20.glGetError();
        FLog.i("checkErr: " + err);
    }

    public static void glCheckErr(String tag) {
        int err = GLES20.glGetError();
        if (err != 0) {
            FLog.i(tag + " > checkErr: " + err);
        }
    }

    public static void glCheckErr(int tag) {
        int err = GLES20.glGetError();
        FLog.i(tag + " > checkErr: " + err);
    }

    private static final float[] TEX_VERTEX_0 = {
            0f, 0f,
            0f, 1f,
            1f, 1f,
            1f, 0f,
    };
    // y,x
    private static final float[] TEX_VERTEX_90 = {
            0f, 1f,
            1f, 1f,
            1f, 0f,
            0f, 0f,
    };

    private static final float[] TEX_VERTEX_180 = {
            1f, 1f,
            1f, 0f,
            0f, 0f,
            0f, 1f,
    };

    private static final float[] TEX_VERTEX_270 = {
            1f, 0f,
            0f, 0f,
            0f, 1f,
            1f, 1f,
    };

    public static float[] resolveRotate(int rotation) {
        return resolveRotate(rotation, 0, 0);
    }

    public static float[] resolveRotate(int rotation, float rx, float ry) {
        float[] texCoordinate = new float[8];
        switch (rotation) {
            case 90:
                System.arraycopy(TEX_VERTEX_90, 0, texCoordinate, 0, texCoordinate.length);
                texCoordinate[1] = 1 - rx;
                texCoordinate[3] = 1 - rx;
                texCoordinate[5] = rx;
                texCoordinate[7] = rx;

                texCoordinate[2] = 1 - ry;
                texCoordinate[4] = 1 - ry;
                texCoordinate[0] = ry;
                texCoordinate[6] = ry;
                return texCoordinate;
            case 180:
                System.arraycopy(TEX_VERTEX_180, 0, texCoordinate, 0, texCoordinate.length);
                texCoordinate[1] = 1 - ry;
                texCoordinate[7] = 1 - ry;
                texCoordinate[3] = ry;
                texCoordinate[5] = ry;

                texCoordinate[0] = 1 - rx;
                texCoordinate[2] = 1 - rx;
                texCoordinate[4] = rx;
                texCoordinate[6] = rx;
                return texCoordinate;
            case 270:
                System.arraycopy(TEX_VERTEX_270, 0, texCoordinate, 0, texCoordinate.length);
                texCoordinate[5] = 1 - rx;
                texCoordinate[7] = 1 - rx;
                texCoordinate[1] = rx;
                texCoordinate[3] = rx;

                texCoordinate[0] = 1 - ry;
                texCoordinate[6] = 1 - ry;
                texCoordinate[2] = ry;
                texCoordinate[4] = ry;
                return texCoordinate;
            case 0:
            default:
                System.arraycopy(TEX_VERTEX_0, 0, texCoordinate, 0, texCoordinate.length);
                texCoordinate[3] = 1 - ry;
                texCoordinate[5] = 1 - ry;
                texCoordinate[1] = ry;
                texCoordinate[7] = ry;

                texCoordinate[4] = 1 - rx;
                texCoordinate[6] = 1 - rx;
                texCoordinate[0] = rx;
                texCoordinate[2] = rx;
                return texCoordinate;
        }
    }

    private static void swap(float[] textureCoords, int from, int to) {
        float t = textureCoords[from];
        textureCoords[from] = textureCoords[to];
        textureCoords[to] = t;
    }

    public static boolean isNotSame(float[] textureCoord1, float[] textureCoord2) {
        for (int i = 0; i < textureCoord1.length; i++) {
            if (textureCoord1[i] != textureCoord2[i]) {
                return true;
            }
        }
        return false;
    }

}
