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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class MainPresenterTest {

  private val ioManager = mock<IoManager>()
  private val photoLoader = mock<PhotoLoader>()

  private val presenter = RealMainPresenter(
      ioManager,
      photoLoader
  )

  @Test fun loadPhotos() = runBlocking {
    val photos = listOf(
        Photo(0, "file://idk/1", 0, testUriParser),
        Photo(0, "file://idk/2", 0, testUriParser)
    )
    whenever(photoLoader.queryPhotos()).doReturn(photos)

    val results = presenter.loadPhotos()
    assertThat(results).isEqualTo(photos)
  }

  @Test fun loadPhotos_tooFast() = runBlocking {
    val photos = listOf(
        Photo(0, "file://idk/1", 0, testUriParser),
        Photo(0, "file://idk/2", 0, testUriParser)
    )
    whenever(photoLoader.queryPhotos()).doReturn(photos)

    presenter.loadPhotos()
    presenter.loadPhotos()
    presenter.loadPhotos()

    verify(photoLoader, times(1)).queryPhotos()
    return@runBlocking
  }

  @Test fun resetLoadThreshold() = runBlocking {
    val photos = listOf(
        Photo(0, "file://idk/1", 0, testUriParser),
        Photo(0, "file://idk/2", 0, testUriParser)
    )
    whenever(photoLoader.queryPhotos()).doReturn(photos)

    assertThat(presenter.getLastLoadTimestamp()).isEqualTo(-1)
    presenter.loadPhotos()

    assertThat(presenter.getLastLoadTimestamp()).isGreaterThan(-1)
    presenter.resetLoadThreshold()

    assertThat(presenter.getLastLoadTimestamp()).isEqualTo(-1)
  }

  @Test fun onExternalPhotoSelected() = runBlocking {
    val uri = mock<Uri>()
    val path = "file://idk/1.png"
    val cacheFile = mock<File> {
      on { toString() } doReturn path
    }
    whenever(ioManager.makeTempFile(".png"))
        .doReturn(cacheFile)

    val result = presenter.onExternalPhotoSelected(uri)

    verify(ioManager).copyUriToFile(uri, cacheFile)
    assertThat(result).isEqualTo(cacheFile)
  }
}
