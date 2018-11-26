/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utils

import com.afollestad.photoaffix.App
import com.afollestad.photoaffix.activities.MainActivity

fun MainActivity.inject() {
  (application as App).appComponent.inject(this)
}
