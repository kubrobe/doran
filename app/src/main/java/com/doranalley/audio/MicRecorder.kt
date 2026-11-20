package com.doranalley.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

/**
 * Records 16 kHz mono speech straight from the microphone with AudioRecord.
 * Stops on its own after the learner stops talking, or when [stop] is called.
 */
class MicRecorder(context: Context) {
    private val cacheDir = context.applicationContext.cacheDir
    private val _level = MutableStateFlow(0f)
    /** 0..1 loudness for the live meter. */
    val level: StateFlow<Float> = _level.asStateFlow()

    @Volatile private var stopRequested = false

    sealed interface Result {
        /** [heardVoice] is false when the recording was (almost) silent. */
        data class Ok(val samples: FloatArray, val file: File, val heardVoice: Boolean, val seconds: Float) : Result
        data class Failed(val reason: String) : Result
    }

    fun stop() { stopRequested = true }

    val lastTake: File get() = File(cacheDir, "last_take.wav")

    @SuppressLint("MissingPermission") // The caller checks RECORD_AUDIO; SecurityException is still handled.
    suspend fun record(maxSeconds: Float = 12f): Result = withContext(Dispatchers.IO) {
        stopRequested = false
        val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return@withContext Result.Failed("This phone does not support 16 kHz recording.")
        val bufSize = maxOf(minBuf, RATE / 5 * 2)
        var rec: AudioRecord? = null
        for (source in listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.DEFAULT)) {
            try {
                val r = AudioRecord(source, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
                if (r.state == AudioRecord.STATE_INITIALIZED) { rec = r; break }
                r.release()
            } catch (e: SecurityException) {
                return@withContext Result.Failed("Microphone permission is off.")
            } catch (t: Throwable) {
                Log.w(TAG, "source $source failed", t)
            }
        }
        val r = rec ?: return@withContext Result.Failed("The microphone could not be opened. Close other apps that use it and try again.")
        val pcm = ShortArrayBuilder(RATE * 4)
        try {
            r.startRecording()
            if (r.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return@withContext Result.Failed("The microphone is busy. Close other apps that use it and try again.")
            }
            val frame = ShortArray(RATE / 50) // 20 ms
            var noiseFloor = Double.MAX_VALUE
            var frames = 0
            var speechFrames = 0
            var silenceAfterSpeech = 0
            var heard = false
            val maxFrames = (maxSeconds * 50).toInt()
            while (coroutineContext.isActive && !stopRequested && frames < maxFrames) {
                val n = r.read(frame, 0, frame.size)
                if (n < 0) return@withContext Result.Failed("The microphone stopped working (error $n).")
                if (n == 0) continue
                pcm.add(frame, n)
                frames++
                var sum = 0.0
                for (i in 0 until n) sum += frame[i].toDouble() * frame[i]
                val rms = sqrt(sum / n)
                _level.value = (rms / 6000.0).coerceIn(0.0, 1.0).toFloat()
                if (frames <= 15) noiseFloor = minOf(noiseFloor, rms)
                val threshold = maxOf(350.0, (if (noiseFloor == Double.MAX_VALUE) 0.0 else noiseFloor) * 3.0)
                if (rms > threshold) {
                    speechFrames++
                    if (speechFrames >= 4) heard = true
                    silenceAfterSpeech = 0
                } else {
                    speechFrames = 0
                    if (heard) silenceAfterSpeech++
                }
                // Stop 1.4 s after the learner stops talking, or after 7 s of silence from the start.
                if (heard && silenceAfterSpeech > 70) break
                if (!heard && frames > 350) break
            }
        } catch (t: Throwable) {
            Log.e(TAG, "recording failed", t)
            return@withContext Result.Failed("Recording failed: ${t.message ?: t.javaClass.simpleName}")
        } finally {
            runCatching { r.stop() }
            r.release()
            _level.value = 0f
        }
        val shorts = pcm.toArray()
        var peak = 0
        for (s in shorts) peak = maxOf(peak, kotlin.math.abs(s.toInt()))
        val file = lastTake
        writeWav(file, shorts)
        val floats = FloatArray(shorts.size) { shorts[it] / 32768f }
        Result.Ok(floats, file, heardVoice = peak > 800 && shorts.size > RATE / 4, seconds = shorts.size / RATE.toFloat())
    }

    private class ShortArrayBuilder(cap: Int) {
        private var a = ShortArray(cap)
        private var n = 0
        fun add(src: ShortArray, count: Int) {
            if (n + count > a.size) a = a.copyOf(maxOf(a.size * 2, n + count))
            System.arraycopy(src, 0, a, n, count); n += count
        }
        fun toArray(): ShortArray = a.copyOf(n)
    }

    companion object {
        const val RATE = 16000
        private const val TAG = "MicRecorder"

        fun writeWav(file: File, pcm: ShortArray, rate: Int = RATE) {
            RandomAccessFile(file, "rw").use { f ->
                f.setLength(0)
                val dataLen = pcm.size * 2
                fun i32(v: Int) { f.write(v and 0xff); f.write(v shr 8 and 0xff); f.write(v shr 16 and 0xff); f.write(v shr 24 and 0xff) }
                fun i16(v: Int) { f.write(v and 0xff); f.write(v shr 8 and 0xff) }
                f.writeBytes("RIFF"); i32(36 + dataLen); f.writeBytes("WAVE")
                f.writeBytes("fmt "); i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
                f.writeBytes("data"); i32(dataLen)
                val bytes = ByteArray(dataLen)
                for (i in pcm.indices) { bytes[2 * i] = (pcm[i].toInt() and 0xff).toByte(); bytes[2 * i + 1] = (pcm[i].toInt() shr 8).toByte() }
                f.write(bytes)
            }
        }
    }
}
