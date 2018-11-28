/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine.subengines

import com.afollestad.photoaffix.engine.EngineOwner
import com.afollestad.photoaffix.engine.bitmaps.BitmapIterator
import com.afollestad.photoaffix.prefs.ImageSpacingHorizontal
import com.afollestad.photoaffix.prefs.ImageSpacingVertical
import com.afollestad.photoaffix.prefs.ScalePriority
import com.afollestad.photoaffix.prefs.StackHorizontally
import com.afollestad.photoaffix.utilities.DpConverter
import com.afollestad.photoaffix.utilities.ext.toRoundedInt
import com.afollestad.photoaffix.utilities.qualifiers.MainDispatcher
import com.afollestad.rxkprefs.Pref
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/** @author Aidan Follestad (afollestad) */
interface DimensionsEngine {

  fun setup(
    bitmapIterator: BitmapIterator,
    engineOwner: EngineOwner
  )

  suspend fun calculateSize(): Size
}

class RealDimensionsEngine @Inject constructor(
  private val dpConverter: DpConverter,
  @StackHorizontally private val stackHorizontallyPref: Pref<Boolean>,
  @ScalePriority private val scalePriorityPref: Pref<Boolean>,
  @ImageSpacingVertical private val spacingVerticalPref: Pref<Int>,
  @ImageSpacingHorizontal private val spacingHorizontalPref: Pref<Int>,
  @MainDispatcher private val mainContext: CoroutineContext
) : DimensionsEngine {

  private lateinit var bitmapIterator: BitmapIterator
  private lateinit var engineOwner: EngineOwner

  override fun setup(
    bitmapIterator: BitmapIterator,
    engineOwner: EngineOwner
  ) {
    this.bitmapIterator = bitmapIterator
    this.engineOwner = engineOwner
  }

  override suspend fun calculateSize(): Size {
    return if (stackHorizontallyPref.get()) {
      calculateHorizontalSize()
    } else {
      calculateVerticalSize()
    }
  }

  private suspend fun calculateHorizontalSize(): Size {
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
      bitmapIterator.reset()
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
      withContext(mainContext) { engineOwner.showErrorDialog(e) }
      bitmapIterator.reset()
      return Size(0, 0)
    }

    // Compensate for horizontal spacing
    totalWidth += spacingHorizontal * (bitmapIterator.size() - 1)

    return Size(
        totalWidth, if (scalePriority) maxHeight else minHeight
    )
  }

  private suspend fun calculateVerticalSize(): Size {
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
      withContext(mainContext) { engineOwner.showErrorDialog(e) }
      bitmapIterator.reset()
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
      withContext(mainContext) { engineOwner.showErrorDialog(e) }
      bitmapIterator.reset()
      return Size(0, 0)
    }

    // Compensate for spacing
    totalHeight += spacingVertical * (bitmapIterator.size() - 1)

    return Size(
        if (scalePriority) maxWidth else minWidth, totalHeight
    )
  }

  private fun Int.dp() = dpConverter.toDp(this).toInt()
}
