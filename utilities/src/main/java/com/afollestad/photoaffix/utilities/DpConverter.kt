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
package com.afollestad.photoaffix.utilities

import android.app.Application
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import androidx.annotation.Px
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
interface DpConverter {

  fun toDp(@Px pixels: Int): Float
}

class RealDpConverter @Inject constructor(
  private val app: Application
) : DpConverter {

  override fun toDp(pixels: Int) = applyDimension(
      COMPLEX_UNIT_DIP,
      pixels.toFloat(),
      app.resources.displayMetrics
  )
}
