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
