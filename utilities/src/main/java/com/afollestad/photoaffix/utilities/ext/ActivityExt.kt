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
