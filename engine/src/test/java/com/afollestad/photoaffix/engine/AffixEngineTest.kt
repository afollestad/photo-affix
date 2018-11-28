/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ScanResult
import com.afollestad.rxkprefs.Pref
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File

class AffixEngineTest {

  companion object {
    const val COLOR_BLUE: Int = -0xffff01
  }

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser)
  )
  private var bitmaps = listOf<Bitmap>()

  private val app = mock<Application>()
  private val mediaScanner = mock<MediaScanner>()

  private val spacingVerticalPref = mock<Pref<Int>>()
  private val spacingHorizontalPref = mock<Pref<Int>>()
  private val scalePriorityPref = mock<Pref<Boolean>>()
  private val stackHorizontallyPref = mock<Pref<Boolean>>()
  private val bgFillColorPref = mock<Pref<Int>>()

  private val ioManager = mock<IoManager>()
  private val bitmapManipulator = mock<BitmapManipulator> {
    on { createOptions(any()) } doAnswer { inv ->
      Options().apply { inJustDecodeBounds = inv.getArgument(0) }
    }
  }
  private val engineOwner = mock<EngineOwner> {
    on { showErrorDialog(any()) } doAnswer { inv ->
      val exception = inv.getArgument<Exception>(0)
      throw exception
    }
  }

  private var currentPaint: Paint? = null
  private var currentCanvas: Canvas? = null
  private val engine = RealAffixEngine(
      app,
      mediaScanner,
      spacingVerticalPref,
      spacingHorizontalPref,
      scalePriorityPref,
      stackHorizontallyPref,
      bgFillColorPref,
      ioManager,
      bitmapManipulator,
      Dispatchers.Default,
      Dispatchers.Default
  ).apply {
    setDpConverter { it.toFloat() }
    setPaintCreator {
      currentPaint = mock()
      return@setPaintCreator currentPaint!!
    }
    setCanvasCreator {
      currentCanvas = mock()
      return@setCanvasCreator currentCanvas!!
    }
    setRectCreator { l, t, r, b ->
      val rect = mock<Rect>()
      rect.left = l
      rect.top = t
      rect.right = r
      rect.bottom = b
      return@setRectCreator rect
    }
  }

  @Before fun setup() {
    val fakeUri = mock<Uri>()
    doAnswer { inv ->
      val path = inv.getArgument<String>(0)
      val callback = inv.getArgument<ScanResult?>(1)
      callback?.invoke(path, fakeUri)
      null
    }.whenever(mediaScanner)
        .scan(any(), any())

    whenever(bitmapManipulator.decodePhoto(any(), any()))
        .doAnswer { inv ->
          val photo = inv.getArgument<Photo>(0)
          val options = inv.getArgument<Options>(1)
          if (options.inJustDecodeBounds) {
            val bm = bitmapManipulator.decodeUri(photo.uri, options) ?: throw IllegalStateException(
                "decodeUri(${photo.uri}, ...) returned null"
            )
            options.outWidth = bm.width
            options.outHeight = bm.height
            null
          } else {
            bitmapManipulator.decodeUri(photo.uri, options)
          }
        }
  }

  // Process

  @Test fun process_horizontal() {
    setupBitmapIterator(
        Size(2, 2),
        Size(2, 2)
    )
    whenever(stackHorizontallyPref.get()).doReturn(true)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(spacingVerticalPref.get()).doReturn(0)
    whenever(spacingHorizontalPref.get()).doReturn(0)

    engine.process(photos, engineOwner)

    assertThat(engine.getBitmapIterator().size()).isEqualTo(photos.size)
    assertThat(engine.getEngineOwner()).isEqualTo(engineOwner)

    verify(engineOwner).showImageSizingDialog(4, 2)
  }

  @Test fun process_vertical() {
    setupBitmapIterator(
        Size(2, 2),
        Size(2, 2)
    )
    whenever(stackHorizontallyPref.get()).doReturn(false)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(spacingVerticalPref.get()).doReturn(0)
    whenever(spacingHorizontalPref.get()).doReturn(0)

    engine.process(photos, engineOwner)

    assertThat(engine.getBitmapIterator().size()).isEqualTo(photos.size)
    assertThat(engine.getEngineOwner()).isEqualTo(engineOwner)

    verify(engineOwner).showImageSizingDialog(2, 4)
  }

  // Reset

  @Test fun reset() {
    whenever(bitmapManipulator.decodePhoto(any(), any()))
        .doAnswer { inv ->
          val options = inv.getArgument<Options>(1)
          // This allows us to pass the != 0 checks in Iterator.next()
          options.outWidth = 10
          options.outHeight = 10
          null
        }
    val iterator = BitmapIterator(photos, bitmapManipulator)

    iterator.next()
    assertThat(iterator.traverseIndex()).isGreaterThan(-1)

    engine.setBitmapIterator(iterator)
    engine.reset()
    assertThat(iterator.traverseIndex()).isEqualTo(-1)
  }

  // Horizontal width and height calculation

  @Test fun calculateHorizontalWidthAndHeight_smallestFirst_noSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 4),
        Size(2, 8)
    )

    // No spacing and scale to largest
    val horizontalSpacing = 0
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 4 // scale to largest height ratio
    val expectedFirstHeight = 8 // scale to largest height ratio
    val expectedLastWidth = 2 // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_largestFirst_noSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 8),
        Size(2, 4)
    )

    // No spacing and scale to largest
    val horizontalSpacing = 0
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 4 // scale to largest height ratio
    val expectedFirstHeight = 8 // scale to largest height ratio
    val expectedLastWidth = 2 // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_largestFirst_noSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )

    // No spacing and scale to smallest
    val horizontalSpacing = 0
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1 // Scale to smallest height ratio
    val expectedFirstHeight = 3 // Scale to smallest height ratio
    val expectedLastWidth = 1 // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_smallestFirst_noSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )

    // No spacing and scale to smallest
    val horizontalSpacing = 0
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1 // Scale to smallest height ratio
    val expectedFirstHeight = 3 // Scale to smallest height ratio
    val expectedLastWidth = 1 // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_largestFirst_withSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(4, 8),
        Size(2, 4)
    )

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 4
    val expectedFirstHeight = 8
    val expectedLastWidth = 4

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth + horizontalSpacing,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_smallestFirst_withSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 4
    val expectedFirstHeight = 8
    val expectedLastWidth = 4

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth + horizontalSpacing,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_largestFirst_withSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(4, 8),
        Size(2, 4)
    )

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 2
    val expectedFirstHeight = 4
    val expectedLastWidth = 2

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth + horizontalSpacing,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_smallestFirst_withSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 2
    val expectedFirstHeight = 4
    val expectedLastWidth = 2

    assertThat(size).isEqualTo(
        Size(
            // total width
            width = expectedFirstWidth + expectedLastWidth + horizontalSpacing,
            // max height
            height = expectedFirstHeight
        )
    )
  }

  @Test fun calculateHorizontalWidthAndHeight_error_showDialogAndReset() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )

    whenever(spacingHorizontalPref.get()).doReturn(0)
    whenever(engineOwner.showErrorDialog(any())).doAnswer {
      // no-op
    }

    val error = Exception("Oh no!")
    whenever(bitmapManipulator.createOptions(any())).doAnswer { throw error }

    val size = engine.calculateHorizontalWidthAndHeight()
    verify(engineOwner).showErrorDialog(error)
    assertThat(size.isZero()).isTrue()
  }

  // Vertical width and height calculation

  @Test fun calculateVerticalWidthAndHeight_smallestFirst_noSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )

    // No spacing and scale to largest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 4  // scale to largest width ratio
    val expectedFirstHeight = 8 // scale to largest width ratio
    val expectedLastHeight = 8  // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height
            height = expectedFirstHeight + expectedLastHeight
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_largestFirst_noSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(4, 8),
        Size(2, 4)
    )

    // No spacing and scale to largest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 4  // scale to largest width ratio
    val expectedFirstHeight = 8 // Remains the same
    val expectedLastHeight = 8  // scale to largest width ratio

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height
            height = expectedFirstHeight + expectedLastHeight
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_largestFirst_noSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )

    // No spacing and scale to smallest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1  // scale to smallest width ratio
    val expectedFirstHeight = 4 // scale to smallest width ratio
    val expectedLastHeight = 3  // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height
            height = expectedFirstHeight + expectedLastHeight
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_smallestFirst_noSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )

    // No spacing and scale to smallest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1 // scale to smallest width ratio
    val expectedFirstHeight = 3 // Remains the same
    val expectedLastHeight = 4 // scale to smallest width ratio

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height
            height = expectedFirstHeight + expectedLastHeight
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_largestFirst_withSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )

    // No spacing and scale to smallest
    val verticalSpacing = 4
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1  // scale to smallest width ratio
    val expectedFirstHeight = 4 // scale to smallest width ratio
    val expectedLastHeight = 3  // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height + vertical spacing
            height = expectedFirstHeight + expectedLastHeight + verticalSpacing
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_smallestFirst_withSpacing_scaleToLargest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )

    // No spacing and scale to smallest
    val verticalSpacing = 4
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1  // scale to smallest width ratio
    val expectedFirstHeight = 4 // scale to smallest width ratio
    val expectedLastHeight = 3  // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height + vertical spacing
            height = expectedFirstHeight + expectedLastHeight + verticalSpacing
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_largestFirst_withSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )

    // No spacing and scale to smallest
    val verticalSpacing = 6
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1  // scale to smallest width ratio
    val expectedFirstHeight = 4 // scale to smallest width ratio
    val expectedLastHeight = 3  // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height + vertical spacing
            height = expectedFirstHeight + expectedLastHeight + verticalSpacing
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_smallestFirst_withSpacing_scaleToSmallest() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )

    // No spacing and scale to smallest
    val verticalSpacing = 6
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner, never()).showErrorDialog(any())

    // Verify results
    val expectedFirstWidth = 1  // scale to smallest width ratio
    val expectedFirstHeight = 4 // scale to smallest width ratio
    val expectedLastHeight = 3  // Remains the same

    assertThat(size).isEqualTo(
        Size(
            // max width
            width = expectedFirstWidth,
            // total height + vertical spacing
            height = expectedFirstHeight + expectedLastHeight + verticalSpacing
        )
    )
  }

  @Test fun calculateVerticalWidthAndHeight_error_showDialogAndReset() {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )

    whenever(spacingVerticalPref.get()).doReturn(0)
    whenever(engineOwner.showErrorDialog(any())).doAnswer {
      // no-op
    }

    val error = Exception("Oh no!")
    whenever(bitmapManipulator.createOptions(any())).doAnswer { throw error }

    val size = engine.calculateVerticalWidthAndHeight()
    verify(engineOwner).showErrorDialog(error)
    assertThat(size.isZero()).isTrue()
  }

  // Horizontal processing

  @Test fun performHorizontalProcessing_fullScale_scaleToLargest() = runBlocking {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(5, 5),
        Size(5, 5)
    )

    // Setup Prefs
    val spacingHorizontal = 5
    whenever(spacingHorizontalPref.get()).doReturn(spacingHorizontal)
    whenever(bgFillColorPref.get()).doReturn(COLOR_BLUE)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Setup Bitmap/Canvas/Paint stuff
    val totalWidth = 15 // bitmaps are both 5x5, + 5 horizontal spacing
    val totalHeight = 5 // horizontal, so height is the same for all
    val totalBitmap = fakeBitmap(totalWidth, totalHeight)
    whenever(bitmapManipulator.createEmptyBitmap(totalWidth, totalHeight))
        .doReturn(totalBitmap)

    // Setup IO Manager
    val cacheFilePath = "file://idk/result"
    val cacheFile = mock<File> {
      on { toString() } doReturn cacheFilePath
    }
    whenever(ioManager.makeTempFile(".png")).doReturn(cacheFile)

    // Perform Actions
    engine.performHorizontalProcessing(
        selectedScale = 1.0,
        resultWidth = totalWidth,
        resultHeight = totalHeight,
        format = PNG,
        quality = 100
    )

    // Verify Results
    verify(currentCanvas!!).drawColor(COLOR_BLUE)
    verify(engineOwner).showContentLoading(true)

    // Draw first Bitmap
    val rectCaptor1 = argumentCaptor<Rect>()
    verify(currentCanvas!!, times(1)).drawBitmap(
        eq(bitmaps[0]),
        eq(null),
        rectCaptor1.capture(),
        eq(currentPaint!!)
    )
    assertThat(rectCaptor1.lastValue.top).isEqualTo(0)
    assertThat(rectCaptor1.lastValue.left).isEqualTo(0)
    assertThat(rectCaptor1.lastValue.right).isEqualTo(5)
    assertThat(rectCaptor1.lastValue.bottom).isEqualTo(5)
    verify(bitmaps[0]).recycle()

    // Draw last Bitmap
    val rectCaptor2 = argumentCaptor<Rect>()
    verify(currentCanvas!!, times(1)).drawBitmap(
        eq(bitmaps[1]),
        eq(null),
        rectCaptor2.capture(),
        eq(currentPaint!!)
    )
    assertThat(rectCaptor2.lastValue.top).isEqualTo(0)
    assertThat(rectCaptor2.lastValue.left).isEqualTo(10)
    assertThat(rectCaptor2.lastValue.right).isEqualTo(15)
    assertThat(rectCaptor2.lastValue.bottom).isEqualTo(5)
    verify(bitmaps[1]).recycle()

    // Finish processing
    verify(bitmapManipulator).encodeBitmap(
        bitmap = totalBitmap,
        format = PNG,
        quality = 100,
        file = cacheFile
    )
    verify(totalBitmap).recycle()

    // Done
    verify(engineOwner).onDoneProcessing()
    verify(mediaScanner).scan(eq(cacheFilePath), any())
    verify(engineOwner).showContentLoading(false)
    verify(engineOwner).launchViewer(any())
  }

  // Vertical processing

  @Test fun performVerticalProcessing_fullScale_scaleToLargest() = runBlocking {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)
    setupBitmapIterator(
        Size(5, 5),
        Size(5, 5)
    )

    // Setup Prefs
    val spacingVertical = 5
    whenever(spacingVerticalPref.get()).doReturn(spacingVertical)
    whenever(bgFillColorPref.get()).doReturn(COLOR_BLUE)
    whenever(scalePriorityPref.get()).doReturn(true)

    // Setup Bitmap/Canvas/Paint stuff
    val totalWidth = 5   // horizontal, so width is always thw same
    val totalHeight = 15 // total heights + vertical spacing
    val totalBitmap = fakeBitmap(totalWidth, totalHeight)
    whenever(bitmapManipulator.createEmptyBitmap(totalWidth, totalHeight))
        .doReturn(totalBitmap)

    // Setup IO Manager
    val cacheFilePath = "file://idk/result"
    val cacheFile = mock<File> {
      on { toString() } doReturn cacheFilePath
    }
    whenever(ioManager.makeTempFile(".png")).doReturn(cacheFile)

    // Perform Actions
    engine.performVerticalProcessing(
        selectedScale = 1.0,
        resultWidth = totalWidth,
        resultHeight = totalHeight,
        format = PNG,
        quality = 100
    )

    // Verify Results
    verify(currentCanvas!!).drawColor(COLOR_BLUE)
    verify(engineOwner).showContentLoading(true)

    // Draw first Bitmap
    val rectCaptor1 = argumentCaptor<Rect>()
    verify(currentCanvas!!, times(1)).drawBitmap(
        eq(bitmaps[0]),
        eq(null),
        rectCaptor1.capture(),
        eq(currentPaint!!)
    )
    assertThat(rectCaptor1.lastValue.top).isEqualTo(0)
    assertThat(rectCaptor1.lastValue.left).isEqualTo(0)
    assertThat(rectCaptor1.lastValue.right).isEqualTo(5)
    assertThat(rectCaptor1.lastValue.bottom).isEqualTo(5)
    verify(bitmaps[0]).recycle()

    // Draw last Bitmap
    val rectCaptor2 = argumentCaptor<Rect>()
    verify(currentCanvas!!, times(1)).drawBitmap(
        eq(bitmaps[1]),
        eq(null),
        rectCaptor2.capture(),
        eq(currentPaint!!)
    )
    assertThat(rectCaptor2.lastValue.top).isEqualTo(10)
    assertThat(rectCaptor2.lastValue.left).isEqualTo(0)
    assertThat(rectCaptor2.lastValue.right).isEqualTo(5)
    assertThat(rectCaptor2.lastValue.bottom).isEqualTo(15)
    verify(bitmaps[1]).recycle()

    // Finish processing
    verify(bitmapManipulator).encodeBitmap(
        bitmap = totalBitmap,
        format = PNG,
        quality = 100,
        file = cacheFile
    )
    verify(totalBitmap).recycle()

    // Done
    verify(engineOwner).onDoneProcessing()
    verify(mediaScanner).scan(eq(cacheFilePath), any())
    verify(engineOwner).showContentLoading(false)
    verify(engineOwner).launchViewer(any())
  }

  // Finish processing specifies (we test success cases above)

  @Test fun finishProcessing_noProcessed() = runBlocking {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)

    val image = mock<Bitmap>()
    engine.finishProcessing(
        processedCount = 0,
        result = image,
        format = PNG,
        quality = 100
    )
    verify(image).recycle()
  }

  @Test fun finishProcessing_encodeError() = runBlocking {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)

    val cacheFile = mock<File>()
    whenever(ioManager.makeTempFile(".png")).doReturn(cacheFile)

    val error = Exception("Oh no!")
    whenever(engineOwner.showErrorDialog(any())).doAnswer {
      // Do nothing
    }
    whenever(bitmapManipulator.encodeBitmap(any(), any(), any(), any()))
        .doAnswer { throw error }

    val image = mock<Bitmap>()
    engine.finishProcessing(
        processedCount = 1,
        result = image,
        format = PNG,
        quality = 100
    )
    verify(image).recycle()
    verify(cacheFile).delete()
    verify(engineOwner).showErrorDialog(error)
  }

  // Utility functions

  private fun setupBitmapIterator(vararg sizes: Size) {
    val bitmapIterator = BitmapIterator(photos, bitmapManipulator)
    val photoBitmaps = mutableListOf<Bitmap>()

    for ((i, photo) in photos.withIndex()) {
      val image = fakeBitmap(
          width = sizes[i].width,
          height = sizes[i].height
      )
      photoBitmaps.add(image)
      // In our @Before setup() method above, decodePhoto(...)
      // uses decodeUri(...) to pull dimensions or a final Bitmap.
      whenever(bitmapManipulator.decodeUri(eq(photo.uri), any()))
          .doReturn(image)
    }

    engine.setBitmapIterator(bitmapIterator)
    this.bitmaps = photoBitmaps.toList()
  }
}
