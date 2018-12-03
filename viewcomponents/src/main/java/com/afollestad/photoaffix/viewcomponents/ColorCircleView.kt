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
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.graphics.Paint
import android.graphics.Paint.Style.STROKE
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/** @author Aidan Follestad (afollestad) */
class ColorCircleView(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val circleRadius: Int = context.resources.getDimension(R.dimen.circle_border_radius)
      .toInt()
  private val edgePaint: Paint
  private val fillPaint: Paint

  init {
    setWillNotDraw(false)
    fillPaint = Paint().apply {
      isAntiAlias = true
      color = BLACK
    }
    edgePaint = Paint().apply {
      isAntiAlias = true
      style = STROKE
      color = WHITE
      strokeWidth = circleRadius.toFloat()
    }
  }

  fun setColor(@ColorInt color: Int) {
    fillPaint.color = color
    invalidate()
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(measuredWidth, measuredWidth)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val center = measuredWidth / 2
    val radius = measuredWidth / 2 - circleRadius
    canvas.drawCircle(center.toFloat(), center.toFloat(), radius.toFloat(), fillPaint)
    canvas.drawCircle(center.toFloat(), center.toFloat(), radius.toFloat(), edgePaint)
  }
}
