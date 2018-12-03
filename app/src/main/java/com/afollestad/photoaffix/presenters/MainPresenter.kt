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
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.photos.PhotoLoader
import com.afollestad.photoaffix.utilities.IoManager
import org.jetbrains.annotations.TestOnly
import java.io.File
import java.lang.System.currentTimeMillis
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface MainPresenter {

  suspend fun loadPhotos(): List<Photo>

  fun resetLoadThreshold()

  suspend fun onExternalPhotoSelected(uri: Uri): File?
}

/** @author Aidan Follestad (afollestad) */
class RealMainPresenter @Inject constructor(
  private val ioManager: IoManager,
  private val photoLoader: PhotoLoader
) : MainPresenter {

  companion object {
    const val LOAD_PHOTOS_DEBOUNCE_MS = 10000
  }

  private var lastLoadTimestamp: Long = -1

  override suspend fun loadPhotos(): List<Photo> {
    if (lastLoadTimestamp > -1) {
      val threshold = lastLoadTimestamp + LOAD_PHOTOS_DEBOUNCE_MS
      if (currentTimeMillis() < threshold) {
        // Not enough time has passed
        return listOf()
      }
    }

    lastLoadTimestamp = currentTimeMillis()
    return photoLoader.queryPhotos()
  }

  override fun resetLoadThreshold() {
    lastLoadTimestamp = -1
  }

  override suspend fun onExternalPhotoSelected(uri: Uri): File? {
    var targetFile: File? = null
    try {
      targetFile = ioManager.makeTempFile(".png")
      ioManager.copyUriToFile(uri, targetFile)
      return targetFile
    } catch (e: Exception) {
      targetFile?.delete()
      throw e
    }
  }

  @TestOnly fun getLastLoadTimestamp() = this.lastLoadTimestamp
}
