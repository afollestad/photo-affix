/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.ofObject
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.content.Intent.EXTRA_STREAM
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.assent.Permission.READ_EXTERNAL_STORAGE
import com.afollestad.assent.Permission.WRITE_EXTERNAL_STORAGE
import com.afollestad.assent.runWithPermissions
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
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
import com.afollestad.photoaffix.engine.AffixEngine
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.presenters.MainPresenter
import com.afollestad.photoaffix.utilities.MediaScanner
import com.afollestad.photoaffix.utilities.ext.hide
import com.afollestad.photoaffix.utilities.ext.scopeWhileAttached
import com.afollestad.photoaffix.utilities.ext.show
import com.afollestad.photoaffix.utilities.ext.showOrHide
import com.afollestad.photoaffix.utilities.ext.toast
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

/** @author Aidan Follestad (afollestad) */
class MainActivity : AppCompatActivity(),
    SpacingCallback,
    SizingCallback,
    ImageSpacingDialogShower {

  companion object {
    private const val BROWSE_RC = 21
  }

  @Inject lateinit var presenter: MainPresenter
  @Inject lateinit var affixEngine: AffixEngine
  @Inject lateinit var mediaScanner: MediaScanner

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
        performAffix(adapter.selectedPhotos)
      }
    }
    expandButton.setOnClickListener { toggleSettingsExpansion() }

    setupMainGrid(savedInstanceState)
    processIntent(intent)
  }

  fun browseExternalPhotos() {
    presenter.resetLoadThreshold()
    val intent = Intent(ACTION_GET_CONTENT)
        .setType("image/*")
    startActivityForResult(intent, BROWSE_RC)
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

  private fun performAffix(photos: List<Photo>) {
    lockOrientation()
    content_loading_progress_frame.show()

    appbar_toolbar.scopeWhileAttached(Main) {
      launch(coroutineContext) {
        val sizingResult = async(IO) {
          affixEngine.process(photos)
        }.await()

        if (sizingResult.isError()) {
          showErrorDialog(sizingResult.error!!)
          return@launch
        }

        val size = sizingResult.size!!
        ImageSizingDialog.show(
            context = this@MainActivity,
            width = size.width,
            height = size.height
        )
      }
    }
  }

  private fun refresh(force: Boolean = false) = runWithPermissions(READ_EXTERNAL_STORAGE) {
    if (force) {
      presenter.resetLoadThreshold()
    }

    appbar_toolbar.scopeWhileAttached(Main) {
      launch(coroutineContext) {
        val photos = async(IO) {
          presenter.loadPhotos()
        }.await()

        if (photos.isEmpty()) {
          return@launch
        }
        adapter.setPhotos(photos)
        empty.showOrHide(photos.isEmpty())

        if (photos.isNotEmpty() && autoSelectFirst) {
          adapter.shiftSelections()
          adapter.setSelected(1, true)
          autoSelectFirst = false
        }
      }
    }
  }

  private fun clearSelection() {
    affixEngine.reset()
    adapter.clearSelected()
    appbar_toolbar.menu
        .findItem(R.id.clear)
        .isVisible = false
  }

  override fun onSizeChanged(
    scale: Double,
    resultWidth: Int,
    resultHeight: Int,
    format: CompressFormat,
    quality: Int,
    cancelled: Boolean
  ) {
    if (cancelled) {
      affixEngine.reset()
      unlockOrientation()
      content_loading_progress_frame.hide()
      return
    }

    appbar_toolbar.scopeWhileAttached(Main) {
      launch(coroutineContext) {
        val result = async(IO) {
          affixEngine.commit(
              scale,
              resultWidth,
              resultHeight,
              format,
              quality
          )
        }.await()

        if (result.isError()) {
          showErrorDialog(result.error!!)
          return@launch
        }

        mediaScanner.scan(result.outputFile!!) { _, uri ->
          presenter.resetLoadThreshold()
          clearSelection()
          unlockOrientation()
          content_loading_progress_frame.hide()
          viewUri(uri) { refresh(force = true) }
        }
      }
    }
  }

  override fun showImageSpacingDialog() =
    ImageSpacingDialog.show(this)

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
    refresh()
  }

  override fun onResume() {
    super.onResume()
    if (!adapter.hasPhotos()) {
      refresh()
    }
  }

  override fun onSpacingChanged(
    horizontal: Int,
    vertical: Int
  ) = settingsLayout.imageSpacingUpdated(horizontal, vertical)

  override fun onBackPressed() {
    if (adapter.hasSelection()) {
      clearSelection()
    } else {
      super.onBackPressed()
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (data != null && requestCode == BROWSE_RC && resultCode == RESULT_OK) {
      autoSelectFirst = true

      appbar_toolbar.scopeWhileAttached(Main) {
        launch(coroutineContext) {
          try {
            val selection = async(IO) {
              presenter.onExternalPhotoSelected(data.data!!)
            }.await()!!

            mediaScanner.scan(selection) { _, _ ->
              refresh(force = true)
            }
          } catch (e: Exception) {
            showErrorDialog(e)
          }
        }
      }
    }
  }

  private fun processIntent(intent: Intent?) {
    if (intent != null && ACTION_SEND_MULTIPLE == intent.action) {
      val uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_STREAM)

      if (uris != null && uris.size > 1) {
        val photos = uris.map { Photo(0, it.toString(), 0) }
        performAffix(photos)
      } else {
        toast(R.string.need_two_or_more)
        finish()
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

    settingsFrameAnimator?.run {
      interpolator = FastOutSlowInInterpolator()
      duration = 200
      start()
    }
  }
}
