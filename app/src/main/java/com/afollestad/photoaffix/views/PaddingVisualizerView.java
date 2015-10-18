package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.afollestad.photoaffix.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class PaddingVisualizerView extends View {

    private Paint mEdgePaint;

    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;

    public void saveInstanceState(Bundle out) {
        out.putInt("visualizer_padding_left", mLeft);
        out.putInt("visualizer_padding_top", mTop);
        out.putInt("visualizer_padding_right", mRight);
        out.putInt("visualizer_padding_bottom", mBottom);
    }

    public boolean restoreInstanceState(Bundle in) {
        if (in != null && in.containsKey("visualizer_padding_left")) {
            mLeft = in.getInt("visualizer_padding_left", 0);
            mTop = in.getInt("visualizer_padding_top", 0);
            mRight = in.getInt("visualizer_padding_right", 0);
            mBottom = in.getInt("visualizer_padding_bottom", 0);
            return true;
        }
        return false;
    }

    public PaddingVisualizerView(Context context) {
        super(context);
        init();
    }

    public PaddingVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaddingVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PaddingVisualizerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private int convert(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void setValues(int left, int top, int right, int bottom) {
        mLeft = convert(left);
        mTop = convert(top);
        mRight = convert(right);
        mBottom = convert(bottom);
        invalidate();
    }

    private void init() {
        setWillNotDraw(false);
        int mAccentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);

        mEdgePaint = new Paint();
        mEdgePaint.setAntiAlias(true);
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setColor(mAccentColor);
        mEdgePaint.setStrokeWidth(1);

        if (isInEditMode())
            setValues(3, 3, 3, 3);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Left side
        mEdgePaint.setStrokeWidth(mLeft);
        canvas.drawLine(mLeft / 2,
                0,
                mLeft / 2,
                getMeasuredHeight(),
                mEdgePaint);

        // Top side
        mEdgePaint.setStrokeWidth(mTop);
        canvas.drawLine(0,
                mTop / 2,
                getMeasuredWidth(),
                mTop / 2,
                mEdgePaint);

        // Right side
        mEdgePaint.setStrokeWidth(mRight);
        canvas.drawLine(getMeasuredWidth() - (mRight / 2),
                0,
                getMeasuredWidth() - (mRight / 2),
                getMeasuredHeight(),
                mEdgePaint);

        // Bottom side
        mEdgePaint.setStrokeWidth(mBottom);
        canvas.drawLine(0,
                getMeasuredHeight() - (mBottom / 2),
                getMeasuredWidth(),
                getMeasuredHeight() - (mBottom / 2),
                mEdgePaint);
    }
}