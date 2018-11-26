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
import com.afollestad.photoaffix.App
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.di.BgFillColor
import com.afollestad.photoaffix.di.ImageSpacingHorizontal
import com.afollestad.photoaffix.di.ImageSpacingVertical
import com.afollestad.photoaffix.utils.onProgressChanged
import com.afollestad.rxkprefs.Pref
import kotlinx.android.synthetic.main.dialog_imagespacing.view.horizontalLine
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingHorizontalLabel
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingHorizontalSeek
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingVerticalLabel
import kotlinx.android.synthetic.main.dialog_imagespacing.view.spacingVerticalSeek
import kotlinx.android.synthetic.main.dialog_imagespacing.view.verticalLine
import javax.inject.Inject

interface SpacingCallback {

  fun onSpacingChanged(
    horizontal: Int,
    vertical: Int
  )
}

/** @author Aidan Follestad (afollestad) */
class ImageSpacingDialog : DialogFragment() {

  companion object {
    private const val TAG = "[IMAGE_SIZING_DIALOG]"

    fun show(context: AppCompatActivity) {
      ImageSpacingDialog().show(context.supportFragmentManager, TAG)
    }
  }

  private var context: SpacingCallback? = null

  @Inject
  @field:BgFillColor
  lateinit var bgFillColorPref: Pref<Int>
  @Inject
  @field:ImageSpacingVertical
  lateinit var verticalSpacingPref: Pref<Int>
  @Inject
  @field:ImageSpacingHorizontal
  lateinit var horizontalSpacingPref: Pref<Int>

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val myActivity = activity
    requireNotNull(myActivity)

    (myActivity.applicationContext as App)
        .appComponent
        .inject(this)

    var fillColor = bgFillColorPref.get()
    if (fillColor == TRANSPARENT) {
      fillColor = getColor(myActivity, R.color.colorAccent)
    }

    val dialog = MaterialDialog(myActivity)
        .title(R.string.image_spacing)
        .customView(R.layout.dialog_imagespacing, scrollable = true)
        .positiveButton(android.R.string.ok) { notifyActivity() }
        .negativeButton(android.R.string.cancel) { dismiss() }

    val customView = dialog.getCustomView() ?: return dialog

    customView.spacingHorizontalSeek.max = 100
    customView.spacingHorizontalSeek.onProgressChanged {
      customView.spacingHorizontalLabel.text = it.toString()
      customView.horizontalLine.width = it
    }
    customView.horizontalLine.setColor(fillColor)

    customView.spacingVerticalSeek.max = 100
    customView.spacingVerticalSeek.onProgressChanged {
      customView.spacingVerticalLabel.text = it.toString()
      customView.verticalLine.width = it
    }
    customView.verticalLine.setColor(fillColor)

    customView.spacingHorizontalSeek.progress = horizontalSpacingPref.get()
    customView.spacingVerticalSeek.progress = verticalSpacingPref.get()

    return dialog
  }

  private fun notifyActivity() {
    val materialDialog = dialog as? MaterialDialog ?: return
    val customView = materialDialog.getCustomView() ?: return

    val horizontal = customView.spacingHorizontalSeek.progress
    horizontalSpacingPref.set(horizontal)
    val vertical = customView.spacingVerticalSeek.progress
    verticalSpacingPref.set(vertical)

    context?.onSpacingChanged(
        horizontal = horizontal,
        vertical = vertical
    )
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.context = context as? SpacingCallback
  }
}
