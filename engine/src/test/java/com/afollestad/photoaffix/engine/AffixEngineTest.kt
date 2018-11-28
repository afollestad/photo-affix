/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.subengines.DimensionsEngine
import com.afollestad.photoaffix.engine.subengines.ProcessingResult
import com.afollestad.photoaffix.engine.subengines.Size
import com.afollestad.photoaffix.engine.subengines.StitchEngine
import com.afollestad.photoaffix.utilities.IoManager
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class AffixEngineTest {

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser)
  )

  private val mediaScanner = testMediaScanner()

  private val ioManager = mock<IoManager>()
  private val bitmapManipulator = mock<BitmapManipulator>()
  private val engineOwner = mock<EngineOwner> {
    on { showErrorDialog(any()) } doAnswer { inv ->
      val exception = inv.getArgument<Exception>(0)
      throw exception
    }
  }

  private val dimensionsEngine = mock<DimensionsEngine>()
  private val stitchEngine = mock<StitchEngine>()
  private val engine = RealAffixEngine(
      mediaScanner,
      ioManager,
      bitmapManipulator,
      Dispatchers.Default,
      Dispatchers.Default,
      dimensionsEngine,
      stitchEngine
  )

  // Process

  @Test fun process() = runBlocking {
    whenever(dimensionsEngine.calculateSize())
        .doReturn(Size(1, 1))

    engine.process(photos, engineOwner)
    assertThat(engine.getEngineOwner()).isEqualTo(engineOwner)
    assertThat(engine.getBitmapIterator().size()).isEqualTo(photos.size)

    verify(dimensionsEngine).setup(engine.getBitmapIterator(), engineOwner)
    verify(engineOwner).showImageSizingDialog(1, 1)
  }

  @Test fun process_noSize() = runBlocking {
    whenever(dimensionsEngine.calculateSize())
        .doReturn(Size(0, 0))

    engine.process(photos, engineOwner)
    assertThat(engine.getEngineOwner()).isEqualTo(engineOwner)
    assertThat(engine.getBitmapIterator().size()).isEqualTo(photos.size)

    verify(dimensionsEngine).setup(engine.getBitmapIterator(), engineOwner)
    verify(engineOwner, never()).showImageSizingDialog(any(), any())
  }

  // Commit results

  @Test fun commitResult_none() = runBlocking {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)

    val bitmap = mock<Bitmap>()
    val processingResult = ProcessingResult(
        processedCount = 0,
        output = bitmap
    )
    val result = engine.commitResult(processingResult)

    assertThat(result).isNull()
    verify(bitmap).recycle()
    verify(engineOwner).showContentLoading(false)
    verify(bitmapManipulator, never())
        .encodeBitmap(any(), any(), any(), any())
  }

  @Test fun commitResult() = runBlocking {
    // Override since we don't call engine.process()
    engine.setEngineOwner(engineOwner)

    val cacheFile = mock<File>()
    whenever(ioManager.makeTempFile(".jpg"))
        .doReturn(cacheFile)

    val bitmap = mock<Bitmap>()
    val processingResult = ProcessingResult(
        processedCount = 1,
        output = bitmap,
        format = JPEG,
        quality = 50
    )
    val result = engine.commitResult(processingResult)

    assertThat(result).isEqualTo(cacheFile)
    verify(bitmap).recycle()
    verify(engineOwner, never()).showContentLoading(any())
    verify(bitmapManipulator).encodeBitmap(
        bitmap,
        JPEG,
        50,
        cacheFile
    )
  }

  @Test fun commitResult_error() = runBlocking {
    // Override since we don't call engine.process()
    whenever(engineOwner.showErrorDialog(any())).doAnswer {
      // Do nothing
    }
    engine.setEngineOwner(engineOwner)

    val cacheFile = mock<File>()
    whenever(ioManager.makeTempFile(".jpg"))
        .doReturn(cacheFile)
    val error = Exception("Oh no!")
    whenever(bitmapManipulator.encodeBitmap(any(), any(), any(), any()))
        .doAnswer { throw error }

    val bitmap = mock<Bitmap>()
    val processingResult = ProcessingResult(
        processedCount = 1,
        output = bitmap,
        format = JPEG,
        quality = 50
    )
    val result = engine.commitResult(processingResult)

    assertThat(result).isNull()
    verify(cacheFile).delete()
    verify(bitmap).recycle()
    verify(engineOwner).showErrorDialog(error)
    verify(engineOwner, never()).showContentLoading(any())
    verify(bitmapManipulator).encodeBitmap(
        bitmap,
        JPEG,
        50,
        cacheFile
    )
  }

  // Done

  @Test fun done() = runBlocking {
    engine.setEngineOwner(engineOwner)

    val path = "file://idk/hello.jpg"
    val cacheFile = mock<File> {
      on { toString() } doReturn path
    }
    engine.done(cacheFile)

    verify(mediaScanner).scan(eq(path), any())
    verify(engineOwner).onDoneProcessing()
    verify(engineOwner).showContentLoading(false)
    verify(engineOwner).launchViewer(any())
  }
}
