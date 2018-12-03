/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.photoaffix.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.photoaffix.prefs.BgFillColor
import com.afollestad.photoaffix.prefs.ImageSpacingHorizontal
import com.afollestad.photoaffix.prefs.ImageSpacingVertical
import com.afollestad.photoaffix.utilities.Injector
import com.afollestad.photoaffix.utilities.ext.colorAttr
import com.afollestad.photoaffix.utilities.ext.onProgressChanged
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

    fun show(context: AppCompatActivity) =
      ImageSpacingDialog().show(context.supportFragmentManager, TAG)
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

    (myActivity.applicationContext as Injector).injectInto(this)

    var fillColor = bgFillColorPref.get()
    if (fillColor == TRANSPARENT) {
      fillColor = myActivity.colorAttr(R.attr.colorAccent)
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
