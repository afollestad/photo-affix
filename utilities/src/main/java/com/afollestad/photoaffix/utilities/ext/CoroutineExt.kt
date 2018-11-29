/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities.ext

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

fun View.scopeWhileAttached(
  context: CoroutineContext,
  exec: CoroutineScope.() -> Unit
) {
  val job = Job(context[Job])

  addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) = Unit
    override fun onViewDetachedFromWindow(v: View) {
      job.cancel()
    }
  })

  exec(CoroutineScope(context + job))
}
