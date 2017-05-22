package com.afollestad.photoaffix.animation;

import android.animation.IntEvaluator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/** @author Aidan Follestad (afollestad) */
public class HeightEvaluator extends IntEvaluator {

  private final View v;

  public HeightEvaluator(View v) {
    this.v = v;
  }

  @NonNull
  @Override
  public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
    int num = super.evaluate(fraction, startValue, endValue);
    ViewGroup.LayoutParams params = v.getLayoutParams();
    params.height = num;
    v.setLayoutParams(params);
    return num;
  }
}
