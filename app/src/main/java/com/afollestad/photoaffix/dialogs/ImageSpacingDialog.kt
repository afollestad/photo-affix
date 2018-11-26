/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.utils.Prefs
import com.afollestad.photoaffix.utils.onProgressChanged
import kotlinx.android.synthetic.main.dialog_imagespacing.view.horizontalLine
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingHorizontalLabel
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingHorizontalSeek
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingVerticalLabel
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingVerticalSeek
import kotlinx.android.synthetic.main.dialog_imagespacing.view.verticalLine

interface SpacingCallback {

  fun onSpacingChanged(
    horizontal: Int,
    vertical: Int
  )
}

/** @author Aidan Follestad (afollestad) */
class ImageSpacingDialog : DialogFragment() {

  companion object {
    private const val TAG = "[IMAGE_PADDING_DIALOG]"

    fun show(context: AppCompatActivity) {
      ImageSizingDialog().show(context.supportFragmentManager, TAG)
    }
  }

  private var context: SpacingCallback? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val myActivity = activity
    requireNotNull(myActivity)

    var fillColor = Prefs.bgFillColor(myActivity)
    if (fillColor == TRANSPARENT) {
      fillColor = getColor(myActivity, R.color.colorAccent)
    }

    val dialog = MaterialDialog(myActivity)
        .title(R.string.image_spacing)
        .customView(R.layout.dialog_imagespacing, scrollable = true)
        .positiveButton(android.R.string.ok) { notifyActivity() }
        .negativeButton(android.R.string.cancel) { it.dismiss() }

    val customView = dialog.getCustomView() ?: return dialog

    customView.spacingHorizontalSeek.onProgressChanged {
      customView.spacingHorizontalLabel.text = it.toString()
      customView.horizontalLine.width = it
    }
    customView.horizontalLine.setColor(fillColor)

    customView.spacingVerticalSeek.onProgressChanged {
      customView.spacingVerticalLabel.text = it.toString()
      customView.verticalLine.width = it
    }
    customView.verticalLine.setColor(fillColor)

    val spacing = Prefs.imageSpacing(activity)
    customView.spacingHorizontalSeek.progress = spacing[0]
    customView.spacingVerticalSeek.progress = spacing[1]

    return dialog
  }

  private fun notifyActivity() {
    val materialDialog = dialog as? MaterialDialog ?: return
    val customView = materialDialog.getCustomView() ?: return
    context?.onSpacingChanged(
        horizontal = customView.spacingHorizontalSeek.progress,
        vertical = customView.spacingVerticalSeek.progress
    )
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.context = context as? SpacingCallback
  }
}
