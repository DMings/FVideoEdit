package com.videoeditor.downloader.intubeshot.frame.deprecated;

@Deprecated
public class FrameLruCache {

//    private LinkedHashMap<String, FrameBitmap> mCacheMap;
//    private LinkedHashMap<String, FrameBitmap> mEvictionMap;
//    private final static int MAX_SIZE = 40 * 1024 * 1024;
//    private final static int DISK_SIZE = 8 * 1024 * 1024;
//    private final static int MAX_WIDTH = 120;
//    private final static int MAX_HEIGHT = 60;
//    private Context mContext;
//    private int mCacheSize = 0;
//    private int mEvictionSize = 0;
//    //
//    private HandlerThread mHandlerThread;
//    private Handler mHandler;
//    private boolean mQuit = false;
//    private Bitmap mPlaceholderBitmap;
//
//    public FrameLruCache(Context context) {
//        mContext = context.getApplicationContext();
//        mCacheMap = new LinkedHashMap<>(0, 0.75f, true);
//        mEvictionMap = new LinkedHashMap<>(0, 0.75f, true);
//        mHandlerThread = new HandlerThread("cache");
//        mHandlerThread.start();
//        mHandler = new Handler(mHandlerThread.getLooper());
//    }
//
//    public synchronized FrameBitmap getFrame(String key) { // 要同步的
//        FrameBitmap frameBitmap = mCacheMap.get(key);
//        if (frameBitmap != null) {
//            return frameBitmap;
//        }
//        frameBitmap = mEvictionMap.get(key);
//        if (frameBitmap == null) {
//            FrameKey frameKey = FrameKey.parseName(key);
//            frameBitmap = getFromDisk(key);
//            if (frameBitmap == null) { // 只有出错状态才会进入
//                if (mPlaceholderBitmap == null) {
//                    mPlaceholderBitmap = Bitmap.createBitmap(MAX_WIDTH, MAX_HEIGHT, Bitmap.Config.RGB_565);
//                    mPlaceholderBitmap.eraseColor(0);
//                }
//                frameBitmap = new FrameBitmap(frameKey.getTag(), frameKey.getTime(), mPlaceholderBitmap);
//            }
//        }
//        putFrame(frameBitmap); // 重新加入
//        return frameBitmap;
//    }
//
//    public synchronized void putFrame(FrameBitmap frameBitmap) { // 要同步的
//        mCacheMap.put(frameBitmap.getKey(), frameBitmap);
//        int size = sizeOf(frameBitmap);
//        mCacheSize += size;
//        if (mCacheSize >= MAX_SIZE) {// 超过规定的容量
//            Map.Entry<String, FrameBitmap> toEvict = mCacheMap.entrySet().iterator().next();
//            mCacheSize -= sizeOf(toEvict.getValue());
//            mCacheMap.remove(toEvict.getKey());
//            removed(toEvict.getValue());
//        }
////        FLog.i("mCacheSize: " + mCacheSize + " mEvictionSize: " + mEvictionSize);
//    }
//
//    private int sizeOf(FrameBitmap value) {
//        if (value.getBitmap() == null) {
//            return value.getWidth() * value.getHeight() * 2;
//        }
//        return value.getBitmap().getRowBytes() * value.getBitmap().getHeight() * 2;
//    }
//
//    private void removed(FrameBitmap frameBitmap) { // 内面的代码也在同步中跑
////        FLog.i("removed key: " + frameBitmap.getKey());
//        mEvictionMap.put(frameBitmap.getKey(), frameBitmap);
//        mEvictionSize += sizeOf(frameBitmap);
//        if (mEvictionSize >= DISK_SIZE && mContext != null) {
////            FLog.e("开始写入");
//            if (!mQuit) {
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        putToDisk(mContext);
//                    }
//                });
//            }
//        }
//    }
//
//    private void putToDisk(Context context) { // 在线程跑
//        // 先copy
//        LinkedHashMap<String, FrameBitmap> copyEvictionMap = new LinkedHashMap<>(0, 0.75f, true);
//        synchronized (this) { // 锁住，保证同步
//            for (Map.Entry<String, FrameBitmap> entry : mEvictionMap.entrySet()) {
//                copyEvictionMap.put(entry.getKey(), entry.getValue());
//                entry.getValue().setIsWrite(false);
//            }
//        }
//        // 使用copy
//        for (Map.Entry<String, FrameBitmap> entry : copyEvictionMap.entrySet()) {
//            if (mQuit) return;
//            FrameKey frameKey = FrameKey.parseName(entry.getKey());
//            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
//                    frameKey.getTag() + "/" + frameKey.getTime() + ".jpg");
//            if (!file.exists() && entry.getValue().getBitmap() != null) {
//                file.getParentFile().mkdirs();
//                FileOutputStream fileOutputStream = null;
//                try {
//                    fileOutputStream = new FileOutputStream(file);
//                    entry.getValue().getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
//                    fileOutputStream.flush();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    if (fileOutputStream != null) {
//                        try {
//                            fileOutputStream.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
////                    FLog.i("serializeDisk file: " + file.toString() + " file.exists: " + file.exists());
//                }
//            }
//            if (file.exists()) { // 检查是否写入成功了
//                entry.getValue().setIsWrite(true); // 写成功标记一下
//            }
//        }
//        synchronized (this) { // 锁住，保证同步
//            for (Map.Entry<String, FrameBitmap> entry : copyEvictionMap.entrySet()) {
//                mEvictionSize -= sizeOf(entry.getValue());
//                mEvictionMap.remove(entry.getKey());
//            }
//        }
//    }
//
//    private FrameBitmap getFromDisk(String key) {
//        FLog.i("getFromDisk>>>" + key);
//        if (mContext == null) {
//            return null;
//        }
//        FrameKey frameKey = FrameKey.parseName(key);
//        File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
//                frameKey.getTag() + "/" + frameKey.getTime() + ".jpg");
//        if (file.exists()) {
////            long time = System.currentTimeMillis();
////            Bitmap bitmap = BitmapFactory.decodeFile(file.toString());
////            FLog.i("cost time: " + (System.currentTimeMillis() - time));
////            if (bitmap == null) {
////                return null;
////            }
//            FrameBitmap frameBitmap = new FrameBitmap(frameKey.getTag(), frameKey.getTime(), null);
//            frameBitmap.setFile(file);
//            return frameBitmap;
//        }
//        return null;
//    }
//
//    public void release() {
//        mQuit = true;
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                mHandlerThread.quit();
//            }
//        });
//    }


}
