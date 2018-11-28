/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.presenters

import android.app.Application
import android.graphics.Bitmap.CompressFormat
import android.media.MediaScannerConnection.scanFile
import android.net.Uri
import com.afollestad.photoaffix.engine.AffixEngine
import com.afollestad.photoaffix.engine.Photo
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.ext.closeQuietely
import com.afollestad.photoaffix.views.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface MainPresenter {

  fun attachView(mainView: MainView)

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
  private val app: Application,
  private val affixEngine: AffixEngine,
  private val ioManager: IoManager
) : MainPresenter {

  private var mainView: MainView? = null

  override fun attachView(mainView: MainView) {
    this.mainView = mainView
  }

  override fun onClickAffix(photos: List<Photo>) {
    mainView?.lockOrientation() ?: return
    affixEngine.process(photos, mainView!!)
  }

  override fun onExternalPhotoSelected(uri: Uri) {
    GlobalScope.launch(Dispatchers.IO) {
      var input: InputStream? = null
      var output: FileOutputStream? = null
      val targetFile = ioManager.makeTempFile(".png")

      try {
        input = ioManager.openStream(uri)
        output = FileOutputStream(targetFile)
        input!!.copyTo(output)
        output.close()

        withContext(Main) {
          scanFile(app, arrayOf(targetFile.toString()), null) { _, _ ->
            mainView?.refresh()
          }
        }
      } catch (e: Exception) {
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
    affixEngine.onSizeConfirmed(
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
