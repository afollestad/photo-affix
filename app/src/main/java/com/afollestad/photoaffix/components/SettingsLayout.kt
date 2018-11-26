/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.components

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.photoaffix.App
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.constants.PRIMARY_COLORS
import com.afollestad.photoaffix.constants.PRIMARY_COLORS_SUB
import com.afollestad.photoaffix.di.BgFillColor
import com.afollestad.photoaffix.di.ImageSpacingHorizontal
import com.afollestad.photoaffix.di.ImageSpacingVertical
import com.afollestad.photoaffix.di.ScalePriority
import com.afollestad.photoaffix.di.StackHorizontally
import com.afollestad.photoaffix.dialogs.ImageSpacingDialog
import com.afollestad.photoaffix.utils.getActivity
import com.afollestad.photoaffix.utils.unsubscribeOnDetach
import com.afollestad.photoaffix.views.MainActivity
import com.afollestad.rxkprefs.Pref
import com.jakewharton.rxbinding3.widget.checkedChanges
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
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
class SettingsLayout(
  context: Context,
  attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

  @Inject
  @field:StackHorizontally
  lateinit var stackHorizontallyPref: Pref<Boolean>
  @Inject
  @field:ScalePriority
  lateinit var scalePriorityPref: Pref<Boolean>
  @Inject
  @field:BgFillColor
  lateinit var bgFillColorPref: Pref<Int>
  @Inject
  @field:ImageSpacingVertical
  lateinit var verticalSpacingPref: Pref<Int>
  @Inject
  @field:ImageSpacingHorizontal
  lateinit var horizontalSpacingPref: Pref<Int>

  init {
    inflate(context, R.layout.settings_layout, this)
    orientation = VERTICAL
  }

  override fun onFinishInflate() {
    super.onFinishInflate()

    (context.applicationContext as App)
        .appComponent
        .inject(this)

    settingStackHorizontally.setOnClickListener { onClickStackHorizontally() }
    settingBgFillColor.setOnClickListener { onClickBackgroundFillColor() }
    settingImagePadding.setOnClickListener { onClickImagePadding() }
    settingScalePriority.setOnClickListener { onClickScalePriority() }
    removeBgButton.setOnClickListener { onClickRemoveBackground() }

    // Stack orientation
    val stackHorizontally = stackHorizontallyPref.get()
    stackHorizontallySwitch.isChecked = stackHorizontally
    stackHorizontallyLabel.setText(
        if (stackHorizontally) R.string.stack_horizontally
        else R.string.stack_vertically
    )
    stackHorizontallySwitch.checkedChanges()
        .subscribe(stackHorizontallyPref)
        .unsubscribeOnDetach(this)

    // Scale priority
    val scalePriority = scalePriorityPref.get()
    scalePrioritySwitch.isChecked = scalePriority
    scalePriorityLabel.setText(
        if (scalePriority) R.string.scale_priority_on
        else R.string.scale_priority_off
    )
    scalePrioritySwitch.checkedChanges()
        .subscribe(scalePriorityPref)
        .unsubscribeOnDetach(this)

    // Background fill
    val bgFillColor = bgFillColorPref.get()
    bgFillColorCircle.setColor(bgFillColor)
    val verticalSpacing = verticalSpacingPref.get()
    val horizontalSpacing = horizontalSpacingPref.get()
    imagePaddingLabel.text = context.getString(
        R.string.image_spacing_x,
        horizontalSpacing,
        verticalSpacing
    )

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
        if (stackHorizontallySwitch.isChecked) {
          R.string.stack_horizontally
        } else {
          R.string.stack_vertically
        }
    )
  }

  private fun onClickBackgroundFillColor() {
    MaterialDialog(getActivity() as MainActivity).show {
      title(R.string.background_fill_color_title)
      colorChooser(
          colors = PRIMARY_COLORS,
          subColors = PRIMARY_COLORS_SUB,
          initialSelection = bgFillColorPref.get(),
          allowCustomArgb = true,
          showAlphaSelector = true
      ) { _, color ->
        onColorSelection(color)
      }
      positiveButton(R.string.done)
      negativeButton(android.R.string.cancel)
    }
  }

  private fun onClickImagePadding() =
    ImageSpacingDialog.show(getActivity() as MainActivity)

  private fun onClickScalePriority() {
    scalePrioritySwitch.isChecked = !scalePrioritySwitch.isChecked
    scalePrioritySwitch.setText(
        if (scalePrioritySwitch.isChecked) {
          R.string.scale_priority_on
        } else {
          R.string.scale_priority_off
        }
    )
  }

  private fun onClickRemoveBackground() {
    removeBgButton.visibility = GONE
    onColorSelection(TRANSPARENT)
  }

  private fun onColorSelection(@ColorInt selectedColor: Int) {
    if (selectedColor != TRANSPARENT) {
      removeBgButton.visibility = VISIBLE
      bgFillColorLabel.setText(R.string.background_fill_color)
      bgFillColorPref.set(selectedColor)
    } else {
      bgFillColorLabel.setText(R.string.background_fill_color_transparent)
      bgFillColorPref.delete()
    }
    bgFillColorCircle.setColor(selectedColor)
  }
}
