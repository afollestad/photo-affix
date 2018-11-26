/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.di

import android.app.Application
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.utilities.AppName
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Module
open class AppProvideModule {

  @Provides
  @Singleton
  @AppName
  fun provideAppName(app: Application) = app.resources.getString(R.string.app_name)
}
