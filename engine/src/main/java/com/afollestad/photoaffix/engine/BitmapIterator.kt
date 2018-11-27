/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.closeQuietely
import java.io.InputStream

/** @author Aidan Follestad (afollestad) */
internal class BitmapIterator(
  private val photos: List<Photo>,
  private val ioManager: IoManager
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
    val options = Options()
        .apply {
          inJustDecodeBounds = true
        }
    var inputStream: InputStream? = null

    try {
      inputStream = ioManager.openStream(nextPhoto.uri)
      BitmapFactory.decodeStream(inputStream, null, options)
      this.currentOptions = options
    } finally {
      inputStream.closeQuietely()
    }

    return options
  }

  fun currentBitmap(): Bitmap? {
    if (this.currentOptions == null) {
      throw IllegalStateException("Must call next() first.")
    }

    val nextPhoto = photos[traverseIndex]
    var inputStream: InputStream? = null

    return try {
      inputStream = ioManager.openStream(nextPhoto.uri)
      BitmapFactory.decodeStream(inputStream, null, this.currentOptions)
    } finally {
      inputStream.closeQuietely()
    }
  }

  fun size() = photos.size

  fun reset() {
    traverseIndex = -1
  }
}
