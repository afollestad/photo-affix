/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.ofObject
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.graphics.Bitmap.CompressFormat
import android.media.MediaScannerConnection.scanFile
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_90
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.assent.Permission.READ_EXTERNAL_STORAGE
import com.afollestad.assent.Permission.WRITE_EXTERNAL_STORAGE
import com.afollestad.assent.runWithPermissions
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.photoaffix.App
import com.afollestad.photoaffix.R
import com.afollestad.photoaffix.adapters.PhotoGridAdapter
import com.afollestad.photoaffix.animation.HeightEvaluator
import com.afollestad.photoaffix.animation.ViewHideAnimationListener
import com.afollestad.photoaffix.dialogs.AboutDialog
import com.afollestad.photoaffix.dialogs.ImageSizingDialog
import com.afollestad.photoaffix.dialogs.ImageSpacingDialog
import com.afollestad.photoaffix.dialogs.SizingCallback
import com.afollestad.photoaffix.dialogs.SpacingCallback
import com.afollestad.photoaffix.engine.Photo
import com.afollestad.photoaffix.engine.PhotoLoader
import com.afollestad.photoaffix.presenters.MainPresenter
import com.afollestad.photoaffix.utilities.IoManager
import com.afollestad.photoaffix.utilities.closeQuietely
import com.afollestad.photoaffix.utilities.toast
import com.afollestad.photoaffix.viewcomponents.ImageSpacingDialogShower
import kotlinx.android.synthetic.main.activity_main.affixButton
import kotlinx.android.synthetic.main.activity_main.appbar_toolbar
import kotlinx.android.synthetic.main.activity_main.content_loading_progress_frame
import kotlinx.android.synthetic.main.activity_main.empty
import kotlinx.android.synthetic.main.activity_main.expandButton
import kotlinx.android.synthetic.main.activity_main.list
import kotlinx.android.synthetic.main.activity_main.settingsLayout
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
class MainActivity : AppCompatActivity(),
    SpacingCallback,
    SizingCallback,
    MainView,
    ImageSpacingDialogShower {

  companion object {
    private const val BROWSE_RC = 21
  }

  @Inject lateinit var photoLoader: PhotoLoader
  @Inject lateinit var mainPresenter: MainPresenter
  @Inject lateinit var ioManager: IoManager

  private lateinit var adapter: PhotoGridAdapter

  private var settingsFrameAnimator: ValueAnimator? = null
  private var autoSelectFirst: Boolean = false
  private var originalSettingsFrameHeight = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (application as App).appComponent.inject(this)
    setContentView(R.layout.activity_main)

    appbar_toolbar.inflateMenu(R.menu.menu_main)
    appbar_toolbar.setOnMenuItemClickListener { item ->
      when {
        item.itemId == R.id.clear -> {
          clearSelection()
          true
        }
        item.itemId == R.id.about -> {
          AboutDialog.show(this@MainActivity)
          true
        }
        else -> false
      }
    }

    affixButton.setOnClickListener {
      runWithPermissions(WRITE_EXTERNAL_STORAGE) {
        mainPresenter.onClickAffix(adapter.selectedPhotos)
      }
    }
    expandButton.setOnClickListener { toggleSettingsExpansion() }

    setupMainGrid(savedInstanceState)
    processIntent(intent)
  }

  private fun setupMainGrid(savedInstanceState: Bundle?) {
    adapter = PhotoGridAdapter(this)
    adapter.restoreInstanceState(savedInstanceState)
    adapter.onSelection { _, count ->
      affixButton.text = getString(R.string.affix_x, count)
      affixButton.isEnabled = count > 0
      appbar_toolbar
          .menu
          .findItem(R.id.clear)
          .isVisible = adapter.hasSelection()
    }

    list.layoutManager = GridLayoutManager(this, resources.getInteger(R.integer.grid_width))
    list.adapter = adapter
    val animator = DefaultItemAnimator().apply {
      supportsChangeAnimations = false
    }
    list.itemAnimator = animator

    val dragListener = DragSelectTouchListener.create(this, adapter)
    adapter.dragListener = dragListener
    list.addOnItemTouchListener(dragListener)
  }

  override fun clearSelection() = runOnUiThread {
    mainPresenter.clearPhotos()
    adapter.clearSelected()
    appbar_toolbar.menu
        .findItem(R.id.clear)
        .isVisible = false
  }

  override fun showContentLoading(loading: Boolean) = runOnUiThread {
    content_loading_progress_frame.visibility =
        if (loading) VISIBLE else GONE
  }

  override fun launchViewer(uri: Uri) = runOnUiThread {
    try {
      startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*"))
    } catch (_: ActivityNotFoundException) {
    }
  }

  override fun lockOrientation() {
    val orientation: Int
    val rotation = (getSystemService(WINDOW_SERVICE) as WindowManager)
        .defaultDisplay
        .rotation
    orientation = when (rotation) {
      ROTATION_0 -> SCREEN_ORIENTATION_PORTRAIT
      ROTATION_90 -> SCREEN_ORIENTATION_LANDSCAPE
      ROTATION_180 -> SCREEN_ORIENTATION_REVERSE_PORTRAIT
      else -> SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    }
    requestedOrientation = orientation
  }

  override fun unlockOrientation() {
    requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
  }

  override fun showErrorDialog(e: Exception) {
    e.printStackTrace()
    MaterialDialog(this).show {
      title(R.string.error)
      message(text = e.message)
      positiveButton(android.R.string.ok)
    }
  }

  override fun showMemoryError() = showErrorDialog(
      Exception("Your device is low on RAM!")
  )

  override fun showImageSizingDialog(
    width: Int,
    height: Int
  ) = ImageSizingDialog.show(this, width, height)

  override fun showImageSpacingDialog() = ImageSpacingDialog.show(this)

  fun browseExternalPhotos() {
    val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
    startActivityForResult(intent, BROWSE_RC)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    processIntent(intent)
  }

  override fun onSaveInstanceState(
    outState: Bundle,
    outPersistentState: PersistableBundle
  ) {
    super.onSaveInstanceState(outState, outPersistentState)
    adapter.saveInstanceState(outState)
  }

  override fun onStart() {
    super.onStart()
    mainPresenter.attachView(this)
    refresh()
  }

  override fun onStop() {
    mainPresenter.detachView()
    super.onStop()
  }

  override fun onSpacingChanged(
    horizontal: Int,
    vertical: Int
  ) = settingsLayout.imageSpacingUpdated(horizontal, vertical)

  override fun onSizeChanged(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  ) = mainPresenter.sizeDetermined(scale, resultWidth, resultHeight, format, quality, cancelled)

  override fun onDoneProcessing() {
    clearSelection()
    unlockOrientation()
  }

  override fun onBackPressed() {
    if (adapter.hasSelection()) {
      clearSelection()
    } else
      super.onBackPressed()
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (data != null && requestCode == BROWSE_RC && resultCode == RESULT_OK) {
      GlobalScope.launch(IO) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        val targetFile = ioManager.makeTempFile(".png")

        try {
          input = ioManager.openStream(data.data!!)
          output = FileOutputStream(targetFile)
          input!!.copyTo(output)
          output.close()

          scanFile(
              this@MainActivity,
              arrayOf(targetFile.toString()), null
          ) { _, _ ->
            autoSelectFirst = true
            refresh()
          }
        } catch (e: Exception) {
          toast(message = e.message)
        } finally {
          input.closeQuietely()
          output.closeQuietely()
        }
      }
    }
  }

  private fun processIntent(intent: Intent?) {
    if (intent != null && Intent.ACTION_SEND_MULTIPLE == intent.action) {
      val uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_STREAM)
      if (uris != null && uris.size > 1) {
        mainPresenter.onClickAffix(
            uris.map { Photo(0, it.toString(), 0) }
        )
      } else {
        toast(R.string.need_two_or_more)
        finish()
      }
    }
  }

  private fun refresh() = runWithPermissions(READ_EXTERNAL_STORAGE) {
    GlobalScope.launch(Main) {
      val photos = withContext(IO) { photoLoader.queryPhotos() }
      adapter.setPhotos(photos)
      empty.visibility = if (photos.isEmpty()) VISIBLE else GONE

      if (photos.isNotEmpty() && autoSelectFirst) {
        adapter.shiftSelections()
        adapter.setSelected(1, true)
        autoSelectFirst = false
      }
    }
  }

  private fun toggleSettingsExpansion() {
    if (originalSettingsFrameHeight == -1) {
      val settingControlHeight = resources.getDimension(R.dimen.settings_control_height)
          .toInt()
      originalSettingsFrameHeight = settingControlHeight * settingsLayout.childCount
    }
    settingsFrameAnimator?.cancel()

    if (settingsLayout.visibility == GONE) {
      settingsLayout.visibility = VISIBLE
      expandButton.setImageResource(R.drawable.ic_collapse)
      settingsFrameAnimator = ofObject(
          HeightEvaluator(settingsLayout),
          0,
          originalSettingsFrameHeight
      )
    } else {
      expandButton.setImageResource(R.drawable.ic_expand)
      settingsFrameAnimator = ofObject(
          HeightEvaluator(settingsLayout),
          originalSettingsFrameHeight,
          0
      )
      settingsFrameAnimator!!.addListener(ViewHideAnimationListener(settingsLayout))
    }

    settingsFrameAnimator!!.run {
      interpolator = FastOutSlowInInterpolator()
      duration = 200
      start()
    }
  }
}
