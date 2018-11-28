/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine.bitmaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import com.afollestad.photoaffix.engine.photos.Photo
import org.jetbrains.annotations.TestOnly

/** @author Aidan Follestad (afollestad) */
class BitmapIterator(
  private val photos: List<Photo>,
  private val bitmapManipulator: BitmapManipulator
) : Iterator<Options> {

  private var traverseIndex: Int = -1
  private var currentOptions: Options? = null

  override fun hasNext(): Boolean {
    return traverseIndex + 1 < photos.size
  }

  override fun next(): Options {
    this.currentOptions = null
    check(++traverseIndex < photos.size) { "No more options." }

    val nextPhoto = photos[traverseIndex]
    val options = bitmapManipulator.createOptions(
        onlyGetBounds = true
    )
    bitmapManipulator.decodePhoto(nextPhoto, options)

    check(options.outWidth != 0 && options.outHeight != 0) {
      "decodePhoto(Photo, Options) should retrieve non-zero Bitmap dimensions here."
    }

    // Options' properties have now been populated
    this.currentOptions = options
    return options
  }

  fun currentBitmap(): Bitmap {
    requireNotNull(this.currentOptions) { "Must call next() first." }
    val currentPhoto = photos[traverseIndex]
    return bitmapManipulator.decodePhoto(
        currentPhoto,
        this.currentOptions!!.apply {
          inJustDecodeBounds = false
        })!!
  }

  fun size() = photos.size

  fun reset() {
    traverseIndex = -1
  }

  @TestOnly fun currentOptions() = currentOptions

  @TestOnly fun traverseIndex() = traverseIndex
}
