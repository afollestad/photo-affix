/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine.subengines

import java.io.File

/** @author Aidan Follestad (afollestad) */
data class CommitResult(
  val outputFile: File? = null,
  val error: Exception? = null
) {

  fun isError() = error != null
}
