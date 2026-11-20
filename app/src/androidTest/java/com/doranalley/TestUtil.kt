package com.doranalley

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.captureToImage
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

object TestUtil {
    val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun clearAppData() {
        File(ctx.filesDir, "progress.json").delete()
        File(ctx.filesDir, "progress.json.tmp").delete()
    }

    private var shot = 0
    /** Saves a full-screen screenshot (dialogs and popups included) for review. Never fails the test. */
    fun screenshot(rule: ComposeTestRule, name: String) {
        try {
            rule.waitForIdle()
            Thread.sleep(250) // let animations settle
            val dir = File(ctx.getExternalFilesDir(null), "screens").apply { mkdirs() }
            val bmp = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
            FileOutputStream(File(dir, "%03d_%s.png".format(++shot, name))).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (t: Throwable) {
            android.util.Log.w("TestUtil", "screenshot $name failed", t)
        }
    }

    /** Decodes a bundled mp3 to mono float samples at 16 kHz. */
    fun decodeAsset(path: String): FloatArray {
        val afd = ctx.assets.openFd(path)
        val ex = MediaExtractor()
        ex.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        val fmt = ex.getTrackFormat(0)
        ex.selectTrack(0)
        val rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(fmt, null, null, 0)
        codec.start()
        val out = ArrayList<Short>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val i = codec.dequeueInputBuffer(10_000)
                if (i >= 0) {
                    val buf = codec.getInputBuffer(i)!!
                    val n = ex.readSampleData(buf, 0)
                    if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                    else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() }
                }
            }
            val o = codec.dequeueOutputBuffer(info, 10_000)
            if (o >= 0) {
                val buf = codec.getOutputBuffer(o)!!
                val sb = buf.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                var k = 0
                while (sb.hasRemaining()) { val v = sb.get(); if (k++ % channels == 0) out.add(v) }
                codec.releaseOutputBuffer(o, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        codec.stop(); codec.release(); ex.release(); afd.close()
        val src = FloatArray(out.size) { out[it] / 32768f }
        if (rate == 16000) return src
        val n = (src.size.toLong() * 16000 / rate).toInt()
        return FloatArray(n) { i ->
            val pos = i.toDouble() * rate / 16000
            val a = pos.toInt().coerceAtMost(src.size - 1)
            val b = (a + 1).coerceAtMost(src.size - 1)
            val t = (pos - a).toFloat()
            src[a] * (1 - t) + src[b] * t
        }
    }
}
