/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.app.Application
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.rxkprefs.Pref
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

class AffixEngineTest {

  private val app = mock<Application>()
  private val spacingVerticalPref = mock<Pref<Int>>()
  private val spacingHorizontalPref = mock<Pref<Int>>()
  private val scalePriorityPref = mock<Pref<Boolean>>()
  private val stackHorizontallyPref = mock<Pref<Boolean>>()
  private val bgFillColorPref = mock<Pref<Int>>()
  private val ioManager = mock<IoManager>()
  private val bitmapDecoder = mock<BitmapDecoder>()

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

  @Test fun test() {
  }
}
