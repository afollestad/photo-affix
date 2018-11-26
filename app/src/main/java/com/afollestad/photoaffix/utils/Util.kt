/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.afollestad.photoaffix.R.string
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** @author Aidan Follestad (afollestad) */
object Util {

  fun makeTempFile(
    context: Context,
    extension: String
  ): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val parent =
      File(Environment.getExternalStorageDirectory(), context.getString(string.app_name))
    parent.mkdirs()
    return File(parent, "AFFIX_$timeStamp$extension")
  }

  @Throws(FileNotFoundException::class)
  fun openStream(
    context: Context?,
    uri: Uri
  ): InputStream? {
    if (context == null) {
      return null
    }
    val scheme = uri.scheme ?: ""
    return if (scheme.equals("file", ignoreCase = true)) {
      FileInputStream(uri.path!!)
    } else {
      context.contentResolver.openInputStream(uri)
    }
  }
}
