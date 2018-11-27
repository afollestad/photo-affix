/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test

class BitmapIteratorTest {

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser),
      Photo(0, "file://idk/3", 0, testUriParser)
  )
  private val bitmapDecoder = mock<BitmapDecoder>()
  private val bitmapIterator = BitmapIterator(
      photos,
      bitmapDecoder
  )

  @Test fun traversal() {
    // Test next()
    assertThat(bitmapIterator.currentOptions()).isNull()

    val nextOptions = mock<Options>()
    nextOptions.inJustDecodeBounds = true

    whenever(bitmapDecoder.createOptions(true)).doReturn(nextOptions)
    val nextBitmap = mock<Bitmap>()
    whenever(bitmapDecoder.decodePhoto(photos[0], nextOptions)).doReturn(nextBitmap)

    bitmapIterator.next()

    assertThat(bitmapIterator.traverseIndex()).isEqualTo(0)
    assertThat(bitmapIterator.currentOptions()).isEqualTo(nextOptions)
    verify(bitmapDecoder).decodePhoto(photos[0], nextOptions)

    // Test currentBitmap()
    val currentBitmap = bitmapIterator.currentBitmap()
    verify(bitmapDecoder, times(2)).decodePhoto(photos[0], nextOptions)
    assertThat(currentBitmap).isEqualTo(nextBitmap)
  }

  @Test(expected = IllegalStateException::class)
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
