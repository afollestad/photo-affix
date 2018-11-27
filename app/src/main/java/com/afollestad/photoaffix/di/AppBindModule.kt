/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.di

import com.afollestad.photoaffix.engine.AffixEngine
import com.afollestad.photoaffix.engine.PhotoLoader
import com.afollestad.photoaffix.engine.RealAffixEngine
import com.afollestad.photoaffix.engine.RealPhotoLoader
import com.afollestad.photoaffix.presenters.MainPresenter
import com.afollestad.photoaffix.presenters.RealMainPresenter
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.RealIoManager
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

  @Binds
  @Singleton
  abstract fun provideIoManager(ioManager: RealIoManager): IoManager

  @Binds
  @Singleton
  abstract fun provideAffixEngine(affixEngine: RealAffixEngine): AffixEngine
}
