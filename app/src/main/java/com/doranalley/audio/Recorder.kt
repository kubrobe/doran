package com.doranalley.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.doranalley.score.Wav
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.log10

/**
 * Records the learner's voice to a local 16-bit WAV file with AudioRecord. Nothing is uploaded.
 *
 * Raw PCM (instead of a compressed MediaRecorder file) gives a live level meter, lets the app tell
 * "the microphone heard nothing" apart from a real take, and feeds the pronunciation score directly.
 */
class Recorder {

    var recordingKey by mutableStateOf<String?>(null)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set
    /** 0..1 input level for the meter. */
    var level by mutableFloatStateOf(0f)
        private set
    /** Loudest moment of the current/last take in dBFS. */
    var peakDb by mutableFloatStateOf(-90f)
        private set

    /** Called on the main thread when a take hits its time limit. */
    var onAutoStop: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var thread: Thread? = null
    private var record: AudioRecord? = null
    private var file: File? = null
    private var raf: RandomAccessFile? = null
    @Volatile private var bytes = 0
    private var rate = 16_000

    val isRecording get() = recordingKey != null

    @SuppressLint("MissingPermission") // Callers check RECORD_AUDIO first; a SecurityException is caught below.
    fun start(key: String, target: File, maxMs: Long = 60_000): Boolean {
        cancel()
        target.parentFile?.mkdirs()
        val r = open() ?: return false
        var opened: RandomAccessFile? = null
        return try {
            val out = RandomAccessFile(target, "rw").apply { setLength(0) }
            opened = out
            Wav.writeHeader(out, rate, 0)
            r.startRecording()
            if (r.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw IllegalStateException("mic busy")
            record = r
            raf = out
            file = target
            bytes = 0
            peakDb = -90f
            level = 0f
            running = true
            recordingKey = key
            startedAt = SystemClock.elapsedRealtime()
            val t = Thread({ loop(r, out, maxMs) }, "doran-recorder")
            thread = t
            t.start()
            true
        } catch (e: Exception) {
            running = false
            runCatching { r.release() }
            runCatching { opened?.close() }
            raf = null
            record = null
            target.delete()
            false
        }
    }

    /** Tries the speech-tuned source first, then the plain microphone; 16 kHz first, then 44.1 kHz. */
    @SuppressLint("MissingPermission")
    private fun open(): AudioRecord? {
        val sources = listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC)
        for (src in sources) for (sr in listOf(16_000, 44_100, 48_000)) {
            val min = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val r = try {
                AudioRecord(src, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min * 2, sr / 5))
            } catch (e: Exception) { null } ?: continue
            if (r.state == AudioRecord.STATE_INITIALIZED) { rate = sr; return r }
            runCatching { r.release() }
        }
        return null
    }

    private fun loop(r: AudioRecord, out: RandomAccessFile, maxMs: Long) {
        val buf = ShortArray(rate / 20) // 50 ms
        val bb = ByteArray(buf.size * 2)
        val began = SystemClock.elapsedRealtime()
        var peak = -90f
        while (running) {
            val n = try { r.read(buf, 0, buf.size) } catch (e: Exception) { -1 }
            if (n < 0) break
            if (n == 0) continue
            var sum = 0.0
            for (i in 0 until n) {
                val v = buf[i].toInt()
                bb[2 * i] = (v and 0xFF).toByte()
                bb[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
                sum += (v / 32768.0) * (v / 32768.0)
            }
            try { out.write(bb, 0, n * 2) } catch (e: Exception) { break }
            bytes += n * 2
            val db = (10 * log10(sum / n + 1e-12)).toFloat()
            if (db > peak) peak = db
            val lv = ((db + 60f) / 50f).coerceIn(0f, 1f)
            val pk = peak
            main.post { if (running) { level = lv; peakDb = pk } }
            if (SystemClock.elapsedRealtime() - began > maxMs) {
                main.post { onAutoStop?.invoke() }
                break
            }
        }
        // Keep reading nothing more; stop() finalizes the file.
        val pk = peak
        main.post { peakDb = pk }
    }

    /** Stops and returns the file and its duration, or null if nothing usable was recorded. */
    fun stop(): Pair<File, Long>? {
        val r = record ?: return null
        val f = file
        val out = raf
        val duration = SystemClock.elapsedRealtime() - startedAt
        running = false
        runCatching { thread?.join(1500) }
        thread = null
        runCatching { r.stop() }
        runCatching { r.release() }
        record = null
        file = null
        raf = null
        recordingKey = null
        level = 0f
        val ok = try {
            out?.let { Wav.writeHeader(it, rate, bytes); it.close() }
            // Under ~0.3 s is an accidental tap, not a take.
            f != null && bytes > rate * 2 * 3 / 10
        } catch (e: Exception) { false }
        if (!ok) { f?.delete(); return null }
        return f!! to duration
    }

    fun cancel() {
        stop()?.first?.delete()
    }

    /** True when the last take never rose above background hiss. */
    fun lastTakeWasSilent(): Boolean = peakDb < -50f
}
