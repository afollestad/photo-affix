package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.View;

import com.afollestad.photoaffix.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ColorCircleView extends View {

    private int mCircleRadius;
    private Paint mEdgePaint;
    private Paint mFillPaint;

    public ColorCircleView(Context context) {
        super(context);
        init();
    }

    public ColorCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ColorCircleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mCircleRadius = (int) getResources().getDimension(R.dimen.border_radius);

        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(Color.BLACK);

        mEdgePaint = new Paint();
        mEdgePaint.setAntiAlias(true);
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setColor(Color.WHITE);
        mEdgePaint.setStrokeWidth(mCircleRadius);
    }

    public void setColor(@ColorInt int color) {
        mFillPaint.setColor(color);
        invalidate();
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
        final int radius = (getMeasuredWidth() / 2) - mCircleRadius;
        canvas.drawCircle(center, center, radius, mFillPaint);
        canvas.drawCircle(center, center, radius, mEdgePaint);
    }
}