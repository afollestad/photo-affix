/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.BitmapFactory.Options
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import junit.framework.TestCase.fail
import org.junit.Test

class BitmapIteratorTest {

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser),
      Photo(0, "file://idk/3", 0, testUriParser)
  )

  private val bitmapDecoder = mock<BitmapManipulator> {
    on { createOptions(any()) } doAnswer { inv ->
      Options().apply { inJustDecodeBounds = inv.getArgument(0) }
    }
    on { decodePhoto(any(), any()) } doAnswer { inv ->
      val photo = inv.getArgument<Photo>(0)
      val options = inv.getArgument<Options>(1)
      when (photo) {
        photos[0] -> {
          options.outWidth = 2
          options.outHeight = 4
        }
        photos[1] -> {
          options.outWidth = 3
          options.outHeight = 6
        }
        photos[2] -> {
          options.outWidth = 4
          options.outHeight = 8
        }
        else -> fail("Unknown photo: $photo")
      }
      if (options.inJustDecodeBounds) {
        null
      } else {
        fakeBitmap(options.outWidth, options.outHeight)
      }
    }
  }

  private val bitmapIterator = BitmapIterator(
      photos = photos,
      bitmapManipulator = bitmapDecoder
  )

  @Test fun traversal() {
    // Test next()
    assertThat(bitmapIterator.currentOptions()).isNull()

    bitmapIterator.next()
    assertThat(bitmapIterator.traverseIndex()).isEqualTo(0)

    val optionsCaptor = argumentCaptor<Options>()
    verify(bitmapDecoder).decodePhoto(
        eq(photos[0]),
        optionsCaptor.capture()
    )
    assertThat(bitmapIterator.currentOptions())
        .isEqualTo(optionsCaptor.firstValue)

    // Test currentBitmap()
    val currentBitmap = bitmapIterator.currentBitmap()
    verify(bitmapDecoder, times(2)).decodePhoto(
        photos[0],
        optionsCaptor.firstValue
    )
    assertThat(currentBitmap.width).isEqualTo(2)
    assertThat(currentBitmap.height).isEqualTo(4)
  }

  @Test(expected = IllegalArgumentException::class)
  fun currentBitmapWithoutNext() {
    bitmapIterator.currentBitmap()
  }

  @Test(expected = IllegalStateException::class)
  fun traversalTooFar() {
    assertThat(bitmapIterator.hasNext()).isTrue()
    bitmapIterator.next()
    assertThat(bitmapIterator.hasNext()).isTrue()
    bitmapIterator.next()
    assertThat(bitmapIterator.hasNext()).isTrue()
    bitmapIterator.next()
    assertThat(bitmapIterator.hasNext()).isFalse()
    bitmapIterator.next()
  }

  @Test fun size() {
    assertThat(bitmapIterator.size()).isEqualTo(3)
  }

  @Test fun reset() {
    bitmapIterator.next()
    bitmapIterator.next()
    assertThat(bitmapIterator.traverseIndex()).isEqualTo(1)
    bitmapIterator.reset()
    assertThat(bitmapIterator.traverseIndex()).isEqualTo(-1)
  }
}
