package com.videoeditor.downloader.intubeshot;

import android.graphics.Rect;

import androidx.test.runner.AndroidJUnit4;

import com.videoeditor.downloader.intubeshot.create.CreateVideoUtils;
import com.videoeditor.downloader.intubeshot.utils.FLog;
import com.videoeditor.downloader.intubeshot.utils.FUtils;
import com.videoeditor.downloader.intubeshot.video.gl.FGLUtils;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
//        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        assertEquals("com.videoeditor.downloader.intubeshot", appContext.getPackageName());
//        Rect rect = CreateVideoUtils.getVideoRect(720, 720, 640, 320);
//        Rect rect = CreateVideoUtils.getVideoRect(640, 320, 720, 720);
//        Rect rect = CreateVideoUtils.getVideoRect(800, 320, 640, 320);
//        Rect rect = CreateVideoUtils.getVideoRect(640, 320,800, 320);
//        Rect rect = CreateVideoUtils.getVideoRect(320, 640,320, 800);
//        Rect rect = CreateVideoUtils.getVideoRect(320, 800,320, 640);
//        FLog.i("rect: " + rect.toString());


        String string = FUtils.getFileSize(1424164814);
        FLog.i("getFileSize string: " + string);
    }


    @Test
    public void testVertex() {
//        float[] TEX_VERTEX = FGLUtils.resolveRotate(90);
//        for (int i = 0; i < TEX_VERTEX.length; i += 2) {
//            FLog.i("TEX_VERTEX: " + TEX_VERTEX[i] + "  " + TEX_VERTEX[i + 1]);
//        }
//        String id = "12010119900307951x";
//        boolean bo = isIdCardNo(id);
//        FLog.i(id+ " isIdCardNo: " + bo);
    }
}
