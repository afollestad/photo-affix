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
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.utilities.qualifiers.AppName
import com.afollestad.photoaffix.utilities.qualifiers.IoDispatcher
import com.afollestad.photoaffix.utilities.qualifiers.MainDispatcher
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/** @author Aidan Follestad (afollestad) */
@Module
open class AppProvideModule {

  @Provides
  @Singleton
  @AppName
  fun provideAppName(app: Application): String = app.resources.getString(R.string.app_name)

  @Provides
  @Singleton
  @MainDispatcher
  fun provideMainDispatcher(): CoroutineContext = Dispatchers.Main

  @Provides
  @Singleton
  @IoDispatcher
  fun provideIoDispatcher(): CoroutineContext = Dispatchers.IO
}
