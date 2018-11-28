/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.app.Application
import android.media.MediaScannerConnection.scanFile
import android.net.Uri
import javax.inject.Inject

typealias ScanResult = (path: String, uri: Uri) -> Unit

interface MediaScanner {

  fun scan(
    path: String,
    cb: ScanResult? = null
  )
}

/** @author Aidan Follestad (afollestad) */
class RealMediaScanner @Inject constructor(
  private val app: Application
) : MediaScanner {

  override fun scan(
    path: String,
    cb: ScanResult?
  ) = scanFile(app, arrayOf(path), null) { resultPath, uri ->
    cb?.invoke(resultPath, uri)
  }
}
