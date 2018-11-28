/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities.ext

import kotlin.math.roundToInt

fun Double.round(): Double {
  return Math.round(this * 100.0) / 100.0
}

fun Float.toRoundedInt(): Int {
  val result = this.roundToInt()
  return if (result == 0) 1 else result
}

fun Double.toRoundedInt(): Int {
  val result = this.roundToInt()
  return if (result == 0) 1 else result
}
