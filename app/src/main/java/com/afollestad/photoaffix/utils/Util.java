package com.afollestad.photoaffix.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.photoaffix.R;

import java.io.Closeable;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Util {

    private Util() {
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public static File makeTempFile(Context mContext, String extension) {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File parent = new File(Environment.getExternalStorageDirectory(), mContext.getString(R.string.app_name));
        parent.mkdirs();
        return new File(parent, "AFFIX_" + timeStamp + extension);
    }

    public static void closeQuietely(Closeable c) {
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    public static void showError(final Activity context, final Exception e) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showError(context, e);
                }
            });
            return;
        }
        new MaterialDialog.Builder(context)
                .title(R.string.error)
                .content(e.getMessage())
                .positiveText(android.R.string.ok)
                .show();
    }

    public static void lockOrientation(Activity context) {
        int orientation = context.getRequestedOrientation();
        int rotation = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            default:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        context.setRequestedOrientation(orientation);
    }

    public static void unlockOrientation(Activity context) {
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static void log(String message, Object... formatArgs) {
        if (formatArgs != null && formatArgs.length > 0) {
            Log.d("PHOTO_AFFIX", String.format(message, formatArgs));
        } else {
            Log.d("PHOTO_AFFIX", message);
        }
    }
}