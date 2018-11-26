/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.graphics.Paint
import android.graphics.Paint.Style.STROKE
import androidx.annotation.ColorInt
import android.util.AttributeSet
import android.view.View
import com.afollestad.photoaffix.R

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
