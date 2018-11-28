/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Module
abstract class UtilityModule {

  @Binds
  @Singleton
  abstract fun provideDpConverter(dpConverter: RealDpConverter): DpConverter

  @Binds
  @Singleton
  abstract fun provideIoManager(ioManager: RealIoManager): IoManager

  @Binds
  @Singleton
  abstract fun provideMediaScanner(mediaScanner: RealMediaScanner): MediaScanner
}
