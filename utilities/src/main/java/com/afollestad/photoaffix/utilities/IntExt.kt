/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

fun Double.round(): Double {
  return Math.round(this * 100.0) / 100.0
}
