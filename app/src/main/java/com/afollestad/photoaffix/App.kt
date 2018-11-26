/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix

import android.app.Application
import com.afollestad.photoaffix.di.AppComponent
import com.afollestad.photoaffix.di.DaggerAppComponent
import com.afollestad.photoaffix.dialogs.ImageSpacingDialog
import com.afollestad.photoaffix.utilities.Injector
import com.afollestad.photoaffix.viewcomponents.SettingsLayout
import com.afollestad.photoaffix.views.MainActivity

/** @author Aidan Follestad (afollestad) */
class App : Application(), Injector {

  lateinit var appComponent: AppComponent

  override fun onCreate() {
    super.onCreate()
    appComponent = DaggerAppComponent.builder()
        .application(this)
        .build()
  }

  override fun injectInto(target: Any) {
    when (target) {
      is MainActivity -> {
        appComponent.inject(target)
      }
      is SettingsLayout -> {
        appComponent.inject(target)
      }
      is ImageSpacingDialog -> {
        appComponent.inject(target)
      }
      else -> throw IllegalArgumentException("Can't injectInto $target")
    }
  }
}
