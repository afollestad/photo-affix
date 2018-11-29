/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.photoaffix.engine

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import com.afollestad.photoaffix.engine.bitmaps.BitmapIterator
import com.afollestad.photoaffix.engine.bitmaps.BitmapManipulator
import com.afollestad.photoaffix.engine.photos.Photo
import com.afollestad.photoaffix.engine.subengines.DimensionsEngine
import com.afollestad.photoaffix.engine.subengines.ProcessingResult
import com.afollestad.photoaffix.engine.subengines.Size
import com.afollestad.photoaffix.engine.subengines.SizingResult
import com.afollestad.photoaffix.engine.subengines.StitchEngine
import com.afollestad.photoaffix.utilities.IoManager
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class AffixEngineTest {

  private val photos = listOf(
      Photo(0, "file://idk/1", 0, testUriParser),
      Photo(0, "file://idk/2", 0, testUriParser)
  )

  private val ioManager = mock<IoManager>()
  private val bitmapManipulator = mock<BitmapManipulator>()
  private val bitmapIterator = BitmapIterator(photos, bitmapManipulator)

  private val dimensionsEngine = mock<DimensionsEngine>()
  private val stitchEngine = mock<StitchEngine>()
  private val engine = RealAffixEngine(
      ioManager,
      bitmapManipulator,
      dimensionsEngine,
      stitchEngine
  )

  // Process

  @Test fun process() = runBlocking {
    val size = Size(width = 1, height = 1)
    val sizingResult = SizingResult(size = size)
    whenever(dimensionsEngine.calculateSize(any()))
        .doReturn(sizingResult)

    val processResult = engine.process(photos)
    assertThat(engine.getBitmapIterator().size()).isEqualTo(photos.size)
    assertThat(processResult).isEqualTo(sizingResult)
  }

  // Commit results

  @Test fun commitResult_noProcessed() = runBlocking {
    engine.setBitmapIterator(bitmapIterator)

    val bitmap = mock<Bitmap>()
    val processingResult = ProcessingResult(
        processedCount = 0,
        output = bitmap
    )

    whenever(
        stitchEngine.stitch(
            bitmapIterator = bitmapIterator,
            selectedScale = 1.0,
            resultWidth = 1,
            resultHeight = 1,
            format = PNG,
            quality = 100
        )
    ).doReturn(processingResult)

    val result = engine.commit(
        scale = 1.0,
        width = 1,
        height = 1,
        format = PNG,
        quality = 100
    )

    assertThat(result.error).isNotNull()
    assertThat(result.outputFile).isNull()
    verify(bitmap).recycle()
  }

  @Test fun commitResult() = runBlocking {
    engine.setBitmapIterator(bitmapIterator)

    val bitmap = mock<Bitmap>()
    val processingResult = ProcessingResult(
        processedCount = 1,
        output = bitmap
    )

    val outputFile = mock<File>()
    whenever(ioManager.makeTempFile(".png"))
        .doReturn(outputFile)
    whenever(
        stitchEngine.stitch(
            bitmapIterator = bitmapIterator,
            selectedScale = 1.0,
            resultWidth = 1,
            resultHeight = 1,
            format = PNG,
            quality = 100
        )
    ).doReturn(processingResult)

    val result = engine.commit(
        scale = 1.0,
        width = 1,
        height = 1,
        format = PNG,
        quality = 100
    )

    assertThat(result.error).isNull()
    assertThat(result.outputFile).isEqualTo(outputFile)
    verify(bitmap).recycle()
    verify(outputFile, never()).delete()
    verify(bitmapManipulator).encodeBitmap(
        bitmap = bitmap,
        format = PNG,
        quality = 100,
        file = outputFile
    )
  }

  @Test fun commitResult_encodeError() = runBlocking {
    engine.setBitmapIterator(bitmapIterator)

    val bitmap = mock<Bitmap>()
    val processingResult = ProcessingResult(
        processedCount = 0,
        output = bitmap
    )

    val outputFile = mock<File>()
    whenever(ioManager.makeTempFile(".png"))
        .doReturn(outputFile)
    whenever(
        stitchEngine.stitch(
            bitmapIterator = bitmapIterator,
            selectedScale = 1.0,
            resultWidth = 1,
            resultHeight = 1,
            format = PNG,
            quality = 100
        )
    ).doReturn(processingResult)

    val error = Exception("Oh no!")
    whenever(
        bitmapManipulator.encodeBitmap(
            any(), any(), any(), any()
        )
    ).doAnswer { throw error }

    val result = engine.commit(
        scale = 1.0,
        width = 1,
        height = 1,
        format = PNG,
        quality = 100
    )

    verify(bitmap).recycle()
    //verify(outputFile).delete()
    assertThat(result.error).isNotNull()
    assertThat(result.outputFile).isNull()
  }
}
