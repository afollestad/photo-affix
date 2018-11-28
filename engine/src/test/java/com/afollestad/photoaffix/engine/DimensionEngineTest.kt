/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import com.afollestad.photoaffix.engine.bitmaps.BitmapIterator
import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.subengines.RealDimensionsEngine
import com.afollestad.photoaffix.engine.subengines.Size
import com.afollestad.rxkprefs.Pref
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class DimensionEngineTest {

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser)
  )
  private var bitmaps = listOf<Bitmap>()

  private val spacingVerticalPref = mock<Pref<Int>>()
  private val spacingHorizontalPref = mock<Pref<Int>>()
  private val scalePriorityPref = mock<Pref<Boolean>>()
  private val stackHorizontallyPref = mock<Pref<Boolean>>()

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

  private val engine = RealDimensionsEngine(
      testDpConverter(),
      stackHorizontallyPref,
      scalePriorityPref,
      spacingVerticalPref,
      spacingHorizontalPref,
      Dispatchers.Default
  )

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

  // Horizontal width and height calculation

  @Test fun calculateHorizontalWidthAndHeight_smallestFirst_noSpacing_scaleToLargest() =
    runBlocking {
      val bitmapIterator = mockBitmapIterator(
          Size(2, 4),
          Size(2, 8)
      )
      engine.setup(bitmapIterator, engineOwner)

      // No spacing and scale to largest
      val horizontalSpacing = 0
      whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
      whenever(scalePriorityPref.get()).doReturn(true)
      whenever(stackHorizontallyPref.get()).doReturn(true)

      // Perform actions
      val size = engine.calculateSize()
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

  @Test fun calculateHorizontalWidthAndHeight_largestFirst_noSpacing_scaleToLargest() =
    runBlocking {
      val bitmapIterator = mockBitmapIterator(
          Size(2, 8),
          Size(2, 4)
      )
      engine.setup(bitmapIterator, engineOwner)

      // No spacing and scale to largest
      val horizontalSpacing = 0
      whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
      whenever(scalePriorityPref.get()).doReturn(true)
      whenever(stackHorizontallyPref.get()).doReturn(true)

      // Perform actions
      val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_largestFirst_noSpacing_scaleToSmallest() =
    runBlocking {
      val bitmapIterator = mockBitmapIterator(
          Size(3, 12),
          Size(1, 3)
      )
      engine.setup(bitmapIterator, engineOwner)

      // No spacing and scale to smallest
      val horizontalSpacing = 0
      whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
      whenever(scalePriorityPref.get()).doReturn(false)
      whenever(stackHorizontallyPref.get()).doReturn(true)

      // Perform actions
      val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_smallestFirst_noSpacing_scaleToSmallest() =
    runBlocking {
      val bitmapIterator = mockBitmapIterator(
          Size(1, 3),
          Size(3, 12)
      )
      engine.setup(bitmapIterator, engineOwner)

      // No spacing and scale to smallest
      val horizontalSpacing = 0
      whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
      whenever(scalePriorityPref.get()).doReturn(false)
      whenever(stackHorizontallyPref.get()).doReturn(true)

      // Perform actions
      val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_largestFirst_withSpacing_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(4, 8),
        Size(2, 4)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(stackHorizontallyPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_smallestFirst_withSpacing_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(stackHorizontallyPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_largestFirst_withSpacing_scaleToSmallest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(4, 8),
        Size(2, 4)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_smallestFirst_withSpacing_scaleToSmallest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to largest
    val horizontalSpacing = 2
    whenever(spacingHorizontalPref.get()).doReturn(horizontalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(true)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeHorizontal_error_showDialogAndReset() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )
    engine.setup(bitmapIterator, engineOwner)

    whenever(spacingHorizontalPref.get()).doReturn(0)
    whenever(stackHorizontallyPref.get()).doReturn(true)
    whenever(engineOwner.showErrorDialog(any())).doAnswer {
      // no-op
    }

    val error = Exception("Oh no!")
    whenever(bitmapManipulator.createOptions(any())).doAnswer { throw error }

    val size = engine.calculateSize()
    verify(engineOwner).showErrorDialog(error)
    assertThat(size.isZero()).isTrue()
  }

  // Vertical width and height calculation

  @Test fun calculateSizeVertical_smallestFirst_noSpacing_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to largest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_largestFirst_noSpacing_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(4, 8),
        Size(2, 4)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to largest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(true)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_largestFirst_noSpacing_scaleToSmallest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to smallest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_smallestFirst_noSpacing_scaleToSmallest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to smallest
    val verticalSpacing = 0
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_largestFirst_withSpacing_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to smallest
    val verticalSpacing = 4
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_smallestFirst_withSpacing_scaleToLargest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to smallest
    val verticalSpacing = 4
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_largestFirst_withSpacing_scaleToSmallest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(3, 12),
        Size(1, 3)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to smallest
    val verticalSpacing = 6
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_smallestFirst_withSpacing_scaleToSmallest() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(1, 3),
        Size(3, 12)
    )
    engine.setup(bitmapIterator, engineOwner)

    // No spacing and scale to smallest
    val verticalSpacing = 6
    whenever(spacingVerticalPref.get()).doReturn(verticalSpacing)
    whenever(scalePriorityPref.get()).doReturn(false)
    whenever(stackHorizontallyPref.get()).doReturn(false)

    // Perform actions
    val size = engine.calculateSize()
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

  @Test fun calculateSizeVertical_error_showDialogAndReset() = runBlocking {
    val bitmapIterator = mockBitmapIterator(
        Size(2, 4),
        Size(4, 8)
    )
    engine.setup(bitmapIterator, engineOwner)

    whenever(spacingVerticalPref.get()).doReturn(0)
    whenever(stackHorizontallyPref.get()).doReturn(false)
    whenever(engineOwner.showErrorDialog(any())).doAnswer {
      // no-op
    }

    val error = Exception("Oh no!")
    whenever(bitmapManipulator.createOptions(any())).doAnswer { throw error }

    val size = engine.calculateSize()
    verify(engineOwner).showErrorDialog(error)
    assertThat(size.isZero()).isTrue()
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
