/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.activities.MainActivity
import com.afollestad.photoaffix.constants.PRIMARY_COLORS
import com.afollestad.photoaffix.constants.PRIMARY_COLORS_SUB
import com.afollestad.photoaffix.dialogs.ImageSpacingDialog
import com.afollestad.photoaffix.utils.Prefs
import kotlinx.android.synthetic.main.settings_layout.view.bgFillColorCircle
import kotlinx.android.synthetic.main.settings_layout.view.bgFillColorLabel
import kotlinx.android.synthetic.main.settings_layout.view.imagePaddingLabel
import kotlinx.android.synthetic.main.settings_layout.view.removeBgButton
import kotlinx.android.synthetic.main.settings_layout.view.scalePriorityLabel
import kotlinx.android.synthetic.main.settings_layout.view.scalePrioritySwitch
import kotlinx.android.synthetic.main.settings_layout.view.settingBgFillColor
import kotlinx.android.synthetic.main.settings_layout.view.settingImagePadding
import kotlinx.android.synthetic.main.settings_layout.view.settingScalePriority
import kotlinx.android.synthetic.main.settings_layout.view.settingStackHorizontally
import kotlinx.android.synthetic.main.settings_layout.view.stackHorizontallyLabel
import kotlinx.android.synthetic.main.settings_layout.view.stackHorizontallySwitch

/** @author Aidan Follestad (afollestad) */
class SettingsLayout(
  context: Context,
  attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

  init {
    inflate(context, R.layout.settings_layout, this)
    orientation = VERTICAL
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    settingStackHorizontally.setOnClickListener { onClickStackHorizontally() }
    settingBgFillColor.setOnClickListener { onClickBackgroundFillColor() }
    settingImagePadding.setOnClickListener { onClickImagePadding() }
    settingScalePriority.setOnClickListener { onClickScalePriority() }
    removeBgButton.setOnClickListener { onClickRemoveBackground() }

    // Stack orientation
    val stackHorizontally = Prefs.stackHorizontally(context)
    stackHorizontallySwitch.isChecked = stackHorizontally
    stackHorizontallyLabel.setText(
        if (stackHorizontally) R.string.stack_horizontally
        else R.string.stack_vertically
    )

    // Scale priority
    val scalePriority = Prefs.scalePriority(context)
    scalePrioritySwitch.isChecked = scalePriority
    scalePriorityLabel.setText(
        if (scalePriority) R.string.scale_priority_on
        else R.string.scale_priority_off
    )

    // Background fill
    val bgFillColor = Prefs.bgFillColor(context)
    bgFillColorCircle.setColor(bgFillColor)
    val padding = Prefs.imageSpacing(context)
    imagePaddingLabel.text = context.getString(R.string.image_spacing_x, padding[0], padding[1])

    if (bgFillColor != TRANSPARENT) {
      removeBgButton.visibility = View.VISIBLE
      bgFillColorLabel.setText(R.string.background_fill_color)
    } else {
      bgFillColorLabel.setText(R.string.background_fill_color_transparent)
    }
  }

  private fun onClickStackHorizontally() {
    stackHorizontallySwitch.isChecked = !stackHorizontallySwitch.isChecked
    stackHorizontallyLabel.setText(
        if (stackHorizontallySwitch!!.isChecked) {
          R.string.stack_horizontally
        } else {
          R.string.stack_vertically
        }
    )
    Prefs.stackHorizontally(context, stackHorizontallySwitch.isChecked)
  }

  private fun onClickBackgroundFillColor() {
    MaterialDialog(context).show {
      title(R.string.background_fill_color_title)
      colorChooser(
          colors = PRIMARY_COLORS,
          subColors = PRIMARY_COLORS_SUB,
          initialSelection = Prefs.bgFillColor(context),
          allowCustomArgb = true,
          showAlphaSelector = true
      ) { _, color ->
        onColorSelection(color)
      }
      positiveButton(R.string.done)
      negativeButton(android.R.string.cancel)
    }
  }

  private fun onClickImagePadding() {
    ImageSpacingDialog.show(context as MainActivity)
  }

  private fun onClickScalePriority() {
    stackHorizontallySwitch.isChecked = !stackHorizontallySwitch.isChecked
    stackHorizontallyLabel.setText(
        if (stackHorizontallySwitch.isChecked) {
          R.string.scale_priority_on
        } else {
          R.string.scale_priority_off
        }
    )
    Prefs.scalePriority(context, stackHorizontallySwitch.isChecked)
  }

  private fun onClickRemoveBackground() {
    removeBgButton.visibility = View.GONE
    onColorSelection(TRANSPARENT)
  }

  private fun onColorSelection(@ColorInt selectedColor: Int) {
    if (selectedColor != TRANSPARENT) {
      removeBgButton.visibility = View.VISIBLE
      bgFillColorLabel.setText(R.string.background_fill_color)
    } else {
      bgFillColorLabel.setText(R.string.background_fill_color_transparent)
    }
    Prefs.bgFillColor(context, selectedColor)
    bgFillColorCircle.setColor(selectedColor)
  }
}
