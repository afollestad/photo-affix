package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.afollestad.photoaffix.R;

/** @author Aidan Follestad (afollestad) */
public class SquareFrameLayout extends FrameLayout {

  private Paint edgePaint;
  private int borderRadius;

  public SquareFrameLayout(Context context) {
    super(context);
    init();
  }

  public SquareFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public SquareFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    borderRadius = (int) getResources().getDimension(R.dimen.default_square_border_radius);
    edgePaint = new Paint();
    edgePaint.setAntiAlias(true);
    edgePaint.setStyle(Paint.Style.STROKE);
    edgePaint.setColor(ContextCompat.getColor(getContext(), R.color.browseButtonBorder));
    edgePaint.setStrokeWidth(borderRadius);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    //noinspection SuspiciousNameCombination
    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int left = borderRadius;
    int top = borderRadius;
    int bottom = getMeasuredHeight() - borderRadius;
    int right = getMeasuredWidth() - borderRadius;
    canvas.drawRect(left, top, right, bottom, edgePaint);
  }
}
