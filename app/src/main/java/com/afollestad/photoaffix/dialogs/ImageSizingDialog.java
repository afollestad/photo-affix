package com.afollestad.photoaffix.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.ui.MainActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ImageSizingDialog extends DialogFragment {

    public ImageSizingDialog() {
    }

    public interface SizingCallback {
        void onSizingResult(double scale, int resultWidth, int resultHeight, Bitmap.CompressFormat format, int quality, boolean cancelled);
    }

    private SizingCallback mCallback;
    private int mMinimum;

    @Bind(R.id.inputWidth)
    TextView inputWidth;
    @Bind(R.id.inputHeight)
    TextView inputHeight;
    @Bind(R.id.inputScaleSeek)
    SeekBar inputScaleSeek;
    @Bind(R.id.inputScaleLabel)
    TextView inputScaleLabel;
    @Bind(R.id.formatSpinner)
    Spinner mFormatSpinner;
    @Bind(R.id.qualityTitle)
    TextView mQualityTitle;
    @Bind(R.id.qualitySeeker)
    SeekBar mQualitySeeker;
    @Bind(R.id.qualityLabel)
    TextView mQualityLabel;

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
                .title(R.string.output_settings)
                .customView(R.layout.dialog_imagesizing, false)
                .positiveText(R.string.continueStr)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.defaultStr)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        if (mCallback != null) {
                            final int progress = inputScaleSeek.getProgress() + mMinimum;
                            final double scale = round((double) progress / 1000d);
                            mCallback.onSizingResult(scale,
                                    Integer.parseInt(inputWidth.getText().toString().trim()),
                                    Integer.parseInt(inputHeight.getText().toString().trim()),
                                    mFormatSpinner.getSelectedItemPosition() == 0 ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                                    mQualitySeeker.getProgress(),
                                    false);
                        }
                        MainActivity.dismissDialog(materialDialog);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        mCallback.onSizingResult(0.0f, -1, -1, Bitmap.CompressFormat.PNG, 100, true);
                        MainActivity.dismissDialog(materialDialog);
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

        mMinimum = (int) (inputScaleSeek.getMax() * 0.1d);
        inputScaleSeek.setMax((int) (inputScaleSeek.getMax() * 0.9d));
        inputScaleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress += mMinimum;
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_item,
                new String[]{"PNG", "JPEG"});
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        mFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final int visibility = position == 1 ? View.VISIBLE : View.GONE;
                mQualityTitle.setVisibility(visibility);
                mQualitySeeker.setVisibility(visibility);
                mQualityLabel.setVisibility(visibility);
                mQualitySeeker.setProgress(100);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mFormatSpinner.setAdapter(adapter);

        mQualitySeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mQualityLabel.setText(String.format("%d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mQualitySeeker.setMax(100);
        mQualitySeeker.setProgress(100);

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