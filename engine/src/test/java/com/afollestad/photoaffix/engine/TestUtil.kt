/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.net.Uri
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever

val testUriParser: UriParser = { data ->
  val uri = mock<Uri> {
    on { scheme } doReturn data.substring(0, data.indexOf('/') - 1)
    on { path } doReturn data.substring(data.indexOf('/') + 1)
    on { toString() } doReturn data
  }
  uri
}

fun fakeBitmap(
  width: Int,
  height: Int
): Bitmap {
  val bitmap = mock<Bitmap>()
  whenever(bitmap.width).doReturn(width)
  whenever(bitmap.height).doReturn(height)
  return bitmap
}
