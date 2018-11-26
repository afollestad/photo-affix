/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.animation

import android.animation.IntEvaluator
import android.view.View

/** @author Aidan Follestad (afollestad) */
class HeightEvaluator(private val v: View) : IntEvaluator() {

  override fun evaluate(
    fraction: Float,
    startValue: Int?,
    endValue: Int?
  ): Int {
    val num = super.evaluate(fraction, startValue, endValue)!!
    val params = v.layoutParams
    params.height = num
    v.layoutParams = params
    return num
  }
}
