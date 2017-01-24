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

    private Paint edgePaint;
    private int borderRadius;

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

    private void init() {
        int mAccentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        borderRadius = (int) getResources().getDimension(R.dimen.circle_border_radius);
        edgePaint = new Paint();
        edgePaint.setAntiAlias(true);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setColor(mAccentColor);
        edgePaint.setStrokeWidth(borderRadius);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isActivated()) {
            int left = borderRadius;
            int top = borderRadius;
            int bottom = getMeasuredHeight() - borderRadius;
            int right = getMeasuredWidth() - borderRadius;
            canvas.drawRect(left, top, right, bottom, edgePaint);
        }
    }
}
