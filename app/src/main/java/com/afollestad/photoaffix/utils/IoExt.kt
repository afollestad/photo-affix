/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utils

import java.io.Closeable

fun Closeable?.closeQuietely() {
  try {
    this?.close()
  } catch (_: Throwable) {
  }
}
