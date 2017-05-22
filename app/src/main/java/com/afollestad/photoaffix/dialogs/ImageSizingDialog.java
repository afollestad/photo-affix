package com.afollestad.photoaffix.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.ui.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/** @author Aidan Follestad (afollestad) */
public class ImageSizingDialog extends DialogFragment {

  @BindView(R.id.inputWidth)
  TextView inputWidth;
  @BindView(R.id.inputHeight)
  TextView inputHeight;
  @BindView(R.id.inputScaleSeek)
  SeekBar inputScaleSeek;
  @BindView(R.id.inputScaleLabel)
  TextView inputScaleLabel;
  @BindView(R.id.formatSpinner)
  Spinner formatSpinner;
  @BindView(R.id.qualityTitle)
  TextView qualityTitle;
  @BindView(R.id.qualitySeeker)
  SeekBar qualitySeeker;
  @BindView(R.id.qualityLabel)
  TextView qualityLabel;
  private SizingCallback callback;
  private int minimum;
  private Unbinder unbinder;

  public ImageSizingDialog() {}

  public static void show(AppCompatActivity context, int width, int height) {
    ImageSizingDialog dialog = new ImageSizingDialog();
    Bundle args = new Bundle();
    args.putInt("width", width);
    args.putInt("height", height);
    dialog.setArguments(args);
    dialog.show(context.getFragmentManager(), "[IMAGE_SIZING_DIALOG]");
  }

  private double round(double x) {
    return Math.round(x * 100.0) / 100.0;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MaterialDialog dialog =
        new MaterialDialog.Builder(getActivity())
            .title(R.string.output_settings)
            .customView(R.layout.dialog_imagesizing, false)
            .positiveText(R.string.continueStr)
            .negativeText(android.R.string.cancel)
            .neutralText(R.string.defaultStr)
            .autoDismiss(false)
            .onPositive(
                (materialDialog, dialogAction) -> {
                  if (callback != null) {
                    final int progress = inputScaleSeek.getProgress() + minimum;
                    final double scale = round((double) progress / 1000d);
                    callback.onSizingResult(
                        scale,
                        Integer.parseInt(inputWidth.getText().toString().trim()),
                        Integer.parseInt(inputHeight.getText().toString().trim()),
                        formatSpinner.getSelectedItemPosition() == 0
                            ? Bitmap.CompressFormat.PNG
                            : Bitmap.CompressFormat.JPEG,
                        qualitySeeker.getProgress() + 1,
                        false);
                  }
                  MainActivity.dismissDialog(materialDialog);
                })
            .onNegative(
                (materialDialog, dialogAction) -> {
                  callback.onSizingResult(0.0f, -1, -1, Bitmap.CompressFormat.PNG, 100, true);
                  MainActivity.dismissDialog(materialDialog);
                })
            .onNeutral(
                (materialDialog, dialogAction) ->
                    inputScaleSeek.setProgress(inputScaleSeek.getMax()))
            .build();

    final View v = dialog.getCustomView();
    assert v != null;
    unbinder = ButterKnife.bind(this, v);

    minimum = (int) (inputScaleSeek.getMax() * 0.1d);
    inputScaleSeek.setMax((int) (inputScaleSeek.getMax() * 0.9d));
    inputScaleSeek.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            progress += minimum;
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
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    inputScaleSeek.setProgress(inputScaleSeek.getMax());

    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(getActivity(), R.layout.spinner_item, new String[] {"PNG", "JPEG"});
    adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
    formatSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final int visibility = position == 1 ? View.VISIBLE : View.GONE;
            qualityTitle.setVisibility(visibility);
            qualitySeeker.setVisibility(visibility);
            qualityLabel.setVisibility(visibility);
            qualitySeeker.setProgress(99);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    formatSpinner.setAdapter(adapter);

    qualitySeeker.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            qualityLabel.setText(String.format("%d", progress + 1));
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    qualitySeeker.setMax(99);
    qualitySeeker.setProgress(99);

    return dialog;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof SizingCallback) {
      callback = (SizingCallback) activity;
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
    unbinder = null;
  }

  public interface SizingCallback {
    void onSizingResult(
        double scale,
        int resultWidth,
        int resultHeight,
        Bitmap.CompressFormat format,
        int quality,
        boolean cancelled);
  }
}
