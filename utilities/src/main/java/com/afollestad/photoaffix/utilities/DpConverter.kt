/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.app.Application
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import androidx.annotation.Px
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface DpConverter {

  fun toDp(@Px pixels: Int): Float
}

class RealDpConverter @Inject constructor(
  private val app: Application
) : DpConverter {

  override fun toDp(pixels: Int) = applyDimension(
      COMPLEX_UNIT_DIP,
      pixels.toFloat(),
      app.resources.displayMetrics
  )
}
