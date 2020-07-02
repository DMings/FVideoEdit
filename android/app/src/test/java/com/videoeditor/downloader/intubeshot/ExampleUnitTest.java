package com.videoeditor.downloader.intubeshot;

import com.videoeditor.downloader.intubeshot.loader.FrameLoader;
import com.videoeditor.downloader.intubeshot.utils.FUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
//        assertEquals(4, 2 + 2);
        ExecutorService executorService = FrameLoader.getInstance().getExecutorService();
        System.out.println("--------start--------");
        Future future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("--------s--------");
            }
        });
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        future.cancel(false);
        future.cancel(false);
        future.cancel(false);
        future.cancel(false);
        future.cancel(false);
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--------finish--------");
    }

    @Test
    public void testLru() {
        LinkedHashMap<String, String> mCacheMap = new LinkedHashMap<>(0, 0.75f, true);
        mCacheMap.put("1", "111");
        mCacheMap.put("2", "222");
        mCacheMap.put("3", "333");
        mCacheMap.put("4", "444");
        mCacheMap.put("5", "555");

        mCacheMap.get("3");

        Iterator<Map.Entry<String, String>> iterator = mCacheMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> toEvict = iterator.next();
            System.out.println("toEvict: " + toEvict.getKey() + "  getValue " + toEvict.getValue());
        }

    }

    @Test
    public void testTime() {
        long seekTime = (long) (FUtils.getSecDecimal(14164) * 1000);
        String str = FUtils.getTimeText(seekTime);
        System.out.println("time: " + seekTime);
        System.out.println("str: " + str);
    }

    @Test
    public void testList() {
        List<String> stringList = new ArrayList<>();
        stringList.add("111");
        stringList.add("222");
        stringList.add("333");
        stringList.add("444");
        stringList.add("555");

        for (String str : stringList) {
            System.out.println("str: " + str);
        }

        stringList.set(2, "6666");
        System.out.println("str: ------------");
        for (String str : stringList) {
            System.out.println("str: " + str);
        }
    }

    public static class VideoSize {
        public int x;
        public int y;

        public VideoSize(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @Test
    public void testVideoSize() {
        List<VideoSize> pointList = new ArrayList<>();
        pointList.add(new VideoSize(720, 1280));
//        pointList.add(new VideoSize(720, 1280));

        int targetWidth = 0;
        int targetHeight = 0;
        for (int i = 0; i < pointList.size(); i++) {
            VideoSize videoFrame = pointList.get(i);
            int t;
            int l;
            int w;
            int h;
            int curWidth = videoFrame.x;
            int curHeight = videoFrame.y;
            System.out.println("videoFrame.getWidth: " + curWidth +
                    " videoFrame.getHeight: " + curHeight);
            if (i == 0) {
                if (curWidth > curHeight) {
                    targetWidth = 1280;
                    targetHeight = 720;
                } else {
                    targetWidth = 720;
                    targetHeight = 1280;
                }
            }
//                    if (i == 0) {
//                        targetWidth = curWidth;
//                        targetHeight = curHeight;
//                        w = curWidth;
//                        h = curHeight;
//                    } else {
            if (targetHeight > targetWidth && curWidth > curHeight) {
                h = curHeight;
                float r = 1.0f * targetWidth / targetHeight;
                w = (int) (h * r);
                t = 0;
                l = (curWidth - w) / 2;
            } else if (targetWidth > targetHeight && curHeight > curWidth) {
                w = curWidth;
                float r = 1.0f * targetHeight / targetWidth;
                h = (int) (r * w);
                t = (curHeight - h) / 2;
                l = 0;
            } else if (targetWidth > targetHeight && curWidth > curHeight) {
                float tr = 1.0f * targetWidth / targetHeight;
                float cr = 1.0f * curWidth / curHeight;
                if (cr <= tr) {
                    w = curWidth;
                    h = (int) (w / tr);
                    t = (curHeight - h) / 2;
                    l = 0;
                } else {
                    h = curHeight;
                    w = (int) (h / tr);
                    t = 0;
                    l = (curWidth - w) / 2;
                }
            } else {
                float tr = 1.0f * targetHeight / targetWidth;
                float cr = 1.0f * curHeight / curWidth;
                if (tr <= cr) {
                    w = curWidth;
                    h = (int) (tr * w);
                    t = (curHeight - h) / 2;
                    l = 0;
                } else {
                    h = curWidth;
                    w = (int) (h / tr);
                    t = 0;
                    l = (curWidth - w) / 2;
                }
//                        }
            }
            System.out.println("t: " + t +
                    " l: " + l +
                    " w: " + w +
                    " h: " + h
            );
        }
    }

    public int testFun() {
        return 8;
    }

    @Test
    public void testCompute() {
        int ret = 1;
        if ((ret = testFun()) > 5) {
            System.out.println("ret: " + ret);
        }
    }
}
