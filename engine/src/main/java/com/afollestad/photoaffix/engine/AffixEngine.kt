/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.DENSITY_NONE
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaScannerConnection.scanFile
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.afollestad.photoaffix.prefs.BgFillColor
import com.afollestad.photoaffix.prefs.ImageSpacingHorizontal
import com.afollestad.photoaffix.prefs.ImageSpacingVertical
import com.afollestad.photoaffix.prefs.ScalePriority
import com.afollestad.photoaffix.prefs.StackHorizontally
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.closeQuietely
import com.afollestad.photoaffix.utilities.dp
import com.afollestad.photoaffix.utilities.extension
import com.afollestad.photoaffix.utilities.safeRecycle
import com.afollestad.rxkprefs.Pref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class Size(
  val width: Int,
  val height: Int
) {

  fun isZero() = width == 0 || height == 0

  companion object {
    fun fromOptions(options: Options): Size {
      return Size(options.outWidth, options.outHeight)
    }
  }
}

/** @author Aidan Follestad (afollestad) */
interface EngineOwner {

  fun showImageSizingDialog(
    width: Int,
    height: Int
  )

  fun showContentLoading(loading: Boolean)

  fun showErrorDialog(e: Exception)

  fun onDoneProcessing()

  fun launchViewer(uri: Uri)
}

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
  @ImageSpacingVertical private val spacingVerticalPref: Pref<Int>,
  @ImageSpacingHorizontal private val spacingHorizontalPref: Pref<Int>,
  @ScalePriority private val scalePriorityPref: Pref<Boolean>,
  @StackHorizontally private val stackHorizontallyPref: Pref<Boolean>,
  @BgFillColor private val bgFillColorPref: Pref<Int>,
  private val ioManager: IoManager,
  private val bitmapDecoder: BitmapDecoder
) : AffixEngine {

  private lateinit var engineOwner: EngineOwner
  private lateinit var bitmapIterator: BitmapIterator

  override fun process(
    photos: List<Photo>,
    engineOwner: EngineOwner
  ) {
    this.bitmapIterator = BitmapIterator(
        photos = photos,
        bitmapDecoder = bitmapDecoder
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
  ) = performProcessing(scale, width, height, format, quality)

  override fun reset() = bitmapIterator.reset()

  @VisibleForTesting fun calculateHorizontalWidthAndHeight(): Size {
    val spacingHorizontal = spacingHorizontalPref.get()
        .dp(app)
        .toInt()
    val spacingVertical = spacingVerticalPref.get()
        .dp(app)
        .toInt()

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
            w = (h.toFloat() * ratio).toInt()
          }
        } else if (h > minHeight) {
          // Scale to smallest
          h = minHeight
          w = (h.toFloat() * ratio).toInt()
        }

        totalWidth += w
      }
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
      reset()
      return Size(0, 0)
    }

    // Compensate for spacing
    totalWidth += spacingHorizontal * (bitmapIterator.size() + 1)
    minHeight += spacingVertical * 2
    maxHeight += spacingVertical * 2

    return Size(totalWidth, if (scalePriority) maxHeight else minHeight)
  }

  @VisibleForTesting fun calculateVerticalWidthAndHeight(): Size {
    val spacingHorizontal = spacingHorizontalPref.get()
        .dp(app)
        .toInt()
    val spacingVertical = spacingVerticalPref.get()
        .dp(app)
        .toInt()

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
            h = (w.toFloat() * ratio).toInt()
          }
        } else if (w > minWidth) {
          // Scale to smallest
          w = minWidth
          h = (w.toFloat() * ratio).toInt()
        }

        totalHeight += h
      }
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
      reset()
      return Size(0, 0)
    }

    // Compensate for spacing
    totalHeight += spacingVertical * (bitmapIterator.size() + 1)
    minWidth += spacingHorizontal * 2
    maxWidth += spacingHorizontal * 2

    return Size(if (scalePriority) maxWidth else minWidth, totalHeight)
  }

  @VisibleForTesting fun performProcessing(
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

  @VisibleForTesting fun performHorizontalProcessing(
    selectedScale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: Bitmap.CompressFormat,
    quality: Int
  ) {
    val result = Bitmap.createBitmap(resultWidth, resultHeight, ARGB_8888)
    val spacingHorizontal = (spacingHorizontalPref.get().dp(app)
        .toInt() * selectedScale).toInt()
    val spacingVertical = (spacingVerticalPref.get().dp(app)
        .toInt() * selectedScale).toInt()

    val resultCanvas = Canvas(result)
    val paint = Paint().apply {
      isFilterBitmap = true
      isAntiAlias = true
      isDither = true
    }

    val bgFillColor = bgFillColorPref.get()
    if (bgFillColor != TRANSPARENT) {
      // Fill the canvas (blank image) with the user's selected background fill color
      resultCanvas.drawColor(bgFillColor)
    }

    engineOwner.showContentLoading(true)

    GlobalScope.launch(IO) {
      // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
      val dstRect = Rect(0, 0, 10, 10)
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

          var scaledWidth = (width * selectedScale).toInt()
          var scaledHeight = (height * selectedScale).toInt()

          if (scalingPriority) {
            // Scale up to largest height, fill total height
            if (scaledHeight < resultHeight) {
              scaledHeight = resultHeight
              scaledWidth = (scaledHeight.toFloat() * ratio).toInt()
            }
          } else {
            // Scale down to smallest height, fill total height
            if (scaledHeight > resultHeight) {
              scaledHeight = resultHeight
              scaledWidth = (scaledHeight.toFloat() * ratio).toInt()
            }
          }

          // Left is right of last image plus horizontal spacing
          dstRect.left = currentX + spacingHorizontal
          // Right is left plus width of the current image
          dstRect.right = dstRect.left + scaledWidth
          dstRect.top = spacingVertical
          dstRect.bottom = dstRect.top + scaledHeight

          options.inJustDecodeBounds = false
          options.inSampleSize = (dstRect.bottom - dstRect.top) / options.outHeight

          val bm = bitmapIterator.currentBitmap() ?: continue
          try {
            bm.density = DENSITY_NONE
            resultCanvas.drawBitmap(bm, null, dstRect, paint)
          } finally {
            bm.safeRecycle()
          }

          currentX = dstRect.right
        }
      } catch (e: Exception) {
        engineOwner.showErrorDialog(e)
        reset()
        return@launch
      }

      finishProcessing(processedCount, result, format, quality)
    }
  }

  @VisibleForTesting fun performVerticalProcessing(
    selectedScale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: Bitmap.CompressFormat,
    quality: Int
  ) {
    val result = Bitmap.createBitmap(resultWidth, resultHeight, ARGB_8888)
    val spacingHorizontal = (spacingHorizontalPref.get().dp(app)
        .toInt() * selectedScale).toInt()
    val spacingVertical = (spacingVerticalPref.get().dp(app)
        .toInt() * selectedScale).toInt()

    val resultCanvas = Canvas(result)
    val paint = Paint().apply {
      isFilterBitmap = true
      isAntiAlias = true
      isDither = true
    }

    val bgFillColor = bgFillColorPref.get()
    if (bgFillColor != TRANSPARENT) {
      // Fill the canvas (blank image) with the user's selected background fill color
      resultCanvas.drawColor(bgFillColor)
    }

    engineOwner.showContentLoading(true)

    GlobalScope.launch(IO) {
      // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
      val dstRect = Rect(0, 0, 10, 10)
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

          var scaledWidth = (width * selectedScale).toInt()
          var scaledHeight = (height * selectedScale).toInt()

          if (scalingPriority) {
            // Scale up to largest width, fill total width
            if (scaledWidth < resultWidth) {
              scaledWidth = resultWidth
              scaledHeight = (scaledWidth.toFloat() * ratio).toInt()
            }
          } else {
            // Scale down to smallest width, fill total width
            if (scaledWidth > resultWidth) {
              scaledWidth = resultWidth
              scaledHeight = (scaledWidth.toFloat() * ratio).toInt()
            }
          }

          // Top is bottom of the last image plus vertical spacing
          dstRect.top = currentY + spacingVertical
          // Bottom is top plus height of the current image
          dstRect.bottom = dstRect.top + scaledHeight
          dstRect.left = spacingHorizontal
          dstRect.right = dstRect.left + scaledWidth

          options.inJustDecodeBounds = false
          options.inSampleSize = (dstRect.right - dstRect.left) / options.outWidth

          val bm = bitmapIterator.currentBitmap() ?: continue
          try {
            bm.density = DENSITY_NONE
            resultCanvas.drawBitmap(bm, null, dstRect, paint)
          } finally {
            bm.safeRecycle()
          }

          currentY = dstRect.bottom
        }
      } catch (e: Exception) {
        engineOwner.showErrorDialog(e)
        reset()
        return@launch
      }

      finishProcessing(processedCount, result, format, quality)
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
      withContext(Dispatchers.Main) { engineOwner.showContentLoading(false) }
      return
    }

    // Save results to file
    val cacheFile = ioManager.makeTempFile(format.extension())
    var os: FileOutputStream? = null

    try {
      os = FileOutputStream(cacheFile)
      result.compress(format, quality, os)
    } catch (e: Exception) {
      engineOwner.showErrorDialog(e)
    } finally {
      os.closeQuietely()
    }

    // Recycle the large final image
    result.safeRecycle()
    done(cacheFile)
  }

  @VisibleForTesting suspend fun done(file: File) = withContext(Main) {
    engineOwner.onDoneProcessing()

    // Add the affixed file to the media store so gallery apps can see it
    scanFile(app, arrayOf(file.toString()), null) { _, uri ->
      engineOwner.showContentLoading(false)
      engineOwner.launchViewer(uri)
    }
  }

  @TestOnly fun setEngineOwner(engineOwner: EngineOwner) {
    this.engineOwner = engineOwner
  }

  @TestOnly fun getEngineOwner() = this.engineOwner

  @TestOnly fun setBitmapIterator(bitmapIterator: BitmapIterator) {
    this.bitmapIterator = bitmapIterator
  }

  @TestOnly fun getBitmapIterator() = this.bitmapIterator
}
