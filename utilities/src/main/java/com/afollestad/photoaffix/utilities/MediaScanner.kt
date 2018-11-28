/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.app.Application
import android.media.MediaScannerConnection.scanFile
import android.net.Uri
import com.afollestad.photoaffix.utilities.qualifiers.MainDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

typealias ScanResult = suspend (path: String, uri: Uri) -> Unit

interface MediaScanner {

  suspend fun scan(
    path: String,
    cb: ScanResult? = null
  )
}

/** @author Aidan Follestad (afollestad) */
class RealMediaScanner @Inject constructor(
  private val app: Application,
  @MainDispatcher private val mainContext: CoroutineContext
) : MediaScanner {

  override suspend fun scan(
    path: String,
    cb: ScanResult?
  ) = scanFile(app, arrayOf(path), null) { resultPath, uri ->
    GlobalScope.launch(mainContext) {
      cb?.invoke(resultPath, uri)
    }
  }
}
