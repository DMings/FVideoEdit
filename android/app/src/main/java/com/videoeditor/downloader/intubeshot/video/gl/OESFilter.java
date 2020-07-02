package com.videoeditor.downloader.intubeshot.video.gl;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.utils.FLog;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class OESFilter {

    protected ShortBuffer mIndexSB;
    protected FloatBuffer mTexFB;
    protected FloatBuffer mPosFB;
    protected static final short[] VERTEX_INDEX = {
            0, 1, 3,
            2, 3, 1
    };
    protected static final float[] VERTEX_POS = {
            -1, 1.0f, 0f,
            -1, -1.0f, 0f,
            1, -1.0f, 0f,
            1, 1.0f, 0f,
    };
    private float[] mTexCoordinate = new float[8];

    protected int mProgram;
    protected int mPosition;
    protected int mTextureCoordinate;
    protected int mImageOESTexture;
    protected int uMvpMatrix;
    protected float[] mTexMatrix = new float[16];
    protected Context mContext;

    public OESFilter(Context context) {
        this.mContext = context;
        mIndexSB = ShaderHelper.arrayToShortBuffer(VERTEX_INDEX);
        mPosFB = ShaderHelper.arrayToFloatBuffer(VERTEX_POS);
        mProgram = ShaderHelper.loadProgram(context, R.raw.process_ver, R.raw.process_frg);
        mPosition = GLES20.glGetAttribLocation(mProgram, "inputPosition");
        mTextureCoordinate = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        mImageOESTexture = GLES20.glGetUniformLocation(mProgram, "inputImageOESTexture");
        uMvpMatrix = GLES20.glGetUniformLocation(mProgram, "inputMatrix");
        Matrix.setIdentityM(mTexMatrix, 0);
        mTexCoordinate = FGLUtils.resolveRotate(0);
        mTexFB = ShaderHelper.arrayToFloatBuffer(mTexCoordinate);
    }

    public void onDraw(int textureId, float[] verMatrix, float[] texCoordinate, int x, int y, int width, int height) {
        if (FGLUtils.isNotSame(mTexCoordinate, texCoordinate)) {
            System.arraycopy(texCoordinate, 0, mTexCoordinate, 0, mTexCoordinate.length);
            mTexFB = ShaderHelper.arrayToFloatBuffer(mTexCoordinate);
//            for (int i = 0; i < mTexCoordinate.length; i += 2) {
//                FLog.i("mTexCoordinate: " + mTexCoordinate[i] + "  " + mTexCoordinate[i + 1]);
//            }
        }
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glVertexAttribPointer(mPosition, 3,
                GLES20.GL_FLOAT, false, 0, mPosFB);
        GLES20.glEnableVertexAttribArray(mTextureCoordinate);
        GLES20.glVertexAttribPointer(mTextureCoordinate, 2,
                GLES20.GL_FLOAT, false, 0, mTexFB);
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, verMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(mImageOESTexture, 0);
        GLES20.glViewport(x, y, width, height);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                GLES20.GL_UNSIGNED_SHORT, mIndexSB);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mTextureCoordinate);
        GLES20.glUseProgram(0);
    }

    public void onDestroy() {
        GLES20.glDeleteProgram(mProgram);
    }

}
