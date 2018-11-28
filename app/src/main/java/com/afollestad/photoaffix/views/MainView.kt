/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import com.afollestad.photoaffix.engine.EngineOwner
import com.afollestad.photoaffix.engine.photos.Photo

interface MainView : EngineOwner {

  fun refresh()

  fun setPhotos(photos: List<Photo>)

  fun lockOrientation()

  fun unlockOrientation()

  fun clearSelection()
}
