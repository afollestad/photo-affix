/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.di

import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.bitmaps.RealBitmapManipulator
import com.afollestad.photoaffix.utilities.DpConverter
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.RealDpConverter
import com.afollestad.photoaffix.utilities.RealIoManager
import com.afollestad.photoaffix.utilities.RealMediaScanner
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Module
abstract class AppUtilityModule {

  @Binds
  @Singleton
  abstract fun provideDpConverter(dpConverter: RealDpConverter): DpConverter

  @Binds
  @Singleton
  abstract fun provideIoManager(ioManager: RealIoManager): IoManager

  @Binds
  @Singleton
  abstract fun provideBitmapDecoder(bitmapManipulator: RealBitmapManipulator): BitmapManipulator

  @Binds
  @Singleton
  abstract fun provideMediaScanner(mediaScanner: RealMediaScanner): MediaScanner
}
