/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine.subengines

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.CompressFormat.PNG

data class ProcessingResult(
  val processedCount: Int,
  val output: Bitmap? = null,
  val format: CompressFormat = PNG,
  val quality: Int = 100
) {

  fun none() = processedCount == 0

  fun recycle() = output?.recycle()
}
