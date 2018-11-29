/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.Surface
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.utilities.ext.hide
import kotlinx.android.synthetic.main.activity_main.content_loading_progress_frame

fun Activity.lockOrientation() {
  val orientation: Int
  val rotation = (getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager)
      .defaultDisplay
      .rotation
  orientation = when (rotation) {
    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
    else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
  }
  requestedOrientation = orientation
}

fun Activity.unlockOrientation() {
  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

fun Activity.viewUri(
  uri: Uri,
  onNoResolve: (() -> Unit)? = null
) {
  try {
    startActivity(
        Intent(ACTION_VIEW)
            .setDataAndType(uri, "image/*")
    )
  } catch (_: ActivityNotFoundException) {
    onNoResolve?.invoke()
  }
}

fun Activity.showErrorDialog(e: Exception) {
  unlockOrientation()
  content_loading_progress_frame.hide()

  e.printStackTrace()
  val message = if (e is OutOfMemoryError) {
    "Your device is low on RAM!"
  } else {
    e.message
  }
  MaterialDialog(this).show {
    title(R.string.error)
    message(text = message)
    positiveButton(android.R.string.ok)
  }
}
