/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.presenters

import android.graphics.Bitmap.CompressFormat
import com.afollestad.photoaffix.engine.AffixEngine
import com.afollestad.photoaffix.engine.Photo
import com.afollestad.photoaffix.views.MainView
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface MainPresenter {

  fun attachView(mainView: MainView)

  fun onClickAffix(photos: List<Photo>)

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
  private val affixEngine: AffixEngine
) : MainPresenter {

  private var mainView: MainView? = null

  override fun attachView(mainView: MainView) {
    this.mainView = mainView
  }

  override fun onClickAffix(photos: List<Photo>) {
    mainView?.lockOrientation() ?: return
    affixEngine.process(photos, mainView!!)
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
