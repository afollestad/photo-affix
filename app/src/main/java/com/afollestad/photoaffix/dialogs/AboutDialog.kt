/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.photoaffix.R

/** @author Aidan Follestad (afollestad) */
class AboutDialog : DialogFragment() {

  companion object {
    private const val TAG = "[ABOUT_DIALOG]"

    fun show(context: AppCompatActivity) {
      val dialog = AboutDialog()
      dialog.show(context.supportFragmentManager, TAG)
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return MaterialDialog(activity!!)
        .title(R.string.about)
        .message(
            res = R.string.about_body,
            html = true,
            lineHeightMultiplier = 1.6f
        )
        .positiveButton(R.string.dismiss)
  }
}
