package com.doranalley.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.doranalley.data.ContentRepository
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * All playback: bundled reviewed recordings, the learner's own takes, and — only when no recording
 * exists and the learner allows it — a clearly marked synthetic Korean preview via on-device TTS.
 * No network or paid service is involved.
 */
class AudioHub(private val context: Context) {

    enum class TtsState { LOADING, READY, MISSING }
    enum class Source { BUNDLED, SYNTHETIC, OWN, NONE }

    var ttsState by mutableStateOf(TtsState.LOADING)
        private set
    /** Key of what is playing now (line id, phrase id, file path), for UI highlighting. */
    var playing by mutableStateOf<String?>(null)
        private set
    /** Source of the most recent reference playback, so the UI can label synthetic audio. */
    var lastSource by mutableStateOf(Source.NONE)
        private set

    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()
    private var counter = 0

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            val t = tts
            ttsState = if (status == TextToSpeech.SUCCESS && t != null) {
                val r = t.setLanguage(Locale.KOREAN)
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) TtsState.MISSING
                else TtsState.READY
            } else TtsState.MISSING
            t?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = finish(utteranceId, true)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish(utteranceId, false)
                override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId, false)
            })
        }
    }

    private fun finish(id: String?, ok: Boolean) {
        val c = id?.let { pending.remove(it) } ?: return
        if (c.isActive) c.resume(ok)
    }

    fun hasBundled(path: String?) = ContentRepository.hasAudio(context, path)

    /** What would play for this reference right now. */
    fun sourceFor(audio: String?, allowSynthetic: Boolean): Source = when {
        hasBundled(audio) -> Source.BUNDLED
        allowSynthetic && ttsState == TtsState.READY -> Source.SYNTHETIC
        else -> Source.NONE
    }

    /**
     * Plays a Korean reference: the bundled recording if present, else the synthetic preview.
     * Suspends until playback ends. Returns the source used (NONE = nothing could play).
     */
    suspend fun playReference(
        key: String, ko: String, audio: String?, allowSynthetic: Boolean,
        pitch: Float = 1f, rate: Float = 1f,
    ): Source {
        stop()
        val source = sourceFor(audio, allowSynthetic)
        lastSource = source
        playing = key
        try {
            when (source) {
                Source.BUNDLED -> if (!playAsset("audio/$audio")) return Source.NONE
                Source.SYNTHETIC -> if (!speak(ko, pitch, rate)) return Source.NONE
                else -> return Source.NONE
            }
        } finally {
            if (playing == key) playing = null
        }
        return source
    }

    /** Plays one of the learner's own recordings. */
    suspend fun playFile(file: File): Boolean {
        stop()
        if (!file.exists()) return false
        playing = file.absolutePath
        try {
            return playWith { mp -> mp.setDataSource(file.absolutePath) }
        } finally {
            if (playing == file.absolutePath) playing = null
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { tts?.stop() }
        pending.keys.toList().forEach { finish(it, false) }
        playing = null
    }

    fun release() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    private suspend fun playAsset(path: String): Boolean = playWith { mp ->
        context.assets.openFd(path).use { afd -> mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
    }

    private suspend fun playWith(setup: (MediaPlayer) -> Unit): Boolean = suspendCancellableCoroutine { cont ->
        val mp = MediaPlayer()
        var done = false
        fun end(ok: Boolean) {
            if (done) return
            done = true
            runCatching { mp.release() }
            if (player === mp) player = null
            if (cont.isActive) cont.resume(ok)
        }
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            setup(mp)
            mp.setOnCompletionListener { end(true) }
            mp.setOnErrorListener { _, _, _ -> end(false); true }
            mp.prepare()
            player = mp
            mp.start()
        } catch (e: Exception) {
            end(false)
        }
        cont.invokeOnCancellation { runCatching { mp.stop() }; end(false) }
    }

    private suspend fun speak(text: String, pitch: Float, rate: Float): Boolean = suspendCancellableCoroutine { cont ->
        val t = tts
        if (t == null || ttsState != TtsState.READY) { cont.resume(false); return@suspendCancellableCoroutine }
        val id = "u${counter++}"
        pending[id] = cont
        t.setPitch(pitch)
        t.setSpeechRate(rate)
        if (t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id, false)
        cont.invokeOnCancellation { pending.remove(id); runCatching { t.stop() } }
    }
}
