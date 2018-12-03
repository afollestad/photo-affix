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
package com.afollestad.photoaffix.presenters

import android.net.Uri
import com.afollestad.photoaffix.engine.photos.UriParser
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock

val testUriParser: UriParser = { data ->
  val uri = mock<Uri> {
    on { scheme } doReturn data.substring(0, data.indexOf('/') - 1)
    on { path } doReturn data.substring(data.indexOf('/') + 1)
    on { toString() } doReturn data
  }
  uri
}
