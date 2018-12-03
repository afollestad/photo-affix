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

import com.afollestad.photoaffix.engine.photos.PhotoLoader
import com.afollestad.photoaffix.engine.photos.RealPhotoLoader
import com.afollestad.photoaffix.presenters.MainPresenter
import com.afollestad.photoaffix.presenters.RealMainPresenter
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Module
abstract class AppBindModule {

  @Binds
  @Singleton
  abstract fun providePhotoLoader(realPhotoLoader: RealPhotoLoader): PhotoLoader

  @Binds
  @Singleton
  abstract fun provideMainPresenter(presenter: RealMainPresenter): MainPresenter
}
