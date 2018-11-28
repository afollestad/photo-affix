/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.BitmapFactory.Options

data class Size(
  val width: Int,
  val height: Int
) {

  fun isZero() = width <= 0 || height <= 0

  companion object {
    fun fromOptions(options: Options): Size {
      return Size(options.outWidth, options.outHeight)
    }
  }
}
