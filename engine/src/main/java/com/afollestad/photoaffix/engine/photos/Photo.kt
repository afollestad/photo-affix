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
