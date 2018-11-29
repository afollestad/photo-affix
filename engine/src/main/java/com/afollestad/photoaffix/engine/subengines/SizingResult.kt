/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine.subengines

/** @author Aidan Follestad (afollestad) */
data class SizingResult(
  val size: Size? = null,
  val error: Exception? = null
) {

  fun hasSize() = size != null

  fun isError() = error != null
}
