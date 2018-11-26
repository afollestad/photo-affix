/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat.getColor
import com.afollestad.photoaffix.R

/** @author Aidan Follestad (afollestad) */
class SquareImageView(
  context: Context,
  attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

  private val edgePaint: Paint
  private val borderRadius: Int = context.resources.getDimension(R.dimen.circle_border_radius)
      .toInt()

  init {
    val accentColor = getColor(context, R.color.colorAccent)
    edgePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      color = accentColor
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
    if (isActivated) {
      val left = borderRadius
      val top = borderRadius
      val bottom = measuredHeight - borderRadius
      val right = measuredWidth - borderRadius
      canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), edgePaint)
    }
  }
}
