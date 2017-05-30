package com.afollestad.photoaffix.adapters;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.data.Photo;
import com.afollestad.photoaffix.data.PhotoHolder;
import com.afollestad.photoaffix.ui.MainActivity;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/** @author Aidan Follestad (afollestad) */
public class PhotoGridAdapter
    extends DragSelectRecyclerViewAdapter<PhotoGridAdapter.PhotoViewHolder> {

  private final MainActivity context;
  private Photo[] photos;

  public PhotoGridAdapter(MainActivity context) {
    this.context = context;
  }

  @Override
  public void saveInstanceState(Bundle out) {
    super.saveInstanceState(out);
    if (photos != null) {
      out.putSerializable("photos", new PhotoHolder(photos));
    }
  }

  @Override
  public void restoreInstanceState(Bundle in) {
    super.restoreInstanceState(in);
    if (in != null && in.containsKey("photos")) {
      PhotoHolder ph = (PhotoHolder) in.getSerializable("photos");
      if (ph != null) {
        setPhotos(ph.photos);
      }
    }
  }

  public void shiftSelections() {
    for (int i = 1; i < getItemCount() - 1; i++) {
      boolean currentSelected = isIndexSelected(i);
      if (currentSelected) {
        setSelected(i + 1, true);
        setSelected(i, false);
        i++;
      }
    }
  }

  public void setPhotos(Photo[] photos) {
    this.photos = photos;
    notifyDataSetChanged();
  }

  public Photo[] getSelectedPhotos() {
    Integer[] indices = getSelectedIndices();
    ArrayList<Photo> selected = new ArrayList<>();
    for (Integer index : indices) {
      if (index < 0) {
        continue;
      }
      selected.add(photos[index - 1]);
    }
    return selected.toArray(new Photo[selected.size()]);
  }

  @Override
  public int getItemViewType(int position) {
    return position == 0 ? 0 : 1;
  }

  @Override
  public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(
                viewType == 0 ? R.layout.griditem_browse : R.layout.griditem_photo, parent, false);
    return new PhotoViewHolder(v);
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public void onBindViewHolder(PhotoViewHolder holder, int position) {
    super.onBindViewHolder(holder, position);
    if (context == null || context.isFinishing()) {
      return;
    }
    if (position == 0) {
      return;
    }

    Glide.with(context).load(photos[position - 1].getUri()).into(holder.image);

    if (isIndexSelected(position)) {
      holder.check.setVisibility(View.VISIBLE);
      holder.circle.setActivated(true);
      holder.image.setActivated(true);
    } else {
      holder.check.setVisibility(View.GONE);
      holder.circle.setActivated(false);
      holder.image.setActivated(false);
    }
  }

  @Override
  protected boolean isIndexSelectable(int index) {
    return index > 0;
  }

  @Override
  public int getItemCount() {
    return photos != null ? photos.length + 1 : 0;
  }

  class PhotoViewHolder extends RecyclerView.ViewHolder {

    @Nullable
    @BindView(R.id.image)
    ImageView image;

    @Nullable
    @BindView(R.id.check)
    View check;

    @Nullable
    @BindView(R.id.circle)
    View circle;

    PhotoViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);

      itemView.setOnClickListener(
          v -> {
            if (getAdapterPosition() == 0) {
              context.browseExternalPhotos();
              return;
            }
            toggleSelected(getAdapterPosition());
          });

      if (image != null) {
        itemView.setOnLongClickListener(
            v -> {
              toggleSelected(getAdapterPosition());
              context.list.setDragSelectActive(true, getAdapterPosition());
              return false;
            });
      }
    }
  }
}
