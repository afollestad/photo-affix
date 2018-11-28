/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.DENSITY_NONE
import android.graphics.Canvas
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.util.TypedValue.applyDimension
import androidx.annotation.VisibleForTesting
import com.afollestad.photoaffix.prefs.BgFillColor
import com.afollestad.photoaffix.prefs.ImageSpacingHorizontal
import com.afollestad.photoaffix.prefs.ImageSpacingVertical
import com.afollestad.photoaffix.prefs.ScalePriority
import com.afollestad.photoaffix.prefs.StackHorizontally
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ext.extension
import com.afollestad.photoaffix.utilities.ext.safeRecycle
import com.afollestad.photoaffix.utilities.ext.toRoundedInt
import com.afollestad.rxkprefs.Pref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal typealias DpConverter = (Int) -> Float
internal typealias CanvasCreator = (Bitmap) -> Canvas
internal typealias PaintCreator = () -> Paint
internal typealias RectCreator = (left: Int, top: Int, right: Int, bottom: Int) -> Rect

/** @author Aidan Follestad (afollestad) */
interface AffixEngine {

  fun process(
    photos: List<Photo>,
    engineOwner: EngineOwner
  )

  fun onSizeConfirmed(
    scale: Double,
    width: Int,
    height: Int,
    format: CompressFormat,
    quality: Int
  )

  fun reset()
}

