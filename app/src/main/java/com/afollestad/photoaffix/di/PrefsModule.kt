/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.di

import android.app.Application
import android.graphics.Color.TRANSPARENT
import com.afollestad.rxkprefs.Pref
import com.afollestad.rxkprefs.RxkPrefs
import com.afollestad.rxkprefs.rxkPrefs
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
open class PrefsModule {

  @Provides
  @Singleton
  fun provideRxkPrefs(app: Application): RxkPrefs {
    return rxkPrefs(app, "[user-settings]")
  }

  @Provides
  @Singleton
  @StackHorizontally
  fun provideStackHorizontallyPref(prefs: RxkPrefs): Pref<Boolean> {
    return prefs.boolean("stack_horizontally", true)
  }

  @Provides
  @Singleton
  @ScalePriority
  fun provideScalePriorityPref(prefs: RxkPrefs): Pref<Boolean> {
    return prefs.boolean("scale_priority", true)
  }

  @Provides
  @Singleton
  @BgFillColor
  fun provideBgFillColorPref(prefs: RxkPrefs): Pref<Int> {
    return prefs.integer("bg_fill_color", TRANSPARENT)
  }

  @Provides
  @Singleton
  @ImageSpacingVertical
  fun provideImageSpacingVerticalPref(prefs: RxkPrefs): Pref<Int> {
    return prefs.integer("image_spacing_vertical", TRANSPARENT)
  }

  @Provides
  @Singleton
  @ImageSpacingHorizontal
  fun provideImageSpacingHorizontalPref(prefs: RxkPrefs): Pref<Int> {
    return prefs.integer("image_spacing_horizontal", TRANSPARENT)
  }
}
