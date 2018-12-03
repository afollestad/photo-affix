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
package com.afollestad.photoaffix.engine.photos

import android.app.Application
import com.afollestad.photoaffix.utilities.ext.toUri
import javax.inject.Inject

interface PhotoLoader {

  suspend fun queryPhotos(): List<Photo>

  fun isQuerying(): Boolean
}

class RealPhotoLoader @Inject constructor(app: Application) : PhotoLoader {

  private var isQuerying = false
  private val contentResolver = app.contentResolver
  private val photosUri = "content://media/external/images/media".toUri()

  override suspend fun queryPhotos(): List<Photo> {
    isQuerying = true
    val cursor = contentResolver.query(
        photosUri, // uri
        null, // projection
        null, // selection
        null, // selectionArgs
        "datetaken DESC" // sortOrder
    )!!
    cursor.use {
      return mutableListOf<Photo>().also { list ->
        if (it.moveToFirst()) {
          do {
            list.add(Photo.pull(it))
          } while (cursor.moveToNext())
        }
        isQuerying = false
      }
    }
  }

  override fun isQuerying() = isQuerying
}
