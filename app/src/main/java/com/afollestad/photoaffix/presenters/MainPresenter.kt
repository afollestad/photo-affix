/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.presenters

import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import com.afollestad.photoaffix.engine.AffixEngine
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.photos.PhotoLoader
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ext.closeQuietely
import com.afollestad.photoaffix.utilities.qualifiers.IoDispatcher
import com.afollestad.photoaffix.utilities.qualifiers.MainDispatcher
import com.afollestad.photoaffix.views.MainView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.System.currentTimeMillis
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/** @author Aidan Follestad (afollestad) */
interface MainPresenter {

  fun attachView(mainView: MainView)

  fun loadPhotos()

  fun resetLoadThreshold()

  fun onClickAffix(photos: List<Photo>)

  fun onExternalPhotoSelected(uri: Uri)

  fun sizeDetermined(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  )

  fun clearPhotos()

  fun detachView()
}

/** @author Aidan Follestad (afollestad) */
class RealMainPresenter @Inject constructor(
  private val mediaScanner: MediaScanner,
  private val affixEngine: AffixEngine,
  private val ioManager: IoManager,
  private val photoLoader: PhotoLoader,
  @MainDispatcher private val mainContext: CoroutineContext,
  @IoDispatcher private val ioContext: CoroutineContext
) : MainPresenter {

  companion object {
    const val LOAD_PHOTOS_DEBOUNCE_MS = 5000
  }

  private var mainView: MainView? = null
  private var lastLoadTimestamp: Long = -1

  override fun attachView(mainView: MainView) {
    this.mainView = mainView
  }

  override fun loadPhotos() {
    if (lastLoadTimestamp > -1) {
      val threshold = lastLoadTimestamp + LOAD_PHOTOS_DEBOUNCE_MS
      if (currentTimeMillis() < threshold) {
        // Not enough time has passed
        return
      }
    }

    lastLoadTimestamp = currentTimeMillis()
    GlobalScope.launch(mainContext) {
      val photos = withContext(ioContext) { photoLoader.queryPhotos() }
      mainView?.setPhotos(photos)
    }
  }

  override fun resetLoadThreshold() {
    lastLoadTimestamp = -1
  }

  override fun onClickAffix(photos: List<Photo>) {
    mainView?.lockOrientation() ?: return
    affixEngine.process(photos, mainView!!)
  }

  override fun onExternalPhotoSelected(uri: Uri) {
    GlobalScope.launch(ioContext) {
      var input: InputStream? = null
      var output: FileOutputStream? = null
      val targetFile = ioManager.makeTempFile(".png")

      try {
        input = ioManager.openStream(uri)
        output = FileOutputStream(targetFile)
        input!!.copyTo(output)
        output.close()

        withContext(mainContext) {
          mediaScanner.scan(targetFile.toString()) { _, _ ->
            mainView?.refresh(force = true)
          }
        }
      } catch (e: Exception) {
        targetFile.delete()
        mainView?.showErrorDialog(e)
      } finally {
        input.closeQuietely()
        output.closeQuietely()
      }
    }
  }

  override fun sizeDetermined(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  ) {
    if (cancelled) {
      affixEngine.reset()
      mainView?.unlockOrientation()
      return
    }
    affixEngine.confirmSize(
        scale,
        resultWidth,
        resultHeight,
        format,
        quality
    )
  }

  override fun clearPhotos() = affixEngine.reset()

  override fun detachView() {
    this.mainView = null
  }
}
