/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.app.Application
import android.media.MediaScannerConnection.scanFile
import android.net.Uri
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

typealias ScanResult = suspend (path: String, uri: Uri) -> Unit

interface MediaScanner {

  suspend fun scan(
    file: File,
    callbackContext: CoroutineContext = Main,
    cb: ScanResult? = null
  )
}

/** @author Aidan Follestad (afollestad) */
class RealMediaScanner @Inject constructor(
  private val app: Application
) : MediaScanner {

  override suspend fun scan(
    file: File,
    callbackContext: CoroutineContext,
    cb: ScanResult?
  ) = scanFile(app, arrayOf(file.toString()), null) { resultPath, uri ->
    GlobalScope.launch(callbackContext) {
      cb?.invoke(resultPath, uri)
    }
  }
}
