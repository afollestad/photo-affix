/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt fun Context.colorAttr(@AttrRes attr: Int, @ColorInt fallback: Int = 0): Int {
  val a = theme.obtainStyledAttributes(intArrayOf(attr))
  return try {
    a.getColor(0, fallback)
  } catch (ignored: Throwable) {
    fallback
  } finally {
    a.recycle()
  }
}
