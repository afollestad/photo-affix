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
import android.graphics.Paint
import android.graphics.Paint.Style.STROKE
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getColor

/** @author Aidan Follestad (afollestad) */
class SquareFrameLayout(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  private val edgePaint: Paint
  private val borderRadius: Int =
    context.resources.getDimension(R.dimen.default_square_border_radius)
        .toInt()

  init {
    edgePaint = Paint().apply {
      isAntiAlias = true
      style = STROKE
      color = getColor(context, R.color.browseButtonBorder)
      strokeWidth = borderRadius.toFloat()
    }
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    super.onMeasure(widthMeasureSpec, widthMeasureSpec)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val left = borderRadius
    val top = borderRadius
    val bottom = measuredHeight - borderRadius
    val right = measuredWidth - borderRadius
    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), edgePaint)
  }
}
