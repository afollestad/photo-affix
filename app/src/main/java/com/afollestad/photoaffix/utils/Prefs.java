package com.afollestad.photoaffix.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.Size;

/** @author Aidan Follestad (afollestad) */
@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
public class Prefs {

  private Prefs() {}

  public static boolean stackHorizontally(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean("stack_horizontally", true);
  }

  public static void stackHorizontally(Context context, boolean newValue) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean("stack_horizontally", newValue)
        .commit();
  }

  public static boolean scalePriority(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean("scale_priority", true);
  }

  public static void scalePriority(Context context, boolean scalePriority) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean("scale_priority", scalePriority)
        .commit();
  }

  @ColorInt
  public static int bgFillColor(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getInt("bg_fill_color", Color.TRANSPARENT);
  }

  public static void bgFillColor(Context context, @ColorInt int newValue) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putInt("bg_fill_color", newValue)
        .commit();
  }

  @Size(2)
  public static int[] imageSpacing(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return new int[] {
      prefs.getInt("image_spacing_horizontal", 0), prefs.getInt("image_spacing_vertical", 0),
    };
  }

  public static void imageSpacing(Context context, int horizontal, int vertical) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putInt("image_spacing_horizontal", horizontal)
        .putInt("image_spacing_vertical", vertical)
        .commit();
  }
}
