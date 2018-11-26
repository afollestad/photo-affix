/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.viewcomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.afollestad.photoaffix.utilities.colorAttr

/** @author Aidan Follestad (afollestad) */
class CircleView(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private var circleRadius: Int = context.resources.getDimension(R.dimen.circle_border_radius)
      .toInt()
  private val edgePaint: Paint
  private val fillPaint: Paint

  init {
    setWillNotDraw(false)
    val accentColor = context.colorAttr(R.attr.colorAccent)
    fillPaint = Paint().apply {
      isAntiAlias = true
      color = accentColor
    }
    edgePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      color = accentColor
      strokeWidth = circleRadius.toFloat()
    }
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
    if (isActivated) {
      canvas.drawCircle(center.toFloat(), center.toFloat(), radius.toFloat(), fillPaint)
    }
    canvas.drawCircle(center.toFloat(), center.toFloat(), radius.toFloat(), edgePaint)
  }
}
