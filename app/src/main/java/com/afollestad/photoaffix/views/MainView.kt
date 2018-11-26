/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import android.net.Uri

interface MainView {

  fun lockOrientation()

  fun unlockOrientation()

  fun showContentLoading(loading: Boolean)

  fun showImageSizingDialog(
    width: Int,
    height: Int
  )

  fun clearSelection()

  fun launchViewer(uri: Uri)

  fun showErrorDialog(e: Exception)

  fun showMemoryError()
}
