/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.net.Uri
import com.afollestad.photoaffix.engine.photos.UriParser
import com.afollestad.photoaffix.utilities.DpConverter
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ScanResult
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
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

fun testDpConverter() = object : DpConverter {
  override fun toDp(pixels: Int) = pixels.toFloat()
}

fun testMediaScanner(): MediaScanner {
  val mediaScanner = mock<MediaScanner>()
  val fakeUri = mock<Uri>()
  doAnswer { inv ->
    val path = inv.getArgument<String>(0)
    val callback = inv.getArgument<ScanResult?>(1)
    callback?.invoke(path, fakeUri)
    null
  }.whenever(mediaScanner)
      .scan(any(), any())
  return mediaScanner
}
