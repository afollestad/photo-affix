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
import android.net.Uri
import android.os.Environment
import com.afollestad.photoaffix.utilities.ext.closeQuietely
import com.afollestad.photoaffix.utilities.qualifiers.AppName
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface IoManager {

  fun makeTempFile(extension: String): File

  fun openStream(uri: Uri): InputStream?

  fun copyUriToFile(
    uri: Uri,
    file: File
  )
}

/** @author Aidan Follestad (afollestad) */
class RealIoManager @Inject constructor(
  private val app: Application,
  @AppName private val appName: String
) : IoManager {

  override fun makeTempFile(extension: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val parent = File(Environment.getExternalStorageDirectory(), appName)
    parent.mkdirs()
    return File(parent, "AFFIX_$timeStamp$extension")
  }

  override fun openStream(uri: Uri): InputStream? {
    val scheme = uri.scheme ?: ""
    return if (scheme.equals("file", ignoreCase = true)) {
      FileInputStream(uri.path!!)
    } else {
      app.contentResolver.openInputStream(uri)
    }
  }

  override fun copyUriToFile(
    uri: Uri,
    file: File
  ) {
    var input: InputStream? = null
    var output: FileOutputStream? = null

    try {
      input = openStream(uri)
      output = FileOutputStream(file)
      input!!.copyTo(output)
    } finally {
      input.closeQuietely()
      output.closeQuietely()
    }
  }
}
