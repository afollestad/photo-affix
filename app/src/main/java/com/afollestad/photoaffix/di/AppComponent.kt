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
package com.afollestad.photoaffix.di

import android.app.Application
import com.afollestad.photoaffix.dialogs.ImageSpacingDialog
import com.afollestad.photoaffix.engine.EnginesModule
import com.afollestad.photoaffix.prefs.PrefsModule
import com.afollestad.photoaffix.utilities.UtilityModule
import com.afollestad.photoaffix.viewcomponents.SettingsLayout
import com.afollestad.photoaffix.views.MainActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Singleton
@Component(
    modules = [
      PrefsModule::class,
      AppBindModule::class,
      UtilityModule::class,
      EnginesModule::class,
      AppProvideModule::class
    ]
)
interface AppComponent {

  fun inject(mainActivity: MainActivity)

  fun inject(settingsLayout: SettingsLayout)

  fun inject(imageSpacingDialog: ImageSpacingDialog)

  @Component.Builder
  interface Builder {

    @BindsInstance fun application(application: Application): Builder

    fun build(): AppComponent
  }
}
