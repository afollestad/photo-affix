/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import org.jetbrains.annotations.TestOnly

/** @author Aidan Follestad (afollestad) */
class BitmapIterator(
  private val photos: List<Photo>,
  private val bitmapDecoder: BitmapDecoder
) : Iterator<Options> {

  private var traverseIndex: Int = -1
  private var currentOptions: Options? = null

  override fun hasNext(): Boolean {
    return traverseIndex + 1 < photos.size
  }

  override fun next(): Options {
    this.currentOptions = null
    if (++traverseIndex > photos.size - 1) {
      throw IllegalStateException("No more options.")
    }

    val nextPhoto = photos[traverseIndex]
    val options = bitmapDecoder.createOptions(true)

    bitmapDecoder.decodePhoto(nextPhoto, options)
    // Options' properties have now been populated
    this.currentOptions = options

    return options
  }

  fun currentBitmap(): Bitmap? {
    if (this.currentOptions == null) {
      throw IllegalStateException("Must call next() first.")
    }
    val nextPhoto = photos[traverseIndex]
    return bitmapDecoder.decodePhoto(nextPhoto, this.currentOptions!!)
  }

  fun size() = photos.size

  fun reset() {
    traverseIndex = -1
  }

  @TestOnly fun currentOptions() = currentOptions

  @TestOnly fun traverseIndex() = traverseIndex
}
