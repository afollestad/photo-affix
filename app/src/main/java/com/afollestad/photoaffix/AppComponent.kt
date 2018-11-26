/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix

import android.app.Application
import com.afollestad.photoaffix.activities.MainActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/** @author Aidan Follestad (afollestad) */
@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

  fun inject(mainActivity: MainActivity)

  @Component.Builder
  interface Builder {

    @BindsInstance fun application(application: Application): Builder

    fun build(): AppComponent
  }
}
