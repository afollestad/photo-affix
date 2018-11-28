/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
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
  fun provideAppName(app: Application) = app.resources.getString(R.string.app_name)!!

  @Provides
  @Singleton
  @MainDispatcher
  fun provideMainDispatcher(): CoroutineContext = Dispatchers.Main

  @Provides
  @Singleton
  @IoDispatcher
  fun provideIoDispatcher(): CoroutineContext = Dispatchers.IO
}
