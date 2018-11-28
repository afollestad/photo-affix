/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
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
