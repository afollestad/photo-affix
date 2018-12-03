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
package com.afollestad.photoaffix.animation

import android.animation.IntEvaluator
import android.view.View

/** @author Aidan Follestad (afollestad) */
class HeightEvaluator(private val v: View) : IntEvaluator() {

  override fun evaluate(
    fraction: Float,
    startValue: Int?,
    endValue: Int?
  ): Int {
    val num = super.evaluate(fraction, startValue, endValue)!!
    val params = v.layoutParams
    params.height = num
    v.layoutParams = params
    return num
  }
}
