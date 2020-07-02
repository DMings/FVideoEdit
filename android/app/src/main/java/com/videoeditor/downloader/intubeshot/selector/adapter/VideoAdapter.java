package com.videoeditor.downloader.intubeshot.selector.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.selector.glide.VFrame;
import com.videoeditor.downloader.intubeshot.selector.model.VideoInfo;
import com.videoeditor.downloader.intubeshot.utils.FToastUtils;
import com.videoeditor.downloader.intubeshot.utils.FUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by DMing on 2019/12/5.
 */

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<VideoInfo> mVideoList = new ArrayList<>();
    private Context mContext;
    private int mPhotoMaxCount = 50;
    private ArrayList<String> mSelectVideoList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;
    private boolean mIsLongPressDown;
    private VideoInfo mCurVideoInfo;

    public void setVideoList(List<VideoInfo> videoList) {
        mVideoList.addAll(videoList);
        notifyDataSetChanged();
    }

    public void addVideoList(List<VideoInfo> videoList) {
        for (VideoInfo videoInfo : videoList) {
            mVideoList.add(0, videoInfo);
            notifyItemInserted(0);
        }
    }

    public ArrayList<String> getSelectVideoList() {
        return mSelectVideoList;
    }

    public void deleteSelectVideoList() {
//        int size = mVideoList.size();
//        for (int i = 0; i < size; i++) {
//            VideoInfo videoInfo = mVideoList.get(i);
//            if (videoInfo.isSelect()) {
//                notifyItemRemoved(i);
//            }
//        }
        Iterator<VideoInfo> iterator = mVideoList.iterator();
        while (iterator.hasNext()) {
            VideoInfo videoInfo = iterator.next();
            if (videoInfo.isSelect()) {
                iterator.remove();
            }
        }
        mSelectVideoList.clear();
        notifyDataSetChanged();
    }

    public void clearSelectVideoList() {
        for (int i = 0; i < mVideoList.size(); i++) {
            VideoInfo videoInfo = mVideoList.get(i);
            if (videoInfo.isSelect()) {
                videoInfo.setSelect(false);
                notifyItemChanged(i);
            }
        }
        mSelectVideoList.clear();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_video, parent, false);
        final VideoViewHolder videoViewHolder = new VideoViewHolder(view);
        videoViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = videoViewHolder.getAdapterPosition();
                VideoInfo videoInfo = mVideoList.get(position);
                if (videoInfo.isSelect()) {
                    videoInfo.setSelect(false);
                    mSelectVideoList.remove(videoInfo.getPath());
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onVideoSizeChange(mSelectVideoList.size());
                    }
                } else {
                    if (mSelectVideoList.size() >= mPhotoMaxCount) {
                        FToastUtils.show("最多选择" + mPhotoMaxCount + "个视频");
                        return; //不在往下执行
                    }
                    videoInfo.setSelect(true);
                    mSelectVideoList.add(videoInfo.getPath());
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onVideoSizeChange(mSelectVideoList.size());
                    }
                }
                notifyItemChanged(videoViewHolder.getAdapterPosition());
            }
        });
        videoViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = videoViewHolder.getAdapterPosition();
                mCurVideoInfo = mVideoList.get(position);
                if (mOnItemClickListener != null) {
                    mIsLongPressDown = true;
                    mOnItemClickListener.onItemLongClickDown(v, mCurVideoInfo);
                }
                return true;
            }
        });
        videoViewHolder.itemView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_POINTER_UP) {
                    longPressUp();
                }
                return false;
            }
        });
        return videoViewHolder;
    }

    public void longPressUp() {
        if (mOnItemClickListener != null && mIsLongPressDown) {
            mIsLongPressDown = false;
            mOnItemClickListener.onItemLongClickUp(null, mCurVideoInfo);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final VideoViewHolder holder, int position) {
        VideoInfo videoInfo = mVideoList.get(position);
        if (videoInfo.isSelect()) {
            holder.mIvCheck.setVisibility(View.VISIBLE);
            holder.mIvCheckBg.setVisibility(View.VISIBLE);
            holder.mIvCheck.setImageResource(R.drawable.ic_has_check);
        } else {
            holder.mIvCheck.setVisibility(View.INVISIBLE);
            holder.mIvCheckBg.setVisibility(View.INVISIBLE);
        }
        holder.mLlFolder.setVisibility(View.VISIBLE);
        holder.mTvTime.setText(FUtils.getSecTimeText(videoInfo.getDuration()));
        holder.mTvName.setText(videoInfo.getTitle());
        Glide.with(mContext)
                .load(new VFrame(videoInfo.getPath()))
                .placeholder(R.drawable.bg_color_white)
                .centerCrop()
                .into(holder.mIvPic);
    }

    @Override
    public int getItemCount() {
        return mVideoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {

        private ImageView mIvPic;
        private ImageView mIvCheck;
        private View mIvCheckBg;
        private TextView mTvTime;
        private TextView mTvName;
        private LinearLayout mLlFolder;

        VideoViewHolder(View itemView) {
            super(itemView);
            mIvPic = itemView.findViewById(R.id.iv_pic);
            mIvCheck = itemView.findViewById(R.id.iv_check);
            mIvCheckBg = itemView.findViewById(R.id.iv_check_bg);
            mTvTime = itemView.findViewById(R.id.tv_time);
            mTvName = itemView.findViewById(R.id.tv_name);
            mLlFolder = itemView.findViewById(R.id.ll_folder);
        }

    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onVideoSizeChange(int size);

        void onItemLongClickDown(View view, VideoInfo videoInfo);

        void onItemLongClickUp(View view, VideoInfo videoInfo);
    }
}
