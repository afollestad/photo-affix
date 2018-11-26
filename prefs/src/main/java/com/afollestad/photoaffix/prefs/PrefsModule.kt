/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.prefs

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

  companion object {
    private const val KEY_PREFERENCES = "[user-settings]"
    private const val KEY_STACK_HORIZONTALLY = "stack_horizontally"
    private const val KEY_SCALE_PRIORITY = "scale_priority"
    private const val KEY_BG_FILL_COLOR = "bg_fill_color"
    private const val KEY_SPACING_VERTICAL = "image_spacing_vertical"
    private const val KEY_SPACING_HORIZONTAL = "image_spacing_horizontal"
  }

  @Provides
  @Singleton
  fun provideRxkPrefs(app: Application): RxkPrefs {
    return rxkPrefs(app, KEY_PREFERENCES)
  }

  @Provides
  @Singleton
  @StackHorizontally
  fun provideStackHorizontallyPref(prefs: RxkPrefs): Pref<Boolean> {
    return prefs.boolean(KEY_STACK_HORIZONTALLY, true)
  }

  @Provides
  @Singleton
  @ScalePriority
  fun provideScalePriorityPref(prefs: RxkPrefs): Pref<Boolean> {
    return prefs.boolean(KEY_SCALE_PRIORITY, true)
  }

  @Provides
  @Singleton
  @BgFillColor
  fun provideBgFillColorPref(prefs: RxkPrefs): Pref<Int> {
    return prefs.integer(KEY_BG_FILL_COLOR, TRANSPARENT)
  }

  @Provides
  @Singleton
  @ImageSpacingVertical
  fun provideImageSpacingVerticalPref(prefs: RxkPrefs): Pref<Int> {
    return prefs.integer(KEY_SPACING_VERTICAL, 0)
  }

  @Provides
  @Singleton
  @ImageSpacingHorizontal
  fun provideImageSpacingHorizontalPref(prefs: RxkPrefs): Pref<Int> {
    return prefs.integer(KEY_SPACING_HORIZONTAL, 0)
  }
}
