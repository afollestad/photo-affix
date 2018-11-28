/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities.ext

import android.net.Uri

fun String.toUri() = Uri.parse(this)!!
