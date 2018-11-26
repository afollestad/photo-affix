/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_90
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.photoaffix.R
import java.io.Closeable
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
      File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name))
    parent.mkdirs()
    return File(parent, "AFFIX_$timeStamp$extension")
  }

  fun Exception.showInDialog(context: Activity?) {
    if (context == null) {
      return
    } else if (Looper.myLooper() != Looper.getMainLooper()) {
      context.runOnUiThread { this.showInDialog(context) }
      return
    }
    this.printStackTrace()
    MaterialDialog(context).show {
      title(R.string.error)
      message(text = this@showInDialog.message)
      positiveButton(android.R.string.ok)
    }
  }

  fun Activity?.showMemoryError() = Exception(
      "You've run out of RAM for processing images; I'm working to improve memory usage! " +
          "Sit tight while this app is in beta."
  ).showInDialog(this)

  fun Activity?.lockOrientation() {
    if (this == null) {
      return
    }
    val orientation: Int
    val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay
        .rotation
    orientation = when (rotation) {
      ROTATION_0 -> SCREEN_ORIENTATION_PORTRAIT
      ROTATION_90 -> SCREEN_ORIENTATION_LANDSCAPE
      ROTATION_180 -> SCREEN_ORIENTATION_REVERSE_PORTRAIT
      else -> SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    }
    requestedOrientation = orientation
  }

  fun Activity?.unlockOrientation() {
    if (this == null) {
      return
    }
    requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
  }

  @Throws(FileNotFoundException::class)
  fun Uri?.openStream(context: Context?): InputStream? {
    if (this == null || context == null) {
      return null
    }
    return if (scheme == null || scheme!!.equals("file", ignoreCase = true)) {
      FileInputStream(path!!)
    } else {
      context.contentResolver.openInputStream(this)
    }
  }

  fun Closeable?.closeQuietely() {
    try {
      this?.close()
    } catch (_: Throwable) {
    }
  }

  fun Activity?.toast(@StringRes messageRes: Int? = null, message: String? = null) {
    if (this == null) {
      return
    } else if (Looper.myLooper() != Looper.getMainLooper()) {
      runOnUiThread { toast(messageRes, message) }
      return
    }
    if (messageRes != null) {
      Toast.makeText(this, messageRes, LENGTH_SHORT)
          .show()
    } else if (message != null) {
      Toast.makeText(this, message, LENGTH_SHORT)
          .show()
    }
  }

  fun Bitmap?.safeRecycle() = try {
    this?.recycle()
  } catch (_: Throwable) {
  }
}
