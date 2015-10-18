package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.afollestad.photoaffix.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class CircleView extends View {

    private int mCircleRadius;
    private Paint mEdgePaint;
    private Paint mFillPaint;

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

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        int mAccentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        mCircleRadius = (int) getResources().getDimension(R.dimen.circle_border_radius);

        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(mAccentColor);

        mEdgePaint = new Paint();
        mEdgePaint.setAntiAlias(true);
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setColor(mAccentColor);
        mEdgePaint.setStrokeWidth(mCircleRadius);
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
        if (isActivated()) canvas.drawCircle(center, center, radius, mFillPaint);
        canvas.drawCircle(center, center, radius, mEdgePaint);
    }
}