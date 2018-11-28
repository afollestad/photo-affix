/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.database.Cursor
import android.net.Uri
import com.afollestad.photoaffix.utilities.ext.toUri
import java.io.Serializable

typealias UriParser = (String) -> Uri

val defaultUriParser: UriParser = { data ->
  var uri = data.toUri()
  if (!uri.toString().startsWith("file://") &&
      !uri.toString().startsWith("content://")
  ) {
    uri = "file://$uri".toUri()
  }
  uri
}

/** @author Aidan Follestad (afollestad) */
data class Photo(
  var id: Long,
  var data: String,
  var dateTaken: Long,
  private var uriParser: UriParser = defaultUriParser
) : Serializable {

  companion object {
    fun pull(cursor: Cursor): Photo {
      return Photo(
          id = cursor.getLong(cursor.getColumnIndex("_id")),
          data = cursor.getString(cursor.getColumnIndex("_data")),
          dateTaken = cursor.getLong(cursor.getColumnIndex("datetaken"))
      )
    }
  }

  val uri = uriParser.invoke(data)
}
