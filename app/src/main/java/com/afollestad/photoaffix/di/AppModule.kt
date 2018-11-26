/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.di

import com.afollestad.photoaffix.data.PhotoLoader
import com.afollestad.photoaffix.data.RealPhotoLoader
import com.afollestad.photoaffix.presenters.AffixPresenter
import com.afollestad.photoaffix.presenters.RealAffixPresenter
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Module
abstract class AppModule {

  @Binds
  @Singleton
  abstract fun providePhotoLoader(realPhotoLoader: RealPhotoLoader): PhotoLoader

  @Binds
  @Singleton
  abstract fun provideAffixPresenter(affixer: RealAffixPresenter): AffixPresenter
}
