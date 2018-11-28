/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities.ext

import java.io.Closeable

fun Closeable?.closeQuietely() {
  try {
    this?.close()
  } catch (_: Throwable) {
  }
}
