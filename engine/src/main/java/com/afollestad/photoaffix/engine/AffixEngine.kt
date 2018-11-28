/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap.CompressFormat
import androidx.annotation.VisibleForTesting
import com.afollestad.photoaffix.engine.bitmaps.BitmapIterator
import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.subengines.DimensionsEngine
import com.afollestad.photoaffix.engine.subengines.ProcessingResult
import com.afollestad.photoaffix.engine.subengines.StitchEngine
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ext.extension
import com.afollestad.photoaffix.utilities.qualifiers.IoDispatcher
import com.afollestad.photoaffix.utilities.qualifiers.MainDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/** @author Aidan Follestad (afollestad) */
interface AffixEngine {

  fun process(
    photos: List<Photo>,
    engineOwner: EngineOwner
  )

  fun confirmSize(
    scale: Double,
    width: Int,
    height: Int,
    format: CompressFormat,
    quality: Int
  )

  fun reset()
}

class RealAffixEngine @Inject constructor(
  private val mediaScanner: MediaScanner,
  private val ioManager: IoManager,
  private val bitmapManipulator: BitmapManipulator,
  @MainDispatcher private val mainContext: CoroutineContext,
  @IoDispatcher private val ioContext: CoroutineContext,
  private val dimensionsEngine: DimensionsEngine,
  private val stitchEngine: StitchEngine
) : AffixEngine {

  private lateinit var engineOwner: EngineOwner
  private lateinit var bitmapIterator: BitmapIterator

  override fun process(
    photos: List<Photo>,
    engineOwner: EngineOwner
  ) {
    this.bitmapIterator = BitmapIterator(
        photos = photos,
        bitmapManipulator = bitmapManipulator
    )
    this.engineOwner = engineOwner
    this.dimensionsEngine.setup(bitmapIterator, engineOwner)

    GlobalScope.launch(ioContext) {
      val size = dimensionsEngine.calculateSize()
      if (size.isZero()) {
        return@launch
      }

      withContext(mainContext) {
        engineOwner.showImageSizingDialog(
            size.width,
            size.height
        )
      }
    }
  }

  override fun confirmSize(
    scale: Double,
    width: Int,
    height: Int,
    format: CompressFormat,
    quality: Int
  ) {
    stitchEngine.setup(bitmapIterator, engineOwner)

    GlobalScope.launch(ioContext) {
      val result = stitchEngine.stitch(
          selectedScale = scale,
          resultWidth = width,
          resultHeight = height,
          format = format,
          quality = quality
      )

      val outputFile = commitResult(result)
      withContext(mainContext) {
        outputFile?.let(this@RealAffixEngine::done)
      }
    }
  }

  @VisibleForTesting suspend fun commitResult(processingResult: ProcessingResult): File? {
    if (processingResult.none()) {
      processingResult.recycle()
      withContext(mainContext) { engineOwner.showContentLoading(false) }
      return null
    }
    requireNotNull(processingResult.output) { "Generated Bitmap cannot be null." }

    // Save results to file
    var cacheFile: File? = ioManager.makeTempFile(
        extension = processingResult.format.extension()
    )
    requireNotNull(cacheFile) { "Generated temp file cannot be null." }

    try {
      bitmapManipulator.encodeBitmap(
          bitmap = processingResult.output,
          format = processingResult.format,
          quality = processingResult.quality,
          file = cacheFile
      )
    } catch (e: Exception) {
      cacheFile.delete()
      cacheFile = null
      withContext(mainContext) { engineOwner.showErrorDialog(e) }
    }

    processingResult.recycle()
    return cacheFile
  }

  @VisibleForTesting fun done(file: File) {
    // Add the affixed file to the media store so gallery apps can see it
    mediaScanner.scan(file.toString()) { _, uri ->
      engineOwner.onDoneProcessing()
      engineOwner.showContentLoading(false)
      engineOwner.launchViewer(uri)
    }
  }

  override fun reset() = bitmapIterator.reset()

  // Unit test setters

  @TestOnly internal fun setEngineOwner(engineOwner: EngineOwner) {
    this.engineOwner = engineOwner
  }

  @TestOnly internal fun getEngineOwner() = this.engineOwner

  @TestOnly internal fun getBitmapIterator() = this.bitmapIterator
}
