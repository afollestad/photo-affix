/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style.STROKE
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getColor
import com.afollestad.photoaffix.R

/** @author Aidan Follestad (afollestad)
 */
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
