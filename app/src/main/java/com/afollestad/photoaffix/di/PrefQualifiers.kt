/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class StackHorizontally

@Qualifier
@Retention(RUNTIME)
annotation class ScalePriority

@Qualifier
@Retention(RUNTIME)
annotation class BgFillColor

@Qualifier
@Retention(RUNTIME)
annotation class ImageSpacingVertical

@Qualifier
@Retention(RUNTIME)
annotation class ImageSpacingHorizontal
