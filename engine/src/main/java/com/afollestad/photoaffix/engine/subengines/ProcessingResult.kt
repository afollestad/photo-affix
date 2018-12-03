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
package com.afollestad.photoaffix.engine.subengines

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.CompressFormat.PNG

/** @author Aidan Follestad (afollestad) */
data class ProcessingResult(
  val processedCount: Int,
  val output: Bitmap? = null,
  val format: CompressFormat = PNG,
  val quality: Int = 100,
  val error: Exception? = null
) {

  fun none() = processedCount == 0

  fun recycle() = output?.recycle()

  fun isError() = error != null
}
