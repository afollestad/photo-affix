/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.animation

import android.animation.Animator
import android.view.View
import android.view.View.GONE

/** @author Aidan Follestad (afollestad) */
class ViewHideAnimationListener(private val view: View) : Animator.AnimatorListener {

  override fun onAnimationStart(animation: Animator) = Unit

  override fun onAnimationEnd(animation: Animator) {
    view.visibility = GONE
  }

  override fun onAnimationCancel(animation: Animator) = Unit

  override fun onAnimationRepeat(animation: Animator) = Unit
}
