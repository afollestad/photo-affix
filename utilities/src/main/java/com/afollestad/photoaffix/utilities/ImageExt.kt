/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.utilities

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

typealias OnImageLoaded = ((Exception?) -> Boolean)?

fun ImageView.loadImage(
  uri: Uri,
  cb: OnImageLoaded = null
) {
  Glide.with(context)
      .load(uri)
      .listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
          e: GlideException?,
          model: Any?,
          target: Target<Drawable>?,
          isFirstResource: Boolean
        ): Boolean = cb?.invoke(e) ?: false

        override fun onResourceReady(
          resource: Drawable?,
          model: Any?,
          target: Target<Drawable>?,
          dataSource: DataSource?,
          isFirstResource: Boolean
        ): Boolean = cb?.invoke(null) ?: false
      })
      .into(this)
}

fun Bitmap?.safeRecycle() = try {
  this?.recycle()
} catch (_: Throwable) {
}

fun CompressFormat.extension() = if (this == PNG) {
  ".png"
} else {
  ".jpg"
}
