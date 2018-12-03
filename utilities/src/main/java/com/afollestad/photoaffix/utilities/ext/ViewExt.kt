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
package com.afollestad.photoaffix.utilities.ext

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Spinner
import androidx.core.view.ViewCompat
import com.afollestad.photoaffix.utilities.R
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun View.show() {
  visibility = VISIBLE
}

fun View.hide() {
  visibility = GONE
}

fun View.showOrHide(show: Boolean) = if (show) show() else hide()

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

fun View.getActivity(): Activity? {
  var context = context
  while (context is ContextWrapper) {
    if (context is Activity) {
      return context
    }
    context = context.baseContext
  }
  return null
}

val View.isAttachedToWindowCompat: Boolean get() = ViewCompat.isAttachedToWindow(this)

fun View.unsubscribeOnDetach(disposableFactory: () -> Disposable) {
  val attachedDisposables = ensureAttachedDisposables()
  if (isAttachedToWindowCompat) {
    val disposable = disposableFactory()
    if (isAttachedToWindowCompat) {
      attachedDisposables.disposables += disposable
    } else {
      disposable.dispose()
    }
  } else {
    attachedDisposables += disposableFactory
  }
}

fun Disposable.unsubscribeOnDetach(view: View): Disposable {
  view.unsubscribeOnDetach { this }
  return this
}

private fun View.ensureAttachedDisposables(): AttachedDisposables {
  var attachedDisposables = getTag(
      R.id.tag_attached_disposables
  ) as AttachedDisposables?

  if (attachedDisposables == null) {
    attachedDisposables = AttachedDisposables()
    setTag(R.id.tag_attached_disposables, attachedDisposables)
    addOnAttachStateChangeListener(attachedDisposables)
  }

  return attachedDisposables
}

private class AttachedDisposables : View.OnAttachStateChangeListener {
  val disposables = CompositeDisposable()
  private val disposableFactory by lazy { mutableListOf<() -> Disposable>() }

  operator fun plusAssign(disposable: () -> Disposable) {
    disposableFactory += disposable
  }

  override fun onViewAttachedToWindow(v: View) {
    disposableFactory.apply {
      forEach { factory -> disposables += factory() }
      clear()
    }
  }

  override fun onViewDetachedFromWindow(v: View) = disposables.clear()
}
