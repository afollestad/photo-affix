/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.content.Context
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension

fun Double.round(): Double {
  return Math.round(this * 100.0) / 100.0
}

fun Int.dp(context: Context) = applyDimension(
    COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
)
