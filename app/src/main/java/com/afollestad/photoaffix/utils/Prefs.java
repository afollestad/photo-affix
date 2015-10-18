package com.afollestad.photoaffix.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.Size;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Prefs {

    private Prefs() {
    }

    public static boolean stackHorizontally(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("stack_horizontally", true);
    }

    public static void stackHorizontally(Context context, boolean newValue) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("stack_horizontally", newValue).commit();
    }

    @ColorInt
    public static int bgFillColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("bg_fill_color", Color.parseColor("#212121"));
    }

    public static void bgFillColor(Context context, @ColorInt int newValue) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt("bg_fill_color", newValue).commit();
    }

    @Size(4)
    public static int[] imagePadding(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return new int[]{
                prefs.getInt("image_padding_left", 0),
                prefs.getInt("image_padding_top", 0),
                prefs.getInt("image_padding_right", 0),
                prefs.getInt("image_padding_bottom", 0)
        };
    }

    public static void imagePadding(Context context, int left, int top, int right, int bottom) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt("image_padding_left", left)
                .putInt("image_padding_top", top)
                .putInt("image_padding_right", right)
                .putInt("image_padding_bottom", bottom)
                .commit();
    }
}
