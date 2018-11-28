/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.presenters

import android.graphics.Bitmap.CompressFormat.PNG
import android.net.Uri
import com.afollestad.photoaffix.engine.AffixEngine
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.photos.PhotoLoader
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.views.MainView
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class MainPresenterTest {

  private val mediaScanner = runBlocking { testMediaScanner() }
  private val affixEngine = mock<AffixEngine>()
  private val ioManager = mock<IoManager>()
  private val photoLoader = mock<PhotoLoader>()

  private val presenter = RealMainPresenter(
      mediaScanner,
      affixEngine,
      ioManager,
      photoLoader,
      Dispatchers.Default,
      Dispatchers.Default
  )

  private val mainView = mock<MainView> {
    on { showErrorDialog(any()) } doAnswer { inv ->
      val exception = inv.getArgument<Exception>(0)
      throw exception
    }
  }

  @Before fun setup() {
    presenter.attachView(mainView)
  }

  @After fun destroy() {
    presenter.detachView()
  }

  @Test fun loadPhotos() = runBlocking {
    val photos = listOf(
        Photo(0, "file://idk/1", 0, testUriParser),
        Photo(0, "file://idk/2", 0, testUriParser)
    )
    whenever(photoLoader.queryPhotos()).doReturn(photos)

    presenter.loadPhotos()
    verify(mainView).setPhotos(photos)
  }

  @Test fun loadPhotos_error() = runBlocking {
    val photos = listOf(
        Photo(0, "file://idk/1", 0, testUriParser),
        Photo(0, "file://idk/2", 0, testUriParser)
    )
    val error = Exception("Oh no!")
    whenever(photoLoader.queryPhotos()).doAnswer { throw error }
    whenever(mainView.showErrorDialog(any())).doAnswer { inv ->
      val arg = inv.getArgument<Exception>(0)
      if (arg != error) {
        throw arg
      }
    }

    presenter.loadPhotos()

    verify(mainView, never()).setPhotos(photos)
    verify(mainView).showErrorDialog(error)
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
    verify(mainView, never()).showErrorDialog(any())
    verify(mainView, times(1)).setPhotos(photos)
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

  @Test fun onClickAffix() {
    val photos = listOf(
        Photo(0, "file://idk/1", 0, testUriParser),
        Photo(0, "file://idk/2", 0, testUriParser)
    )
    presenter.onClickAffix(photos)

    verify(mainView).lockOrientation()
    verify(affixEngine).process(photos, mainView)
  }

  @Test fun onExternalPhotoSelected() = runBlocking {
    val uri = mock<Uri>()
    val path = "file://idk/1.png"
    val cacheFile = mock<File> {
      on { toString() } doReturn path
    }
    whenever(ioManager.makeTempFile(".png"))
        .doReturn(cacheFile)

    presenter.onExternalPhotoSelected(uri)

    verify(ioManager).copyUriToFile(uri, cacheFile)
    verify(mediaScanner).scan(eq(path), any())
    verify(mainView, never()).showErrorDialog(any())
    verify(mainView).refresh(true)
  }

  @Test fun onExternalPhotoSelected_error() = runBlocking {
    val uri = mock<Uri>()
    val path = "file://idk/1.png"
    val cacheFile = mock<File> {
      on { toString() } doReturn path
    }
    whenever(ioManager.makeTempFile(".png"))
        .doReturn(cacheFile)

    val error = Exception("Oh no!")
    whenever(ioManager.copyUriToFile(any(), any()))
        .doAnswer { throw error }
    whenever(mainView.showErrorDialog(any())).doAnswer { inv ->
      val arg = inv.getArgument<Exception>(0)
      if (arg != error) {
        throw arg
      }
    }

    presenter.onExternalPhotoSelected(uri)

    verify(mainView).showErrorDialog(error)
    verify(cacheFile).delete()
    verify(mediaScanner, never()).scan(any(), any())
    verify(mainView, never()).refresh(any())
  }

  @Test fun sizeDetermined_cancelled() {
    presenter.sizeDetermined(
        scale = 1.0,
        resultWidth = 1,
        resultHeight = 1,
        format = PNG,
        quality = 100,
        cancelled = true
    )

    verify(affixEngine).reset()
    verify(mainView).unlockOrientation()
    verifyNoMoreInteractions(affixEngine)
  }

  @Test fun sizeDetermined() {
    presenter.sizeDetermined(
        scale = 1.0,
        resultWidth = 1,
        resultHeight = 1,
        format = PNG,
        quality = 100,
        cancelled = false
    )

    verify(affixEngine).confirmSize(
        1.0, 1, 1, PNG, 100
    )
  }

  @Test fun clearPhotos() {
    presenter.clearPhotos()
    verify(affixEngine).reset()
  }
}
