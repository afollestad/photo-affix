/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.dragselectrecyclerview.DragSelectReceiver
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.views.MainActivity
import com.afollestad.photoaffix.data.Photo
import com.afollestad.photoaffix.data.PhotoHolder
import com.afollestad.photoaffix.utilities.loadImage
import kotlinx.android.synthetic.main.griditem_photo.view.check
import kotlinx.android.synthetic.main.griditem_photo.view.circle
import kotlinx.android.synthetic.main.griditem_photo.view.image

typealias SelectionListener = (position: Int, count: Int) -> Unit

class PhotoViewHolder(
  itemView: View,
  private val adapter: PhotoGridAdapter
) : RecyclerView.ViewHolder(itemView) {

  init {
    val context = itemView.context as MainActivity
    itemView.setOnClickListener {
      if (adapterPosition == 0) {
        context.browseExternalPhotos()
      } else {
        adapter.toggleSelected(adapterPosition)
      }
    }

    if (itemView.image != null) {
      itemView.setOnLongClickListener { v ->
        adapter.toggleSelected(adapterPosition)
        adapter.dragListener?.setIsActive(true, adapterPosition)
        false
      }
    }
  }

  fun bind(photo: Photo) {
    itemView.image.loadImage(photo.uri)

    if (adapter.isSelected(adapterPosition)) {
      itemView.check.visibility = VISIBLE
      itemView.circle.isActivated = true
      itemView.image.isActivated = true
    } else {
      itemView.check.visibility = GONE
      itemView.circle.isActivated = false
      itemView.image.isActivated = false
    }
  }
}

/** @author Aidan Follestad (afollestad) */
class PhotoGridAdapter(val context: MainActivity) : RecyclerView.Adapter<PhotoViewHolder>(),
    DragSelectReceiver {

  companion object {
    private const val KEY_PHOTOS = "photos"
    private const val KEY_SELECTED_INDICES = "selectedIndices"
  }

  val selectedPhotos: List<Photo>
    get() {
      return selectedIndices.map { photos[it - 1] }
    }
  var dragListener: DragSelectTouchListener? = null

  private var photos = listOf<Photo>()
  private var selectedIndices = mutableListOf<Int>()
  private var selectionListener: SelectionListener? = null

  fun onSelection(selection: SelectionListener) {
    this.selectionListener = selection
  }

  fun hasSelection() = selectedIndices.isNotEmpty()

  fun toggleSelected(index: Int) = setSelected(index, !isSelected(index))

  fun shiftSelections() {
    var i = 1
    while (i < itemCount - 1) {
      val currentSelected = isSelected(i)
      if (currentSelected) {
        setSelected(i + 1, true)
        setSelected(i, false)
        i++
      }
      i++
    }
  }

  fun clearSelected() {
    selectedIndices.clear()
    this.selectionListener?.invoke(0, 0)
    notifyDataSetChanged()
  }

  fun setPhotos(photos: List<Photo>) {
    this.photos = photos
    notifyDataSetChanged()
  }

  fun saveInstanceState(out: Bundle) {
    if (photos.isNotEmpty()) {
      out.putSerializable(KEY_PHOTOS, PhotoHolder(photos))
      out.putIntArray(KEY_SELECTED_INDICES, selectedIndices.toIntArray())
    }
  }

  fun restoreInstanceState(savedState: Bundle?) {
    if (savedState?.containsKey(KEY_PHOTOS) == true) {
      val ph = savedState.getSerializable(KEY_PHOTOS) as? PhotoHolder ?: return
      setPhotos(ph.photos)
    }
    savedState?.getIntArray(KEY_SELECTED_INDICES)
        ?.let {
          this.selectedIndices = it.toMutableList()
          notifyDataSetChanged()
        }
  }

  override fun isSelected(index: Int) = selectedIndices.contains(index)

  override fun isIndexSelectable(index: Int) = index > 0

  override fun setSelected(
    index: Int,
    selected: Boolean
  ) {
    if (selected && !isSelected(index) && isIndexSelectable(index)) {
      selectedIndices.add(index)
    } else if (!selected) {
      selectedIndices.remove(index)
    }
    this.selectionListener?.invoke(index, selectedIndices.size)
    notifyItemChanged(index)
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) 0 else 1
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): PhotoViewHolder {
    val res =
      if (viewType == 0) R.layout.griditem_browse
      else R.layout.griditem_photo
    val view = LayoutInflater.from(parent.context)
        .inflate(res, parent, false)
    return PhotoViewHolder(view, this)
  }

  override fun onBindViewHolder(
    holder: PhotoViewHolder,
    position: Int
  ) {
    if (context.isFinishing || position == 0) return
    val photo = photos[position - 1]
    holder.bind(photo)
  }

  override fun getItemCount() = photos.size + 1
}
