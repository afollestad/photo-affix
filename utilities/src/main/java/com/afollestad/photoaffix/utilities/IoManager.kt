/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.app.Application
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface IoManager {

  fun makeTempFile(extension: String): File

  fun openStream(uri: Uri): InputStream?
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
}
