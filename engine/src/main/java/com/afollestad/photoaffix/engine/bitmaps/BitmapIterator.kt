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
