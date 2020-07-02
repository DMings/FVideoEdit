package com.videoeditor.downloader.intubeshot.frame;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videoeditor.downloader.intubeshot.R;
import com.videoeditor.downloader.intubeshot.loader.FrameLoader;

import java.util.ArrayList;
import java.util.List;

import static com.videoeditor.downloader.intubeshot.frame.FrameEntity.ITEM_FOOTER;
import static com.videoeditor.downloader.intubeshot.frame.FrameEntity.ITEM_HEADER;

public class FrameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<FrameEntity> mFrameEntityList = new ArrayList<>();
    //
    private int mHFWidth;
    private int mItemWidth;
    private int mItemHeight;
    private Context mContext;

    public FrameAdapter(Context context) {
        mContext = context;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(outMetrics);
        } else {
            outMetrics.widthPixels = 720;
        }
        mHFWidth = outMetrics.widthPixels / 2;
        mItemHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 46,
                context.getResources().getDisplayMetrics());
        mItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72,
                context.getResources().getDisplayMetrics());
    }

    public int getItemWidth() {
        return mItemWidth;
    }

    public int getItemHeight() {
        return mItemHeight;
    }

    public int getHFWidth() {
        return mHFWidth;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == ITEM_HEADER) {
            View view = new View(viewGroup.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(mHFWidth, mItemHeight));
            return new HeaderFooterViewHolder(view);
        } else if (viewType == ITEM_FOOTER) {
            View view = new View(viewGroup.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(mHFWidth, mItemHeight));
            return new HeaderFooterViewHolder(view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_frame, viewGroup, false);
            return new FrameViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof FrameViewHolder) {
            FrameViewHolder frameViewHolder = (FrameViewHolder) viewHolder;
            FrameEntity frameEntity = mFrameEntityList.get(position);
            if (frameEntity.getPercentSize() != 1.0f) {
                frameViewHolder.itemView.setLayoutParams(
                        new ViewGroup.LayoutParams((int) (mItemWidth * frameEntity.getPercentSize()), mItemHeight));
            } else {
                if (frameViewHolder.itemView.getLayoutParams().width != mItemWidth) {
                    frameViewHolder.itemView.setLayoutParams(
                            new ViewGroup.LayoutParams(mItemWidth, mItemHeight));
                }
            }
            FrameLoader.with(mContext).load(frameEntity.getFrameKey()).into(frameViewHolder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return mFrameEntityList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mFrameEntityList.get(position).getType();
    }

    public FrameEntity getFrameEntity(int position) {
        return mFrameEntityList.get(position);
    }

    public void clear() {
        mFrameEntityList.clear();
        notifyDataSetChanged();
    }

    public void addHeader() {
        mFrameEntityList.add(new FrameEntity(ITEM_HEADER));
        notifyItemInserted(0);
    }

    public void addVideoFrameList(List<FrameEntity> frameList) {
        mFrameEntityList.addAll(frameList);
        notifyItemInserted(mFrameEntityList.size() - 1);
    }

    public void addFooter() {
        mFrameEntityList.add(new FrameEntity(ITEM_HEADER));
        notifyItemInserted(mFrameEntityList.size() - 1 >= 0 ? mFrameEntityList.size() - 1 : 0);
    }

    public void updateFrameSequence(List<FrameEntity> frameList) {
        mFrameEntityList.clear();
        mFrameEntityList.add(new FrameEntity(ITEM_HEADER));
        mFrameEntityList.addAll(frameList);
        mFrameEntityList.add(new FrameEntity(ITEM_FOOTER));
        notifyDataSetChanged();
    }

    public static class FrameViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;

        public FrameViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_frame);
        }
    }

    public static class HeaderFooterViewHolder extends RecyclerView.ViewHolder {
        public HeaderFooterViewHolder(View itemView) {
            super(itemView);
        }
    }

}
