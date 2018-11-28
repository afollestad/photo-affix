/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.presenters

import android.net.Uri
import com.afollestad.photoaffix.engine.photos.UriParser
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ScanResult
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

val testUriParser: UriParser = { data ->
  val uri = mock<Uri> {
    on { scheme } doReturn data.substring(0, data.indexOf('/') - 1)
    on { path } doReturn data.substring(data.indexOf('/') + 1)
    on { toString() } doReturn data
  }
  uri
}

suspend fun testMediaScanner(cbContext: CoroutineContext = Dispatchers.Default): MediaScanner {
  val mediaScanner = mock<MediaScanner>()
  val fakeUri = mock<Uri>()
  doAnswer { inv ->
    val path = inv.getArgument<String>(0)
    val callback = inv.getArgument<ScanResult?>(1)
    GlobalScope.launch(cbContext) {
      callback?.invoke(path, fakeUri)
    }
    null
  }.whenever(mediaScanner)
      .scan(any(), any())
  return mediaScanner
}
