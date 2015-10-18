package com.afollestad.photoaffix.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.ui.MainActivity;
import com.afollestad.photoaffix.utils.Prefs;
import com.afollestad.photoaffix.views.PaddingVisualizerView;

import butterknife.ButterKnife;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ImagePaddingDialog extends DialogFragment {

    public ImagePaddingDialog() {
    }

    private MainActivity mContext;
    private EditText mPaddingLeft;
    private EditText mPaddingTop;
    private EditText mPaddingRight;
    private EditText mPaddingBottom;
    private PaddingVisualizerView mVisualizer;

    public interface PaddingCallback {
        void onPaddingChanged(int left, int top, int right, int bottom);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mVisualizer != null)
            mVisualizer.saveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.image_padding)
                .customView(R.layout.dialog_imagepadding, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.defaults)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        notifyActivity();
                        materialDialog.dismiss();
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        mPaddingLeft.setText("");
                        mPaddingTop.setText("");
                        mPaddingRight.setText("");
                        mPaddingBottom.setText("");
                    }
                })
                .build();

        final View v = dialog.getCustomView();
        assert v != null;

        mPaddingLeft = ButterKnife.findById(v, R.id.paddingLeft);
        mPaddingTop = ButterKnife.findById(v, R.id.paddingTop);
        mPaddingRight = ButterKnife.findById(v, R.id.paddingRight);
        mPaddingBottom = ButterKnife.findById(v, R.id.paddingBottom);

        mVisualizer = ButterKnife.findById(v, R.id.visualizer);
        if (!mVisualizer.restoreInstanceState(savedInstanceState)) {
            final int[] values = Prefs.imagePadding(getActivity());
            mVisualizer.setValues(values[0], values[1], values[2], values[3]);
            mPaddingLeft.setText(values[0] != 0 ? String.format("%d", values[0]) : "");
            mPaddingTop.setText(values[1] != 0 ? String.format("%d", values[1]) : "");
            mPaddingRight.setText(values[2] != 0 ? String.format("%d", values[2]) : "");
            mPaddingBottom.setText(values[3] != 0 ? String.format("%d", values[3]) : "");
        }

        TextWatcher mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notifyVisualizer();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        mPaddingLeft.addTextChangedListener(mTextWatcher);
        mPaddingTop.addTextChangedListener(mTextWatcher);
        mPaddingRight.addTextChangedListener(mTextWatcher);
        mPaddingBottom.addTextChangedListener(mTextWatcher);

        final TextView.OnEditorActionListener actionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == KeyEvent.KEYCODE_ENTER) {
                    switch (v.getId()) {
                        case R.id.paddingLeft:
                            focus(mPaddingTop);
                            return true;
                        case R.id.paddingTop:
                            focus(mPaddingRight);
                            return true;
                        case R.id.paddingRight:
                            focus(mPaddingBottom);
                            return true;
                        case R.id.paddingBottom:
                            focus(mPaddingLeft);
                            return true;
                    }
                }
                return false;
            }
        };

        mPaddingLeft.setImeActionLabel(getString(R.string.next), KeyEvent.KEYCODE_ENTER);
        mPaddingLeft.setOnEditorActionListener(actionListener);
        mPaddingTop.setImeActionLabel(getString(R.string.next), KeyEvent.KEYCODE_ENTER);
        mPaddingTop.setOnEditorActionListener(actionListener);
        mPaddingRight.setImeActionLabel(getString(R.string.next), KeyEvent.KEYCODE_ENTER);
        mPaddingRight.setOnEditorActionListener(actionListener);
        mPaddingBottom.setImeActionLabel(getString(R.string.next), KeyEvent.KEYCODE_ENTER);
        mPaddingBottom.setOnEditorActionListener(actionListener);

        return dialog;
    }

    private void focus(final EditText view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                view.requestFocus();
            }
        });
    }

    @Size(4)
    private int[] getInputValues() {
        final String leftStr = mPaddingLeft.getText().toString();
        int left;
        try {
            left = Integer.parseInt(leftStr);
        } catch (NumberFormatException e) {
            left = 0;
        }
        final String topStr = mPaddingTop.getText().toString();
        int top;
        try {
            top = Integer.parseInt(topStr);
        } catch (NumberFormatException e) {
            top = 0;
        }
        final String rightStr = mPaddingRight.getText().toString();
        int right;
        try {
            right = Integer.parseInt(rightStr);
        } catch (NumberFormatException e) {
            right = 0;
        }
        final String bottomStr = mPaddingBottom.getText().toString();
        int bottom;
        try {
            bottom = Integer.parseInt(bottomStr);
        } catch (NumberFormatException e) {
            bottom = 0;
        }
        return new int[]{left, top, right, bottom};
    }

    private void notifyActivity() {
        if (mContext == null) return;
        final int[] values = getInputValues();
        mContext.onPaddingChanged(values[0], values[1], values[2], values[3]);
    }

    private void notifyVisualizer() {
        if (mContext == null) return;
        final int[] values = getInputValues();
        mVisualizer.setValues(values[0], values[1], values[2], values[3]);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = (MainActivity) activity;
    }
}
