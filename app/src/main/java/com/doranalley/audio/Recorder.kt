package com.doranalley.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Records the learner's voice to a local AAC file. Nothing is uploaded. */
class Recorder(private val context: Context) {

    var recordingKey by mutableStateOf<String?>(null)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set

    private var rec: MediaRecorder? = null
    private var file: File? = null

    val isRecording get() = recordingKey != null

    fun start(key: String, target: File): Boolean {
        cancel()
        target.parentFile?.mkdirs()
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(96_000)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            rec = r
            file = target
            recordingKey = key
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            target.delete()
            false
        }
    }

    /** Stops and returns the file and its duration, or null if nothing usable was recorded. */
    fun stop(): Pair<File, Long>? {
        val r = rec ?: return null
        val f = file
        val duration = SystemClock.elapsedRealtime() - startedAt
        rec = null
        file = null
        recordingKey = null
        return try {
            r.stop()
            if (f != null && f.length() > 0) f to duration else null
        } catch (e: RuntimeException) {
            // Stopping right after starting throws; treat as an empty take.
            f?.delete()
            null
        } finally {
            runCatching { r.release() }
        }
    }

    fun cancel() {
        stop()?.first?.delete()
    }
}
