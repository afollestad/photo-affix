/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Bitmap.CompressFormat.PNG
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.photoaffix.utilities.ext.onItemSelected
import com.afollestad.photoaffix.utilities.ext.onProgressChanged
import com.afollestad.photoaffix.utilities.ext.round
import kotlinx.android.synthetic.main.dialog_imagesizing.view.formatSpinner
import kotlinx.android.synthetic.main.dialog_imagesizing.view.inputHeight
import kotlinx.android.synthetic.main.dialog_imagesizing.view.inputScaleLabel
import kotlinx.android.synthetic.main.dialog_imagesizing.view.inputScaleSeek
import kotlinx.android.synthetic.main.dialog_imagesizing.view.inputWidth
import kotlinx.android.synthetic.main.dialog_imagesizing.view.qualityLabel
import kotlinx.android.synthetic.main.dialog_imagesizing.view.qualitySeeker
import kotlinx.android.synthetic.main.dialog_imagesizing.view.qualityTitle

interface SizingCallback {
  fun onSizeChanged(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  )
}

/** @author Aidan Follestad (afollestad) */
class ImageSizingDialog : DialogFragment() {

  companion object {
    private const val TAG = "[IMAGE_SIZING_DIALOG]"

    fun show(
      context: AppCompatActivity,
      width: Int,
      height: Int
    ) {
      val dialog = ImageSizingDialog()
      val args = Bundle().apply {
        putInt("width", width)
        putInt("height", height)
      }
      dialog.arguments = args
      dialog.show(context.supportFragmentManager, TAG)
    }
  }

  private var callback: SizingCallback? = null
  private var minimum: Int = 0

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val context = activity
    requireNotNull(context)

    val dialog = MaterialDialog(context)
        .title(R.string.output_settings)
        .customView(R.layout.dialog_imagesizing)
        .positiveButton(R.string.continueStr) { invokeCallback() }
        .negativeButton(android.R.string.cancel) {
          callback?.onSizeChanged(
              scale = 0.0,
              resultWidth = -1,
              resultHeight = -1,
              format = PNG,
              quality = 100,
              cancelled = true
          )
          dismiss()
        }
        .noAutoDismiss()

    val customView = dialog.getCustomView() ?: return dialog
    minimum = (customView.inputScaleSeek.max * 0.1).toInt()

    customView.inputScaleSeek.max = (customView.inputScaleSeek.max * 0.9).toInt()
    customView.inputScaleSeek.onProgressChanged { oldProgress ->
      var newProgress = oldProgress
      newProgress += minimum

      val scale = (newProgress.toDouble() / 1000.0).round()
      val scaleStr = when (scale) {
        0.0 -> "0.00"
        1.0 -> "1.00"
        else -> scale.toString().apply {
          if (this.length == 3) this.plus("0")
        }
      }
      customView.inputScaleLabel.text = scaleStr

      val originalWidth = arguments?.getInt("width") ?: -1
      val originalHeight = arguments?.getInt("height") ?: -1
      customView.inputWidth.text = (originalWidth * scale).toInt()
          .toString()
      customView.inputHeight.text = (originalHeight * scale).toInt()
          .toString()
    }
    customView.inputScaleSeek.progress = customView.inputScaleSeek.max

    val adapter = ArrayAdapter(context, R.layout.spinner_item, arrayOf("PNG", "JPEG"))
    adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
    customView.formatSpinner.onItemSelected {
      val visibility = if (it == 1) VISIBLE else GONE
      customView.qualityTitle.visibility = visibility
      customView.qualitySeeker.visibility = visibility
      customView.qualityLabel.visibility = visibility
      customView.qualitySeeker.progress = 99
    }
    customView.formatSpinner.adapter = adapter

    customView.qualitySeeker.max = 99
    customView.qualitySeeker.onProgressChanged {
      customView.qualityLabel.text = (it + 1).toString()
    }
    customView.qualitySeeker.progress = 99

    return dialog
  }

  private fun invokeCallback() {
    val materialDialog = dialog as? MaterialDialog ?: return
    val customView = materialDialog.getCustomView() ?: return

    val progress = customView.inputScaleSeek.progress + minimum
    val scale = (progress.toDouble() / 1000.0).round()
    val width = customView.inputWidth.text.toString()
        .trim()
        .toInt()
    val height = customView.inputHeight.text.toString()
        .trim()
        .toInt()
    val quality = customView.qualitySeeker.progress + 1

    callback?.onSizeChanged(
        scale = scale,
        resultWidth = width,
        resultHeight = height,
        format = selectedFormat(),
        quality = quality,
        cancelled = false
    )
    dismiss()
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    callback = context as? SizingCallback
  }

  override fun onCancel(dialog: DialogInterface?) {
    super.onCancel(dialog)
    callback?.onSizeChanged(
        scale = 0.0,
        resultWidth = -1,
        resultHeight = -1,
        format = PNG,
        quality = 100,
        cancelled = true
    )
  }

  private fun selectedFormat(): CompressFormat = with(dialog as MaterialDialog) {
    val customView = getCustomView() ?: return PNG
    return if (customView.formatSpinner.selectedItemPosition == 0) {
      PNG
    } else {
      JPEG
    }
  }
}
