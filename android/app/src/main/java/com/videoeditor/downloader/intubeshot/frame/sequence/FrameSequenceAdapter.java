package com.videoeditor.downloader.intubeshot.frame.sequence;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.loader.VideoFrame;
import com.videoeditor.downloader.intubeshot.loader.VideoFrameManager;
import com.videoeditor.downloader.intubeshot.selector.glide.VFrame;
import com.videoeditor.downloader.intubeshot.utils.FUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FrameSequenceAdapter extends RecyclerView.Adapter<FrameSequenceAdapter.FrameSequenceViewHolder> {

    private List<VideoFrame> mVideoFrameList = new ArrayList<>();
    private Context mContext;
    private int mMaxItemSize;
    private int mMinItemSize;
    private OnItemSelectListener mOnItemSelectListener;

    public FrameSequenceAdapter(Context context) {
        mContext = context;
        mMaxItemSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 65,
                context.getResources().getDisplayMetrics());
        mMinItemSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50,
                context.getResources().getDisplayMetrics());
    }

    @NonNull
    @Override
    public FrameSequenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sequence_frame, parent, false);
        final FrameSequenceViewHolder viewHolder = new FrameSequenceViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkIndex = -1;
                for (int i = 0; i < mVideoFrameList.size(); i++) {
                    if (mVideoFrameList.get(i).isCheck()) {
                        checkIndex = i;
                        mVideoFrameList.get(i).setIsCheck(false);
                        notifyItemChanged(i);
                    }
                }
                int pos = viewHolder.getAdapterPosition();
                VideoFrame videoFrame = mVideoFrameList.get(pos);
                videoFrame.setIsCheck(true);
                notifyItemChanged(viewHolder.getAdapterPosition());
                if (mOnItemSelectListener != null) {
                    mOnItemSelectListener.onSelected(checkIndex == pos ? null : videoFrame);
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FrameSequenceViewHolder holder, int position) {
        VideoFrame videoFrame = mVideoFrameList.get(position);
        if (videoFrame.isCheck()) {
            ViewGroup.LayoutParams layoutParams = holder.imageView.getLayoutParams();
            layoutParams.width = mMaxItemSize;
            layoutParams.height = mMaxItemSize;
            holder.imageView.setLayoutParams(layoutParams);
            //
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.width = mMaxItemSize;
            params.height = mMaxItemSize;
            holder.itemView.setLayoutParams(params);
            //
            holder.frameLayout.setVisibility(View.VISIBLE);
        } else {
            ViewGroup.LayoutParams layoutParams = holder.imageView.getLayoutParams();
            layoutParams.width = mMinItemSize;
            layoutParams.height = mMinItemSize;
            holder.imageView.setLayoutParams(layoutParams);
            //
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.width = mMinItemSize;
            params.height = mMaxItemSize;
            holder.itemView.setLayoutParams(params);
            //
            holder.frameLayout.setVisibility(View.INVISIBLE);
        }
        holder.textView.setText(FUtils.getSecTimeText(videoFrame.getOffsetDurationMs()));
        Glide.with(mContext)
                .load(new VFrame(videoFrame.getFile().getPath(), videoFrame.getStartTime()))
                .placeholder(R.drawable.bg_color_white)
                .centerCrop()
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mVideoFrameList.size();
    }

    public void updateVideoFrameList() {
        mVideoFrameList.clear();
        mVideoFrameList.addAll(VideoFrameManager.getInstance().getVideoFrameList());
        notifyDataSetChanged();
    }

//    public List<VideoFrame> getVideoFrameList() {
//        return mVideoFrameList;
//    }

    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mVideoFrameList, fromPosition, toPosition);
        Collections.swap(VideoFrameManager.getInstance().getVideoFrameList(), fromPosition, toPosition);
        VideoFrameManager.getInstance().updateVideoFrameListTime();
        notifyItemMoved(fromPosition, toPosition);
    }

//    void onItemSelected(int position) {
//        for (int i = 0; i < mVideoFrameList.size(); i++) {
//            if (mVideoFrameList.get(i).isCheck()) {
//                mVideoFrameList.get(i).setIsCheck(false);
//                notifyItemChanged(i);
//            }
//        }
//        VideoFrame videoFrame = mVideoFrameList.get(position);
//        videoFrame.setIsCheck(true);
//        notifyItemChanged(position);
//    }

    public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        mOnItemSelectListener = onItemSelectListener;
    }

    public static class FrameSequenceViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public FrameLayout frameLayout;
        public TextView textView;

        public FrameSequenceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_frame);
            frameLayout = itemView.findViewById(R.id.fl_frame);
            textView = itemView.findViewById(R.id.tv_time);
        }
    }

    public interface OnItemSelectListener {
        void onSelected(VideoFrame videoFrame);
    }
}
