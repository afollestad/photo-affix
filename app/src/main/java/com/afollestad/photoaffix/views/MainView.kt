/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import com.afollestad.photoaffix.engine.EngineOwner

interface MainView : EngineOwner {

  fun lockOrientation()

  fun unlockOrientation()

  fun clearSelection()
}
