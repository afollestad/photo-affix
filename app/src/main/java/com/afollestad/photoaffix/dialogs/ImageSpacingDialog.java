package com.afollestad.photoaffix.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.ui.MainActivity;
import com.afollestad.photoaffix.utils.Prefs;
import com.afollestad.photoaffix.views.LineView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ImageSpacingDialog extends DialogFragment {

    public ImageSpacingDialog() {
    }

    private MainActivity mContext;
    @Bind(R.id.spacingHorizontalSeek)
    SeekBar mHorizontalSeek;
    @Bind(R.id.spacingHorizontalLabel)
    TextView mHorizontalLabel;
    @Bind(R.id.spacingVerticalSeek)
    SeekBar mVerticalSeek;
    @Bind(R.id.spacingVerticalLabel)
    TextView mVerticalLabel;
    @Bind(R.id.verticalLine)
    LineView mVerticalLine;
    @Bind(R.id.horizontalLine)
    LineView mHorizontalLine;

    public interface SpacingCallback {
        void onSpacingChanged(int horizontal, int vertical);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int fillColor = Prefs.bgFillColor(getActivity());
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.image_spacing)
                .customView(R.layout.dialog_imagespacing, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .positiveColor(fillColor)
                .negativeColor(fillColor)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        notifyActivity();
                        materialDialog.dismiss();
                    }
                })
                .build();

        final View v = dialog.getCustomView();
        assert v != null;
        ButterKnife.bind(this, v);

        SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getId() == R.id.spacingHorizontalSeek) {
                    mHorizontalLabel.setText(String.format("%d", progress));
                    mHorizontalLine.setWidth(progress);
                } else {
                    mVerticalLabel.setText(String.format("%d", progress));
                    mVerticalLine.setWidth(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        mHorizontalSeek.setOnSeekBarChangeListener(seekListener);
        mVerticalSeek.setOnSeekBarChangeListener(seekListener);

        MDTintHelper.setTint(mHorizontalSeek, fillColor);
        MDTintHelper.setTint(mVerticalSeek, fillColor);

        final int[] spacing = Prefs.imageSpacing(getActivity());
        mHorizontalSeek.setProgress(spacing[0]);
        mVerticalSeek.setProgress(spacing[1]);

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void notifyActivity() {
        if (mContext == null) return;
        mContext.onSpacingChanged(mHorizontalSeek.getProgress(), mVerticalSeek.getProgress());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = (MainActivity) activity;
    }
}