class RealAffixEngine @Inject constructor(
  private val app: Application,
  private val mediaScanner: MediaScanner,
  @ImageSpacingVertical private val spacingVerticalPref: Pref<Int>,
  @ImageSpacingHorizontal private val spacingHorizontalPref: Pref<Int>,
  @ScalePriority private val scalePriorityPref: Pref<Boolean>,
  @StackHorizontally private val stackHorizontallyPref: Pref<Boolean>,
  @BgFillColor private val bgFillColorPref: Pref<Int>,
  private val ioManager: IoManager,
  private val bitmapManipulator: BitmapManipulator,
  private val mainContext: CoroutineContext = Dispatchers.Main,
  private val ioContext: CoroutineContext = Dispatchers.IO
) : AffixEngine {

  private lateinit var engineOwner: EngineOwner
  private lateinit var bitmapIterator: BitmapIterator

  private var canvasCreator: CanvasCreator = { Canvas(it) }
  private var dpConverter: DpConverter = {
    applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        it.toFloat(),
        app.resources.displayMetrics
    )
  }
  private var paintCreator: PaintCreator = {
    Paint().apply {
      isFilterBitmap = true
      isAntiAlias = true
      isDither = true
    }
  }
  private var rectCreator: RectCreator = { l, t, r, b ->
    Rect(l, t, r, b)
  }

  override fun process(
    photos: List<Photo>,
    engineOwner: EngineOwner
  ) {
    this.bitmapIterator = BitmapIterator(
        photos = photos,
        bitmapManipulator = bitmapManipulator
    )
    this.engineOwner = engineOwner

    val horizontalOrientation = stackHorizontallyPref.get()
    val widthAndHeight = if (horizontalOrientation) {
      calculateHorizontalWidthAndHeight()
    } else {
      calculateVerticalWidthAndHeight()
    }

    if (!widthAndHeight.isZero()) {
      this.engineOwner.showImageSizingDialog(
          widthAndHeight.width,
          widthAndHeight.height
      )
    }
  }

  override fun onSizeConfirmed(
    scale: Double,
    width: Int,
    height: Int,
    format: CompressFormat,
    quality: Int
  ) {
    GlobalScope.launch(mainContext) {
      performProcessing(scale, width, height, format, quality)
    }
  }

  override fun reset() = bitmapIterator.reset()

  @VisibleForTesting fun calculateHorizontalWidthAndHeight(): Size {
    val spacingHorizontal = spacingHorizontalPref.get()
        .dp()

    // The width of the resulting image will be the largest width of the selected images
    // The height of the resulting image will be the sum of all the selected images' heights
    var maxHeight = -1
    var minHeight = -1
    // Traverse all selected images to find largest and smallest heights
    bitmapIterator.reset()

    try {
      for (options in bitmapIterator) {
        val size = Size.fromOptions(options)

        if (maxHeight == -1) {
          maxHeight = size.height
        } else if (size.height > maxHeight) {
          maxHeight = size.height
        }

        if (minHeight == -1) {
          minHeight = size.height
        } else if (size.height < minHeight) {
          minHeight = size.height
        }
      }
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
      reset()
      return Size(0, 0)
    }

    // Traverse images again now that we know the min/max height, scale widths accordingly
    bitmapIterator.reset()
    var totalWidth = 0
    val scalePriority = scalePriorityPref.get()

    try {
      for (options in bitmapIterator) {
        val size = Size.fromOptions(options)

        var w = size.width
        var h = size.height
        val ratio = w.toFloat() / h.toFloat()

        if (scalePriority) {
          // Scale to largest
          if (h < maxHeight) {
            h = maxHeight
            w = (h.toFloat() * ratio).toRoundedInt()
          }
        } else if (h > minHeight) {
          // Scale to smallest
          h = minHeight
          w = (h.toFloat() * ratio).toRoundedInt()
        }

        totalWidth += w
      }
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
      reset()
      return Size(0, 0)
    }

    // Compensate for horizontal spacing
    totalWidth += spacingHorizontal * (bitmapIterator.size() - 1)

    return Size(totalWidth, if (scalePriority) maxHeight else minHeight)
  }

  @VisibleForTesting fun calculateVerticalWidthAndHeight(): Size {
    val spacingVertical = spacingVerticalPref.get()
        .dp()

    // The height of the resulting image will be the largest height of the selected images
    // The width of the resulting image will be the sum of all the selected images' widths
    var maxWidth = -1
    var minWidth = -1
    // Traverse all selected images and load min/max width, scale height accordingly
    bitmapIterator.reset()

    try {
      for (options in bitmapIterator) {
        val size = Size.fromOptions(options)

        if (maxWidth == -1) {
          maxWidth = size.width
        } else if (size.width > maxWidth) {
          maxWidth = size.width
        }

        if (minWidth == -1) {
          minWidth = size.width
        } else if (size.width < minWidth) {
          minWidth = size.width
        }
      }
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
      reset()
      return Size(0, 0)
    }

    // Traverse images again now that we know the min/max height, scale widths accordingly
    bitmapIterator.reset()
    var totalHeight = 0
    val scalePriority = scalePriorityPref.get()

    try {
      for (options in bitmapIterator) {
        val size = Size.fromOptions(options)

        var w = size.width
        var h = size.height
        val ratio = h.toFloat() / w.toFloat()

        if (scalePriority) {
          // Scale to largest
          if (w < maxWidth) {
            w = maxWidth
            h = (w.toFloat() * ratio).toRoundedInt()
          }
        } else if (w > minWidth) {
          // Scale to smallest
          w = minWidth
          h = (w.toFloat() * ratio).toRoundedInt()
        }

        totalHeight += h
      }
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
      reset()
      return Size(0, 0)
    }

    // Compensate for spacing
    totalHeight += spacingVertical * (bitmapIterator.size() - 1)

    return Size(if (scalePriority) maxWidth else minWidth, totalHeight)
  }

  private suspend fun performProcessing(
    selectedScale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: Bitmap.CompressFormat,
    quality: Int
  ) {
    val horizontalOrientation = stackHorizontallyPref.get()
    if (horizontalOrientation) {
      performHorizontalProcessing(
          selectedScale,
          resultWidth,
          resultHeight,
          format,
          quality
      )
    } else {
      performVerticalProcessing(
          selectedScale,
          resultWidth,
          resultHeight,
          format,
          quality
      )
    }
  }

  @VisibleForTesting suspend fun performHorizontalProcessing(
    selectedScale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: Bitmap.CompressFormat,
    quality: Int
  ) {
    val result = bitmapManipulator.createEmptyBitmap(resultWidth, resultHeight)
    val spacingHorizontal = (spacingHorizontalPref.get().dp() *
        selectedScale).toInt()

    val resultCanvas = canvasCreator(result)
    val paint = paintCreator()

    val bgFillColor = bgFillColorPref.get()
    if (bgFillColor != TRANSPARENT) {
      // Fill the canvas (blank image) with the user's selected background fill color
      resultCanvas.drawColor(bgFillColor)
    }

    engineOwner.showContentLoading(true)

    withContext(ioContext) {
      // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
      var processedCount = 0
      val scalingPriority = scalePriorityPref.get()

      var currentX = 0
      bitmapIterator.reset()

      try {
        for (options in bitmapIterator) {
          processedCount++

          val width = options.outWidth
          val height = options.outHeight
          val ratio = width.toFloat() / height.toFloat()

          var scaledWidth = (width * selectedScale).toRoundedInt()
          var scaledHeight = (height * selectedScale).toRoundedInt()

          if (scalingPriority) {
            // Scale up to largest height, fill total height
            if (scaledHeight < resultHeight) {
              scaledHeight = resultHeight
              scaledWidth = (scaledHeight.toFloat() * ratio).toRoundedInt()
            }
          } else {
            // Scale down to smallest height, fill total height
            if (scaledHeight > resultHeight) {
              scaledHeight = resultHeight
              scaledWidth = (scaledHeight.toFloat() * ratio).toRoundedInt()
            }
          }

          // Right is left plus width of the current image
          val right = currentX + scaledWidth
          val dstRect = rectCreator(
              currentX,
              0,
              right,
              scaledHeight
          )

          options.inJustDecodeBounds = false
          options.inSampleSize = (dstRect.bottom - dstRect.top) / options.outHeight

          val bm = bitmapIterator.currentBitmap()
          try {
            bm.density = DENSITY_NONE
            resultCanvas.drawBitmap(bm, null, dstRect, paint)
          } finally {
            bm.safeRecycle()
          }

          currentX = dstRect.right + spacingHorizontal
        }
      } catch (e: Exception) {
        withContext(mainContext) { engineOwner.showErrorDialog(e) }
        reset()
        return@withContext
      }

      finishProcessing(
          processedCount = processedCount,
          result = result,
          format = format,
          quality = quality
      )
    }
  }

  @VisibleForTesting suspend fun performVerticalProcessing(
    selectedScale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: Bitmap.CompressFormat,
    quality: Int
  ) {
    val result = bitmapManipulator.createEmptyBitmap(resultWidth, resultHeight)
    val spacingVertical = (spacingVerticalPref.get().dp() *
        selectedScale).toInt()

    val resultCanvas = canvasCreator(result)
    val paint = paintCreator()

    val bgFillColor = bgFillColorPref.get()
    if (bgFillColor != TRANSPARENT) {
      // Fill the canvas (blank image) with the user's selected background fill color
      resultCanvas.drawColor(bgFillColor)
    }

    engineOwner.showContentLoading(true)

    withContext(ioContext) {
      // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
      var processedCount = 0
      val scalingPriority = scalePriorityPref.get()

      var currentY = 0
      bitmapIterator.reset()

      try {
        for (options in bitmapIterator) {
          processedCount++

          val width = options.outWidth
          val height = options.outHeight
          val ratio = height.toFloat() / width.toFloat()

          var scaledWidth = (width * selectedScale).toRoundedInt()
          var scaledHeight = (height * selectedScale).toRoundedInt()

          if (scalingPriority) {
            // Scale up to largest width, fill total width
            if (scaledWidth < resultWidth) {
              scaledWidth = resultWidth
              scaledHeight = (scaledWidth.toFloat() * ratio).toRoundedInt()
            }
          } else {
            // Scale down to smallest width, fill total width
            if (scaledWidth > resultWidth) {
              scaledWidth = resultWidth
              scaledHeight = (scaledWidth.toFloat() * ratio).toRoundedInt()
            }
          }

          // Bottom is top plus height of the current image
          val bottom = currentY + scaledHeight
          val dstRect = rectCreator(0, currentY, scaledWidth, bottom)

          options.inJustDecodeBounds = false
          options.inSampleSize = (dstRect.right - dstRect.left) / options.outWidth

          val bm = bitmapIterator.currentBitmap()
          try {
            bm.density = DENSITY_NONE
            resultCanvas.drawBitmap(bm, null, dstRect, paint)
          } finally {
            bm.safeRecycle()
          }

          currentY = dstRect.bottom + spacingVertical
        }
      } catch (e: Exception) {
        withContext(mainContext) { engineOwner.showErrorDialog(e) }
        reset()
        return@withContext
      }

      finishProcessing(
          processedCount = processedCount,
          result = result,
          format = format,
          quality = quality
      )
    }
  }

  @VisibleForTesting suspend fun finishProcessing(
    processedCount: Int,
    result: Bitmap,
    format: CompressFormat,
    quality: Int
  ) {
    if (processedCount == 0) {
      result.safeRecycle()
      withContext(mainContext) { engineOwner.showContentLoading(false) }
      return
    }

    // Save results to file
    val cacheFile = ioManager.makeTempFile(format.extension())
    requireNotNull(cacheFile) { "Generated temp file cannot be null." }
    try {
      bitmapManipulator.encodeBitmap(
          bitmap = result,
          format = format,
          quality = quality,
          file = cacheFile
      )
    } catch (e: Exception) {
      cacheFile.delete()
      withContext(mainContext) { engineOwner.showErrorDialog(e) }
    }

    // Recycle the large final image
    result.safeRecycle()
    done(cacheFile)
  }

  @VisibleForTesting suspend fun done(file: File) = withContext(mainContext) {
    engineOwner.onDoneProcessing()

    // Add the affixed file to the media store so gallery apps can see it
    mediaScanner.scan(file.toString()) { _, uri ->
      engineOwner.showContentLoading(false)
      engineOwner.launchViewer(uri)
    }
  }

  private fun Int.dp() = dpConverter(this).toInt()

  // Unit test setters

  @TestOnly internal fun setEngineOwner(engineOwner: EngineOwner) {
    this.engineOwner = engineOwner
  }

  @TestOnly internal fun getEngineOwner() = this.engineOwner

  @TestOnly internal fun setBitmapIterator(bitmapIterator: BitmapIterator) {
    this.bitmapIterator = bitmapIterator
  }

  @TestOnly internal fun getBitmapIterator() = this.bitmapIterator

  @TestOnly internal fun setDpConverter(converter: DpConverter) {
    this.dpConverter = converter
  }

  @TestOnly internal fun setPaintCreator(creator: PaintCreator) {
    this.paintCreator = creator
  }

  @TestOnly internal fun setCanvasCreator(creator: CanvasCreator) {
    this.canvasCreator = creator
  }

  @TestOnly internal fun setRectCreator(creator: RectCreator) {
    this.rectCreator = creator
  }
}
