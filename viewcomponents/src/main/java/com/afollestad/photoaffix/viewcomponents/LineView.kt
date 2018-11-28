/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
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
