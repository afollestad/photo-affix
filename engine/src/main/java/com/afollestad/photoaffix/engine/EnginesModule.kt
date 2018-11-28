/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.bitmaps.RealBitmapManipulator
import com.afollestad.photoaffix.engine.subengines.DimensionsEngine
import com.afollestad.photoaffix.engine.subengines.RealDimensionsEngine
import com.afollestad.photoaffix.engine.subengines.RealStitchEngine
import com.afollestad.photoaffix.engine.subengines.StitchEngine
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Module
abstract class EnginesModule {

  @Binds
  @Singleton
  abstract fun provideDimensionEngine(dimensionsEngine: RealDimensionsEngine): DimensionsEngine

  @Binds
  @Singleton
  abstract fun provideStitchEngine(stitchEngine: RealStitchEngine): StitchEngine

  @Binds
  @Singleton
  abstract fun provideAffixEngine(affixEngine: RealAffixEngine): AffixEngine

  @Binds
  @Singleton
  abstract fun provideBitmapManipuilator(bitmapManipulator: RealBitmapManipulator): BitmapManipulator
}
