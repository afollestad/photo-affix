/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utils

import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Spinner

fun SeekBar.onProgressChanged(cb: (Int) -> Unit) {
  setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(
      seekBar: SeekBar?,
      progress: Int,
      fromUser: Boolean
    ) = cb(progress)

    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
  })
}

fun Spinner.onItemSelected(cb: (Int) -> Unit) {
  onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(
      parent: AdapterView<*>,
      view: View,
      position: Int,
      id: Long
    ) = cb(position)

    override fun onNothingSelected(parent: AdapterView<*>) = Unit
  }
}

fun Double.round(): Double {
  return Math.round(this * 100.0) / 100.0
}
