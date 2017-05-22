package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/** @author Aidan Follestad (afollestad) */
public class LineView extends View {

  private Paint paint;

  public LineView(Context context) {
    super(context);
    init();
  }

  public LineView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setWillNotDraw(false);
    paint = new Paint();
    paint.setAntiAlias(true);
    paint.setColor(Color.BLACK);
    paint.setStyle(Paint.Style.FILL_AND_STROKE);
    if (isInEditMode()) setWidth(8);
  }

  public void setWidth(int width) {
    width =
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, width, getResources().getDisplayMetrics());
    paint.setStrokeWidth(width);
    invalidate();
  }

  public void setColor(@ColorInt int color) {
    paint.setColor(color);
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (getTag() != null && getTag().equals("1")) {
      canvas.drawLine(
          0, (getMeasuredHeight() / 2), getMeasuredWidth(), (getMeasuredHeight() / 2), paint);
    } else {
      canvas.drawLine(
          (getMeasuredWidth() / 2), 0, (getMeasuredWidth() / 2), getMeasuredHeight(), paint);
    }
  }
}
