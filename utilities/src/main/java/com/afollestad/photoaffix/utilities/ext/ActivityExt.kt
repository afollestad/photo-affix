/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities.ext

import android.app.Activity
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes

fun Activity?.toast(@StringRes messageRes: Int? = null, message: String? = null) {
  if (this == null) {
    return
  } else if (Looper.myLooper() != Looper.getMainLooper()) {
    runOnUiThread { toast(messageRes, message) }
    return
  }
  if (messageRes != null) {
    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT)
        .show()
  } else if (message != null) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT)
        .show()
  }
}
