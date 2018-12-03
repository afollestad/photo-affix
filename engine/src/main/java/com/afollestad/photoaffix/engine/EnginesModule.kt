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
