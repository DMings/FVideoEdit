package com.videoeditor.downloader.intubeshot.selector;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by DMing on 2019/12/1.
 * 获取所有本地图片
 */

public class PickerManager {

    private List<VideoInfo> mVideoList;

//    public Observable<List<VideoInfo>> getImageList(final Context context) {
//        return Observable.create(new ObservableOnSubscribe<List<VideoInfo>>() {
//            @Override
//            public void subscribe(ObservableEmitter<List<VideoInfo>> emitter) throws Throwable {
//                if (context != null && (mVideoList == null || mVideoList.size() == 0)) {
//                    ContentResolver contentResolver = context.getContentResolver();
//                    if (contentResolver != null) {
//                        try {
//                            Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//                            loading(contentResolver, uri);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                emitter.onNext(mVideoList);
//            }
//        });
//    }

    public void handleImageList(final Context context, final OnDataAvailableListener onDataAvailableListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (context != null && (mVideoList == null || mVideoList.size() == 0)) {
                    ContentResolver contentResolver = context.getContentResolver();
                    if (contentResolver != null) {
                        try {
                            Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            loading(contentResolver, uri);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                onDataAvailableListener.onAvailable(mVideoList);
            }
        }).start();
    }

    private void loading(ContentResolver contentResolver, Uri uri) {
        Cursor cursor = contentResolver.query(uri, null, null, null,
                MediaStore.Video.Media.DATE_MODIFIED + " DESC");
        if (cursor != null) {
            mVideoList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        VideoInfo info = new VideoInfo();
                        int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                        long lastModified = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED));
                        info.setId(id);
                        info.setPath(path);
                        info.setUri(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id));
                        info.setTitle(title);
                        info.setDuration(duration);
                        info.setLastModified(lastModified);
                        if (id == -1 || size == -1) {
                            continue;
                        }
                        mVideoList.add(info);
                    }
                }
            }
            cursor.close();
            sortVideoList();
        }
    }

    private List<VideoInfo> sortVideoList() {
        if (mVideoList.size() > 0) {
            Collections.sort(mVideoList, new Comparator<VideoInfo>() {
                @Override
                public int compare(VideoInfo o1, VideoInfo o2) {
                    return PickerManager.compare(o2.getLastModified(), o1.getLastModified());
                }
            });
        }
        return mVideoList;
    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }


    public interface OnDataAvailableListener {
        void onAvailable(List<VideoInfo> mVideoList);
    }
}
