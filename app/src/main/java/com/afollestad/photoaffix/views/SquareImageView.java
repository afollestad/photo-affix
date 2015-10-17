package com.afollestad.photoaffix.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.afollestad.photoaffix.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SquareImageView extends ImageView {

    private Paint mEdgePaint;
    private int mBorderRadius;

    public SquareImageView(Context context) {
        super(context);
        init();
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        int mAccentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        mBorderRadius = (int) getResources().getDimension(R.dimen.border_radius);
        mEdgePaint = new Paint();
        mEdgePaint.setAntiAlias(true);
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setColor(mAccentColor);
        mEdgePaint.setStrokeWidth(mBorderRadius);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isActivated()) {
            int left = mBorderRadius;
            int top = mBorderRadius;
            int bottom = getMeasuredHeight() - mBorderRadius;
            int right = getMeasuredWidth() - mBorderRadius;
            canvas.drawRect(left, top, right, bottom, mEdgePaint);
        }
    }
}
