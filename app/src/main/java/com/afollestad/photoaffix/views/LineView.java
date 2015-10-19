package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * @author Aidan Follestad (afollestad)
 */
public class LineView extends View {

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

    public LineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private Paint mPaint;

    private void init() {
        setWillNotDraw(false);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (isInEditMode())
            setWidth(8);
    }

    public void setWidth(int width) {
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, getResources().getDisplayMetrics());
        mPaint.setStrokeWidth(width);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getTag() != null && getTag().equals("1")) {
            canvas.drawLine(0,
                    (getMeasuredHeight() / 2),
                    getMeasuredWidth(),
                    (getMeasuredHeight() / 2),
                    mPaint);
        } else {
            canvas.drawLine((getMeasuredWidth() / 2),
                    0,
                    (getMeasuredWidth() / 2),
                    getMeasuredHeight(), mPaint);
        }
    }
}
