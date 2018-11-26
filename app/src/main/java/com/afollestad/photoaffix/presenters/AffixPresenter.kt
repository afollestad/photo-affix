/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.presenters

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.DENSITY_NONE
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaScannerConnection.scanFile
import androidx.annotation.Size
import com.afollestad.photoaffix.data.Photo
import com.afollestad.photoaffix.prefs.BgFillColor
import com.afollestad.photoaffix.prefs.ImageSpacingHorizontal
import com.afollestad.photoaffix.prefs.ImageSpacingVertical
import com.afollestad.photoaffix.prefs.ScalePriority
import com.afollestad.photoaffix.prefs.StackHorizontally
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.closeQuietely
import com.afollestad.photoaffix.utilities.safeRecycle
import com.afollestad.photoaffix.views.MainView
import com.afollestad.rxkprefs.Pref
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

interface AffixPresenter {

  fun attachView(mainView: MainView)

  fun process(photos: List<Photo>)

  fun sizeDetermined(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  )

  fun clearPhotos()

  fun reset()

  fun detachView()
}

class RealAffixPresenter @Inject constructor(
  private val application: Application,
  @ImageSpacingVertical private val spacingVerticalPref: Pref<Int>,
  @ImageSpacingHorizontal private val spacingHorizontalPref: Pref<Int>,
  @ScalePriority private val scalePriorityPref: Pref<Boolean>,
  @BgFillColor private val bgFillColorPref: Pref<Int>,
  @StackHorizontally private val stackHorizontallyPref: Pref<Boolean>,
  private val ioManager: IoManager
) : AffixPresenter {

  private var mainView: MainView? = null
  private var traverseIndex: Int = 0
  private var selectedPhotos: List<Photo>? = null

  override fun attachView(mainView: MainView) {
    this.mainView = mainView
  }

  override fun process(photos: List<Photo>) {
    // Lock orientation so the Activity won't change configuration during processing
    mainView?.lockOrientation() ?: return
    this.selectedPhotos = photos

    val spacingHorizontal = spacingHorizontalPref.get()
    val spacingVertical = spacingVerticalPref.get()
    val horizontal = stackHorizontallyPref.get()

    val resultWidth: Int
    val resultHeight: Int

    if (horizontal) {
      // The width of the resulting image will be the largest width of the selected images
      // The height of the resulting image will be the sum of all the selected images' heights
      var maxHeight = -1
      var minHeight = -1
      // Traverse all selected images to find largest and smallest heights
      reset()

      while (true) {
        val size = nextBitmapSize()
        if (size == null ||
            (size[0] == 0 && size[1] == 0)
        ) {
          break
        }
        if (maxHeight == -1) {
          maxHeight = size[1]
        } else if (size[1] > maxHeight) {
          maxHeight = size[1]
        }
        if (minHeight == -1) {
          minHeight = size[1]
        } else if (size[1] < minHeight) {
          minHeight = size[1]
        }
      }

      // Traverse images again now that we know the min/max height, scale widths accordingly
      reset()
      var totalWidth = 0
      val scalePriority = scalePriorityPref.get()

      while (true) {
        val size = nextBitmapSize()
        if (size == null ||
            (size[0] == 0 && size[1] == 0)
        ) {
          break
        }
        var w = size[0]
        var h = size[1]
        val ratio = w.toFloat() / h.toFloat()
        if (scalePriority) {
          // Scale to largest
          if (h < maxHeight) {
            h = maxHeight
            w = (h.toFloat() * ratio).toInt()
          }
        } else {
          // Scale to smallest
          if (h > minHeight) {
            h = minHeight
            w = (h.toFloat() * ratio).toInt()
          }
        }
        totalWidth += w
      }

      // Compensate for spacing
      totalWidth += spacingHorizontal * (selectedPhotos!!.size + 1)
      minHeight += spacingVertical * 2
      maxHeight += spacingVertical * 2

      // Print data and create large Bitmap
      resultWidth = totalWidth
      resultHeight = if (scalePriority) maxHeight else minHeight
    } else {
      // The height of the resulting image will be the largest height of the selected images
      // The width of the resulting image will be the sum of all the selected images' widths
      var maxWidth = -1
      var minWidth = -1
      // Traverse all selected images and load min/max width, scale height accordingly
      reset()

      while (true) {
        val size = nextBitmapSize()
        if (size == null ||
            (size[0] == 0 && size[1] == 0)
        ) {
          break
        }
        if (maxWidth == -1) {
          maxWidth = size[0]
        } else if (size[0] > maxWidth) {
          maxWidth = size[0]
        }
        if (minWidth == -1) {
          minWidth = size[0]
        } else if (size[0] < minWidth) {
          minWidth = size[0]
        }
      }

      // Traverse images again now that we know the min/max height, scale widths accordingly
      reset()
      var totalHeight = 0
      val scalePriority = scalePriorityPref.get()

      while (true) {
        val size = nextBitmapSize()
        if (size == null || size[0] == 0 && size[1] == 0) {
          break
        }
        var w = size[0]
        var h = size[1]
        val ratio = h.toFloat() / w.toFloat()
        if (scalePriority) {
          // Scale to largest
          if (w < maxWidth) {
            w = maxWidth
            h = (w.toFloat() * ratio).toInt()
          }
        } else {
          // Scale to smallest
          if (w > minWidth) {
            w = minWidth
            h = (w.toFloat() * ratio).toInt()
          }
        }
        totalHeight += h
      }

      // Compensate for spacing
      totalHeight += spacingVertical * (selectedPhotos!!.size + 1)
      minWidth += spacingHorizontal * 2
      maxWidth += spacingHorizontal * 2

      // Print data and create large Bitmap
      resultWidth = if (scalePriority) maxWidth else minWidth
      resultHeight = totalHeight
    }

    mainView?.showImageSizingDialog(resultWidth, resultHeight)
  }

  override fun sizeDetermined(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  ) {
    if (cancelled) {
      reset()
      mainView?.unlockOrientation()
      return
    }
    try {
      finishProcessing(scale, resultWidth, resultHeight, format, quality)
    } catch (_: OutOfMemoryError) {
      mainView?.showMemoryError()
    }
  }

  override fun clearPhotos() {
    this.selectedPhotos = null
  }

  override fun reset() {
    this.traverseIndex = -1
  }

  override fun detachView() {
    this.mainView = null
  }

  @Size(2)
  private fun nextBitmapSize(): IntArray? {
    requireNotNull(selectedPhotos) { "selectedPhotos must be set" }
    traverseIndex++
    if (traverseIndex > selectedPhotos!!.size - 1) {
      return null
    }

    val nextPhoto = selectedPhotos!![traverseIndex]
    val options = BitmapFactory.Options()
        .apply {
          inJustDecodeBounds = true
        }
    var inputStream: InputStream? = null

    try {
      inputStream = ioManager.openStream(nextPhoto.uri)
      BitmapFactory.decodeStream(inputStream, null, options)
    } catch (e: Exception) {
      mainView?.showErrorDialog(e)
      return intArrayOf(0, 0)
    } finally {
      inputStream.closeQuietely()
    }

    return intArrayOf(options.outWidth, options.outHeight)
  }

  private fun nextBitmapOptions(): BitmapFactory.Options? {
    if (selectedPhotos == null) {
      return null
    }
    traverseIndex++
    if (traverseIndex > selectedPhotos!!.size - 1) {
      return null
    }

    val nextPhoto = selectedPhotos!![traverseIndex]
    var inputStream: InputStream? = null
    val options: BitmapFactory.Options?

    try {
      inputStream = ioManager.openStream(nextPhoto.uri)
      options = BitmapFactory.Options()
          .apply {
            inJustDecodeBounds = true
          }
      BitmapFactory.decodeStream(inputStream, null, options)
    } catch (e: Exception) {
      mainView?.showErrorDialog(e)
      return null
    } catch (e2: OutOfMemoryError) {
      mainView?.showMemoryError()
      return null
    } finally {
      inputStream.closeQuietely()
    }

    return options
  }

  private fun nextBitmap(options: BitmapFactory.Options): Bitmap? {
    val nextPhoto = selectedPhotos?.get(traverseIndex) ?: return null
    var inputStream: InputStream? = null

    return try {
      inputStream = ioManager.openStream(nextPhoto.uri)
      BitmapFactory.decodeStream(inputStream, null, options)
    } catch (e: Exception) {
      mainView?.showErrorDialog(e)
      null
    } catch (e2: OutOfMemoryError) {
      mainView?.showMemoryError()
      null
    } finally {
      inputStream.closeQuietely()
    }
  }

  private fun finishProcessing(
    SCALE: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: Bitmap.CompressFormat,
    quality: Int
  ) {
    val result = createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
    val horizontal = stackHorizontallyPref.get()

    val spacingHorizontal = (spacingHorizontalPref.get() * SCALE).toInt()
    val spacingVertical = (spacingVerticalPref.get() * SCALE).toInt()

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

    mainView?.showContentLoading(true)
    GlobalScope.launch(IO) {
      // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
      val dstRect = Rect(0, 0, 10, 10)
      var processedCount = 0
      val scalingPriority = scalePriorityPref.get()

      if (horizontal) {
        var currentX = 0
        traverseIndex = -1

        while (true) {
          processedCount++
          val bitmapOptions = nextBitmapOptions() ?: break

          val width = bitmapOptions.outWidth
          val height = bitmapOptions.outHeight
          val ratio = width.toFloat() / height.toFloat()

          var scaledWidth = (width * SCALE).toInt()
          var scaledHeight = (height * SCALE).toInt()

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
          dstRect.bottom = scaledHeight + spacingVertical

          bitmapOptions.inJustDecodeBounds = false
          bitmapOptions.inSampleSize = (dstRect.bottom - dstRect.top) / bitmapOptions.outHeight

          val bm = nextBitmap(bitmapOptions) ?: break
          try {
            bm.density = DENSITY_NONE
            resultCanvas.drawBitmap(bm, null, dstRect, paint)
          } catch (e: RuntimeException) {
            mainView?.showMemoryError()
          } finally {
            bm.safeRecycle()
          }

          currentX = dstRect.right
        }
      } else {
        var currentY = 0
        traverseIndex = -1

        while (true) {
          val bitmapOptions = nextBitmapOptions() ?: break
          processedCount++

          val width = bitmapOptions.outWidth
          val height = bitmapOptions.outHeight
          val ratio = height.toFloat() / width.toFloat()

          var scaledWidth = (width * SCALE).toInt()
          var scaledHeight = (height * SCALE).toInt()

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
          dstRect.right = scaledWidth + spacingHorizontal

          bitmapOptions.inJustDecodeBounds = false
          bitmapOptions.inSampleSize = (dstRect.right - dstRect.left) / bitmapOptions.outWidth

          val bm = nextBitmap(bitmapOptions) ?: break
          try {
            bm.density = Bitmap.DENSITY_NONE
            resultCanvas.drawBitmap(bm, null, dstRect, paint)
          } catch (e: RuntimeException) {
            mainView?.showMemoryError()
          } finally {
            bm.safeRecycle()
          }

          currentY = dstRect.bottom
        }
      }

      if (processedCount == 0) {
        result.safeRecycle()
        withContext(Main) { mainView?.showContentLoading(false) }
        return@launch
      }

      // Save results to file
      val extension = if (format == CompressFormat.PNG) ".png" else ".jpg"
      val cacheFile = ioManager.makeTempFile(extension)
      var os: FileOutputStream? = null

      try {
        os = FileOutputStream(cacheFile)
        result.compress(format, quality, os)
      } catch (e: Exception) {
        mainView?.showErrorDialog(e)
      } finally {
        os.closeQuietely()
      }

      // Recycle the large final image
      result.safeRecycle()
      withContext(Main) { done(cacheFile) }
    }
  }

  private fun done(file: File) {
    mainView?.run {
      clearSelection()
      unlockOrientation()
    }

    // Add the affixed file to the media store so gallery apps can see it
    scanFile(
        application,
        arrayOf(file.toString()),
        null
    ) { _, uri ->
      mainView?.showContentLoading(false)
      mainView?.launchViewer(uri)
    }
  }
}
