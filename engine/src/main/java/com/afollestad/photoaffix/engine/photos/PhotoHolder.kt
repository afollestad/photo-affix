/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine.photos

import java.io.Serializable

/** @author Aidan Follestad (afollestad) */
data class PhotoHolder(
  val photos: List<Photo>
) : Serializable
