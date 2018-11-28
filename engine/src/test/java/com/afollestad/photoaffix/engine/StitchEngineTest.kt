/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.afollestad.photoaffix.engine.bitmaps.BitmapIterator
import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.subengines.RealStitchEngine
import com.afollestad.photoaffix.engine.subengines.Size
import com.afollestad.rxkprefs.Pref
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class StitchEngineTest {

  companion object {
    const val COLOR_BLUE: Int = -0xffff01
  }

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser)
  )
  private var bitmaps = listOf<Bitmap>()

  private val spacingVerticalPref = mock<Pref<Int>>()
  private val spacingHorizontalPref = mock<Pref<Int>>()
  private val scalePriorityPref = mock<Pref<Boolean>>()
  private val stackHorizontallyPref = mock<Pref<Boolean>>()
  private val bgFillColorPref = mock<Pref<Int>>()

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
  private val engine = RealStitchEngine(
      testDpConverter(),
      bitmapManipulator,
      stackHorizontallyPref,
      scalePriorityPref,
      spacingVerticalPref,
      spacingHorizontalPref,
      bgFillColorPref,
      Dispatchers.Default,
      Dispatchers.Default
  ).apply {
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

  // Horizontal processing

  @Test fun stitchHorizontal_fullScale_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(5, 5),
        Size(5, 5)
    )
    engine.setup(bitmapIterator, engineOwner)

    // Setup Prefs
    val spacingHorizontal = 5
    whenever(spacingHorizontalPref.get()).doReturn(spacingHorizontal)
    whenever(bgFillColorPref.get()).doReturn(COLOR_BLUE)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(stackHorizontallyPref.get()).doReturn(true)

    // Setup Bitmap/Canvas/Paint stuff
    val totalWidth = 15 // bitmaps are both 5x5, + 5 horizontal spacing
    val totalHeight = 5 // horizontal, so height is the same for all
    val totalBitmap = fakeBitmap(totalWidth, totalHeight)
    whenever(bitmapManipulator.createEmptyBitmap(totalWidth, totalHeight))
        .doReturn(totalBitmap)

    // Perform Actions
    engine.stitch(
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
  }

  // Vertical processing

  @Test fun stitchVertical_fullScale_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(5, 5),
        Size(5, 5)
    )
    engine.setup(bitmapIterator, engineOwner)

    // Setup Prefs
    val spacingVertical = 5
    whenever(spacingVerticalPref.get()).doReturn(spacingVertical)
    whenever(bgFillColorPref.get()).doReturn(COLOR_BLUE)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Setup Bitmap/Canvas/Paint stuff
    val totalWidth = 5   // horizontal, so width is always thw same
    val totalHeight = 15 // total heights + vertical spacing
    val totalBitmap = fakeBitmap(totalWidth, totalHeight)
    whenever(bitmapManipulator.createEmptyBitmap(totalWidth, totalHeight))
        .doReturn(totalBitmap)

    // Perform Actions
    engine.stitch(
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
  }

  // Utility functions

  private fun mockBitmapIterator(vararg sizes: Size): BitmapIterator {
    val bitmapIterator =
      BitmapIterator(photos, bitmapManipulator)
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

    this.bitmaps = photoBitmaps.toList()
    return bitmapIterator
  }
}
