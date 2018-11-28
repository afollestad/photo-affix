/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.net.Uri

/** @author Aidan Follestad (afollestad) */
interface EngineOwner {

  fun showImageSizingDialog(
    width: Int,
    height: Int
  )

  fun showContentLoading(loading: Boolean)

  fun showErrorDialog(e: Exception)

  fun onDoneProcessing()

  fun launchViewer(uri: Uri)
}
