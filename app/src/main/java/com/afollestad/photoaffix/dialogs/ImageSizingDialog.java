package com.afollestad.photoaffix.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.photoaffix.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ImageSizingDialog extends DialogFragment {

    public ImageSizingDialog() {
    }

    public interface SizingCallback {
        void onSizingResult(double scale, int resultWidth, int resultHeight);
    }

    private SizingCallback mCallback;

    @Bind(R.id.inputWidth)
    EditText inputWidth;
    @Bind(R.id.inputHeight)
    EditText inputHeight;
    @Bind(R.id.inputScaleSeek)
    SeekBar inputScaleSeek;
    @Bind(R.id.inputScaleLabel)
    TextView inputScaleLabel;

    private double round(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    public static void show(AppCompatActivity context, int width, int height) {
        ImageSizingDialog dialog = new ImageSizingDialog();
        Bundle args = new Bundle();
        args.putInt("width", width);
        args.putInt("height", height);
        dialog.setArguments(args);
        dialog.show(context.getFragmentManager(), "[IMAGE_SIZING_DIALOG]");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.output_size)
                .customView(R.layout.dialog_imagesizing, false)
                .positiveText(R.string.done)
                .neutralText(R.string.defaultStr)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        if (mCallback != null) {
                            final double scale = round((double) inputScaleSeek.getProgress() / 1000d);
                            mCallback.onSizingResult(scale,
                                    Integer.parseInt(inputWidth.getText().toString().trim()),
                                    Integer.parseInt(inputHeight.getText().toString().trim()));
                        }
                        materialDialog.dismiss();
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        inputScaleSeek.setProgress(inputScaleSeek.getMax());
                    }
                })
                .build();

        final View v = dialog.getCustomView();
        assert v != null;
        ButterKnife.bind(this, v);

        inputScaleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final double scale = round((double) progress / 1000d);
                String scaleStr;
                if (scale == 0) scaleStr = "0.00";
                else if (scale == 1) scaleStr = "1.00";
                else {
                    scaleStr = String.format("%s", scale);
                    if (scaleStr.length() == 3) scaleStr += "0";
                }
                inputScaleLabel.setText(scaleStr);
                final int originalWidth = getArguments().getInt("width");
                final int originalHeight = getArguments().getInt("height");
                inputWidth.setText(String.format("%d", (int) (originalWidth * scale)));
                inputHeight.setText(String.format("%d", (int) (originalHeight * scale)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        inputScaleSeek.setProgress(inputScaleSeek.getMax());

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SizingCallback)
            mCallback = (SizingCallback) activity;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}