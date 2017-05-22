package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.afollestad.photoaffix.R;

/** @author Aidan Follestad (afollestad) */
public class CircleView extends View {

  private int circleRadius;
  private Paint edgePaint;
  private Paint fillPaint;

  public CircleView(Context context) {
    super(context);
    init();
  }

  public CircleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setWillNotDraw(false);
    int mAccentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
    circleRadius = (int) getResources().getDimension(R.dimen.circle_border_radius);

    fillPaint = new Paint();
    fillPaint.setAntiAlias(true);
    fillPaint.setColor(mAccentColor);

    edgePaint = new Paint();
    edgePaint.setAntiAlias(true);
    edgePaint.setStyle(Paint.Style.STROKE);
    edgePaint.setColor(mAccentColor);
    edgePaint.setStrokeWidth(circleRadius);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    final int center = (getMeasuredWidth() / 2);
    final int radius = (getMeasuredWidth() / 2) - circleRadius;
    if (isActivated()) canvas.drawCircle(center, center, radius, fillPaint);
    canvas.drawCircle(center, center, radius, edgePaint);
  }
}
