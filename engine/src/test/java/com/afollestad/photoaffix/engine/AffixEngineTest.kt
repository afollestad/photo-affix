/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.app.Application
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.rxkprefs.Pref
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test

class AffixEngineTest {

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser),
      Photo(0, "file://idk/3", 0, testUriParser)
  )

  private val app = mock<Application>()
  private val spacingVerticalPref = mock<Pref<Int>>()
  private val spacingHorizontalPref = mock<Pref<Int>>()
  private val scalePriorityPref = mock<Pref<Boolean>>()
  private val stackHorizontallyPref = mock<Pref<Boolean>>()
  private val bgFillColorPref = mock<Pref<Int>>()
  private val ioManager = mock<IoManager>()
  private val bitmapDecoder = mock<BitmapDecoder>()

  private val engineOwner = mock<EngineOwner>()

  private val engine = RealAffixEngine(
      app,
      spacingVerticalPref,
      spacingHorizontalPref,
      scalePriorityPref,
      stackHorizontallyPref,
      bgFillColorPref,
      ioManager,
      bitmapDecoder
  )

  @Test fun process() {
    whenever(stackHorizontallyPref.get()).doReturn(true)

    engine.process(photos, engineOwner)

    assertThat(engine.getEngineOwner()).isEqualTo(engineOwner)

    // TODO assert that we show the image sizing dialog if the
    // TODO width/height calcs come back as non-zero.
  }

  @Test fun reset() {
    engine.reset()
    verify(engine.getBitmapIterator()).reset()
  }

  @Test fun calculateHorizontalWidthAndHeight() {
  }
}
