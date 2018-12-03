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
package com.afollestad.photoaffix.viewcomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.util.TypedValue.applyDimension
import android.view.View
import androidx.annotation.ColorInt

/** @author Aidan Follestad (afollestad) */
class LineView(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private var paint: Paint

  init {
    setWillNotDraw(false)
    paint = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      style = Paint.Style.FILL_AND_STROKE
    }
    if (isInEditMode) {
      width = 8
    }
  }

  fun setWidth(width: Int) {
    paint.strokeWidth = applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        width.toFloat(),
        resources.displayMetrics
    )
    invalidate()
  }

  fun setColor(@ColorInt color: Int) {
    paint.color = color
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (tag != null && tag == "1") {
      canvas.drawLine(
          0f,
          (measuredHeight / 2).toFloat(),
          measuredWidth.toFloat(),
          (measuredHeight / 2).toFloat(),
          paint
      )
    } else {
      canvas.drawLine(
          (measuredWidth / 2).toFloat(),
          0f,
          (measuredWidth / 2).toFloat(),
          measuredHeight.toFloat(),
          paint
      )
    }
  }
}
