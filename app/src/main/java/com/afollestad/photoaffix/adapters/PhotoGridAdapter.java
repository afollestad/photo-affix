package com.afollestad.photoaffix.adapters;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.data.Photo;
import com.afollestad.photoaffix.data.PhotoHolder;
import com.afollestad.photoaffix.ui.MainActivity;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * @author Aidan Follestad (afollestad)
 */
public class PhotoGridAdapter extends RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    public PhotoGridAdapter(MainActivity context) {
        mContext = context;
        mSelectedIndices = new ArrayList<>();
    }

    private MainActivity mContext;
    private Photo[] mPhotos;
    private ArrayList<Integer> mSelectedIndices;

    public void saveInstanceState(Bundle out) {
        if (mPhotos != null)
            out.putSerializable("photos", new PhotoHolder(mPhotos));
        out.putSerializable("selected_indices", mSelectedIndices);
    }

    public void restoreInstanceState(Bundle in) {
        if (in != null) {
            if (in.containsKey("selected_indices")) {
                //noinspection unchecked
                mSelectedIndices = (ArrayList<Integer>) in.getSerializable("selected_indices");
                if (mSelectedIndices == null) mSelectedIndices = new ArrayList<>();
            }
            if (in.containsKey("photos")) {
                PhotoHolder ph = (PhotoHolder) in.getSerializable("photos");
                if (ph != null) setPhotos(ph.photos);
            }
        }
    }

    public void setPhotos(Photo[] photos) {
        mPhotos = photos;
        notifyDataSetChanged();
    }

    public void toggleSelected(int index) {
        if (mSelectedIndices.contains(index)) {
            mSelectedIndices.remove((Integer) index);
        } else {
            mSelectedIndices.add(index);
        }
        notifyItemChanged(index);
        if (mContext != null)
            mContext.onSelectionChanged(mSelectedIndices.size());
    }

    public void selectRange(int from, int to, int min, int max) {
        if (from == to) {
            // Finger is back on the initial item, unselect everything else
            for (int i = min; i <= max; i++) {
                if (i == from) continue;
                if (mSelectedIndices.contains(i)) {
                    mSelectedIndices.remove((Integer) i);
                    notifyItemChanged(i);
                }
            }
            return;
        }

        if (to < from) {
            // When selecting from one to previous items
            for (int i = to; i <= from; i++) {
                if (!mSelectedIndices.contains(i)) {
                    mSelectedIndices.add(i);
                    notifyItemChanged(i);
                }
            }
            if (min > -1 && min < to) {
                // Unselect items that were selected during this drag but no longer are
                for (int i = min; i < to; i++) {
                    if (mSelectedIndices.contains(i)) {
                        mSelectedIndices.remove((Integer) i);
                        notifyItemChanged(i);
                    }
                }
            }
        } else {
            // When selecting from one to next items
            for (int i = from; i <= to; i++) {
                if (!mSelectedIndices.contains(i)) {
                    mSelectedIndices.add(i);
                    notifyItemChanged(i);
                }
            }
            if (max > -1 && max > to) {
                // Unselect items that were selected during this drag but no longer are
                for (int i = to + 1; i <= max; i++) {
                    if (mSelectedIndices.contains(i)) {
                        mSelectedIndices.remove((Integer) i);
                        notifyItemChanged(i);
                    }
                }
            }
        }
        if (mContext != null)
            mContext.onSelectionChanged(mSelectedIndices.size());
    }

    public void clearSelected() {
        mSelectedIndices.clear();
        notifyDataSetChanged();
        if (mContext != null)
            mContext.onSelectionChanged(0);
    }

    public int getSelectedCount() {
        return mSelectedIndices.size();
    }

    public Photo[] getSelectedPhotos() {
        ArrayList<Photo> selected = new ArrayList<>();
        for (Integer index : mSelectedIndices)
            selected.add(mPhotos[index]);
        return selected.toArray(new Photo[selected.size()]);
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.griditem_photo, parent, false);
        return new PhotoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        Glide.with(mContext)
                .load(mPhotos[position].getUri())
                .into(holder.image);
        if (mSelectedIndices.contains(position)) {
            holder.check.setVisibility(View.VISIBLE);
            holder.circle.setActivated(true);
            holder.image.setActivated(true);
        } else {
            holder.check.setVisibility(View.GONE);
            holder.circle.setActivated(false);
            holder.image.setActivated(false);
        }

        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(this);
        holder.itemView.setOnLongClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mPhotos != null ? mPhotos.length : 0;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            int index = (Integer) v.getTag();
            toggleSelected(index);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getTag() != null) {
            int index = (Integer) v.getTag();
            toggleSelected(index);
            mContext.mList.setDragSelectActive(true, index);
        }
        return false;
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder {

        final ImageView image;
        final View check;
        final View circle;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            image = ButterKnife.findById(itemView, R.id.image);
            check = itemView.findViewById(R.id.check);
            circle = itemView.findViewById(R.id.circle);
        }
    }
}