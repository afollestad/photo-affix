/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.net.Uri
import com.afollestad.photoaffix.engine.photos.UriParser
import com.afollestad.photoaffix.utilities.DpConverter
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
