/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import android.graphics.BitmapFactory.decodeStream
import android.net.Uri
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.closeQuietely
import java.io.InputStream
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface BitmapDecoder {

  fun decodePhoto(
    photo: Photo,
    options: Options
  ): Bitmap?

  fun decodeUri(
    uri: Uri,
    options: Options
  ): Bitmap?

  fun createOptions(onlyGetBounds: Boolean = true): Options
}

class RealBitmapDecoder @Inject constructor(
  private val ioManager: IoManager
) : BitmapDecoder {

  override fun decodePhoto(
    photo: Photo,
    options: Options
  ) = decodeUri(photo.uri, options)

  override fun decodeUri(
    uri: Uri,
    options: Options
  ): Bitmap? {
    var inputStream: InputStream? = null
    try {
      inputStream = ioManager.openStream(uri)
      return decodeStream(inputStream, null, options)
    } finally {
      inputStream.closeQuietely()
    }
  }

  override fun createOptions(onlyGetBounds: Boolean): Options {
    return Options()
        .apply {
          inJustDecodeBounds = onlyGetBounds
        }
  }
}
