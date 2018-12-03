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

import android.graphics.Bitmap.CompressFormat
import com.afollestad.photoaffix.engine.bitmaps.BitmapIterator
import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.subengines.CommitResult
import com.afollestad.photoaffix.engine.subengines.DimensionsEngine
import com.afollestad.photoaffix.engine.subengines.ProcessingResult
import com.afollestad.photoaffix.engine.subengines.SizingResult
import com.afollestad.photoaffix.engine.subengines.StitchEngine
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.ext.extension
import org.jetbrains.annotations.TestOnly
import java.io.File
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface AffixEngine {

  suspend fun process(photos: List<Photo>): SizingResult

  suspend fun commit(
    scale: Double,
    width: Int,
    height: Int,
    format: CompressFormat,
    quality: Int
  ): CommitResult

  fun reset()
}

class RealAffixEngine @Inject constructor(
  private val ioManager: IoManager,
  private val bitmapManipulator: BitmapManipulator,
  private val dimensionsEngine: DimensionsEngine,
  private val stitchEngine: StitchEngine
) : AffixEngine {

  private lateinit var bitmapIterator: BitmapIterator

  override suspend fun process(photos: List<Photo>): SizingResult {
    this.bitmapIterator = BitmapIterator(
        photos = photos,
        bitmapManipulator = bitmapManipulator
    )
    return dimensionsEngine.calculateSize(bitmapIterator)
  }

  override suspend fun commit(
    scale: Double,
    width: Int,
    height: Int,
    format: CompressFormat,
    quality: Int
  ): CommitResult {
    val result = stitchEngine.stitch(
        bitmapIterator = bitmapIterator,
        selectedScale = scale,
        resultWidth = width,
        resultHeight = height,
        format = format,
        quality = quality
    )
    return commitResult(result)
  }

  private fun commitResult(processingResult: ProcessingResult): CommitResult {
    if (processingResult.none()) {
      processingResult.recycle()
      return CommitResult(error = Exception("No images were processed to commit"))
    }
    requireNotNull(processingResult.output) { "Generated Bitmap cannot be null." }

    // Save results to file
    val cacheFile: File? = ioManager.makeTempFile(
        extension = processingResult.format.extension()
    )
    requireNotNull(cacheFile) { "Generated temp file cannot be null." }

    return try {
      bitmapManipulator.encodeBitmap(
          bitmap = processingResult.output,
          format = processingResult.format,
          quality = processingResult.quality,
          file = cacheFile
      )
      CommitResult(outputFile = cacheFile)
    } catch (e: Exception) {
      cacheFile.delete()
      CommitResult(error = Exception("Unable to save your final image", e))
    } finally {
      processingResult.recycle()
    }
  }

  override fun reset() = bitmapIterator.reset()

  // Unit test setters

  @TestOnly internal fun setBitmapIterator(bitmapIterator: BitmapIterator) {
    this.bitmapIterator = bitmapIterator
  }

  @TestOnly internal fun getBitmapIterator() = this.bitmapIterator
}
