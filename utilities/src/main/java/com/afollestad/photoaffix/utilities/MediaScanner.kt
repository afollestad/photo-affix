/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
