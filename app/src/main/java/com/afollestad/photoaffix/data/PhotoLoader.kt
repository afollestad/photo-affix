/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.data

import android.app.Application
import com.afollestad.photoaffix.utilities.toUri
import javax.inject.Inject

interface PhotoLoader {

  suspend fun queryPhotos(): List<Photo>
}

class RealPhotoLoader @Inject constructor(app: Application) : PhotoLoader {

  private val contentResolver = app.contentResolver
  private val photosUri = "content://media/external/images/media".toUri()

  override suspend fun queryPhotos(): List<Photo> {
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
      }
    }
  }
}
