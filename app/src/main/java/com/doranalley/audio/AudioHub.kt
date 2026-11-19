package com.doranalley.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.doranalley.data.ContentRepository
import com.doranalley.score.Wav
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * All playback: bundled reviewed recordings, the learner's own takes, and — only when no recording
 * exists and the learner allows it — a clearly marked synthetic Korean preview via on-device TTS.
 * No network or paid service is involved.
 *
 * The Korean voice is found defensively: the default text-to-speech engine is tried first, then any
 * other installed engine that has Korean (Google, Samsung, …). Playback waits for the engine to
 * finish starting, so the first line of a scene is not silently skipped.
 */
class AudioHub(private val context: Context) {

    enum class TtsState { LOADING, READY, MISSING }
    /** INTERRUPTED = something else started playing; not a failure. */
    enum class Source { BUNDLED, SYNTHETIC, OWN, NONE, INTERRUPTED }

    var ttsState by mutableStateOf(TtsState.LOADING)
        private set
    /** Package of the engine that speaks Korean, for the device check. */
    var ttsEngine by mutableStateOf<String?>(null)
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
    /** Changes whenever playback is stopped, so an interrupted line isn't reported as a failure. */
    private var playToken = 0
    private val triedEngines = mutableSetOf<String>()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val refDir = File(context.cacheDir, "reference").apply { mkdirs() }

    private val speechAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init { initTts(null) }

    /** Starts (or restarts) the TTS search. Called again from the device check after installing a voice. */
    fun retryTts() {
        triedEngines.clear()
        runCatching { tts?.shutdown() }
        tts = null
        ttsState = TtsState.LOADING
        initTts(null)
    }

    private fun initTts(engine: String?) {
        var created: TextToSpeech? = null
        // The callback can arrive inside the constructor (on failure), so handle it on the next main-loop turn.
        val listener = TextToSpeech.OnInitListener { status -> main.post { onTtsInit(status, created, engine) } }
        created = try {
            if (engine == null) TextToSpeech(context.applicationContext, listener)
            else TextToSpeech(context.applicationContext, listener, engine)
        } catch (e: Exception) { null }
        if (created == null) ttsState = TtsState.MISSING
    }

    private fun onTtsInit(status: Int, t: TextToSpeech?, engine: String?) {
        val name = engine ?: runCatching { t?.defaultEngine }.getOrNull()
        name?.let(triedEngines::add)
        if (status == TextToSpeech.SUCCESS && t != null && setupKorean(t)) {
            tts?.takeIf { it !== t }?.let { old -> runCatching { old.shutdown() } }
            tts = t
            ttsEngine = name
            ttsState = TtsState.READY
            return
        }
        // This engine has no Korean: try the next installed engine we haven't tried yet.
        val installed = runCatching { t?.engines?.map { it.name } }.getOrNull().orEmpty()
        runCatching { t?.shutdown() }
        val next = preferred(installed).firstOrNull { it !in triedEngines }
        if (next != null) initTts(next) else ttsState = TtsState.MISSING
    }

    private fun preferred(names: List<String>): List<String> = names.sortedBy {
        when (it) {
            "com.google.android.tts" -> 0
            "com.samsung.SMT" -> 1
            else -> 2
        }
    }

    /** Selects Korean on this engine; prefers an installed (offline) voice. Returns false if there is none. */
    private fun setupKorean(t: TextToSpeech): Boolean {
        val r = runCatching { t.setLanguage(Locale.KOREA) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val langOk = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        val voices: List<Voice> = runCatching { t.voices?.toList() }.getOrNull().orEmpty()
            .filter { it.locale.language == "ko" }
        val installed = voices.filter {
            !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        val pick = installed.sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired }, { -it.quality })).firstOrNull()
        if (pick != null) runCatching { t.voice = pick }
        if (!langOk && pick == null) return false
        runCatching { t.setAudioAttributes(speechAttributes) }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finish(utteranceId, true)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId, false)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId, false)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId, false)
        })
        return true
    }

    private fun finish(id: String?, ok: Boolean) {
        val c = id?.let { pending.remove(it) } ?: return
        if (c.isActive) c.resume(ok)
    }

    /** Waits (briefly) for the voice engine to finish starting. */
    suspend fun awaitTts(timeoutMs: Long = 6_000) {
        withTimeoutOrNull(timeoutMs) { while (ttsState == TtsState.LOADING) delay(50) }
    }

    fun hasBundled(path: String?) = ContentRepository.hasAudio(context, path)

    /** True when the media volume is all the way down — a common reason for "no sound". */
    fun isMuted(): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 }.getOrDefault(false)
    }

    /** Turns the media volume up to about a third if it is at zero. */
    fun raiseVolume() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, maxOf(1, max / 3), AudioManager.FLAG_SHOW_UI)
        }
    }

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
        val token = playToken
        playing = key
        try {
            if (!hasBundled(audio) && allowSynthetic) awaitTts()
            val source = sourceFor(audio, allowSynthetic)
            lastSource = source
            val ok = when (source) {
                Source.BUNDLED -> playAsset("audio/$audio")
                Source.SYNTHETIC -> speak(ko, pitch, rate)
                else -> false
            }
            return when {
                ok -> source
                playToken != token -> Source.INTERRUPTED
                else -> Source.NONE
            }
        } finally {
            if (playing == key) playing = null
        }
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
        playToken++
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { tts?.stop() }
        // Only spoken lines ("u…"); a reference being rendered for scoring ("f…") reports on its own.
        pending.keys.filter { it.startsWith("u") }.forEach { finish(it, false) }
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
            mp.setAudioAttributes(speechAttributes)
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

    // ------------------------------------------------------------------ reference audio for scoring

    /**
     * The model's sound as PCM, for the pronunciation score: the bundled recording if present
     * (decoded), else the synthetic voice rendered to a cached WAV file. Null if neither exists.
     */
    suspend fun referencePcm(ko: String, audio: String?, allowSynthetic: Boolean): Wav.Audio? {
        if (hasBundled(audio)) {
            decodeAsset("audio/$audio")?.let { return it }
        }
        if (!allowSynthetic) return null
        awaitTts()
        val t = tts ?: return null
        if (ttsState != TtsState.READY) return null
        val f = File(refDir, "tts_${(ttsEngine ?: "x").hashCode()}_${ko.hashCode()}.wav")
        if (!f.exists() || f.length() < 1000) {
            // Rendering can be interrupted if he taps play at the same moment; try once more.
            val ok = synthesize(t, ko, f) || synthesize(t, ko, f)
            if (!ok || !f.exists()) return null
        }
        return withContext(Dispatchers.IO) { runCatching { Wav.read(f) }.getOrNull() }
    }

    private suspend fun synthesize(t: TextToSpeech, text: String, target: File): Boolean {
        val tmp = File(target.parentFile, target.name + ".part")
        val ok = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val id = "f${counter++}"
                pending[id] = cont
                t.setPitch(1f)
                t.setSpeechRate(1f)
                val r = runCatching { t.synthesizeToFile(text, null, tmp, id) }.getOrDefault(TextToSpeech.ERROR)
                if (r != TextToSpeech.SUCCESS) finish(id, false)
                cont.invokeOnCancellation { pending.remove(id) }
            }
        } ?: false
        if (ok && tmp.length() > 44) { tmp.renameTo(target); return true }
        tmp.delete()
        return false
    }

    /** Decodes a bundled recording (m4a/mp3/ogg/wav) to mono PCM. */
    private suspend fun decodeAsset(path: String): Wav.Audio? = withContext(Dispatchers.IO) {
        if (path.endsWith(".wav", ignoreCase = true)) {
            return@withContext runCatching { context.assets.open(path).use { Wav.read(it) } }.getOrNull()
        }
        runCatching {
            val ex = MediaExtractor()
            context.assets.openFd(path).use { afd -> ex.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
            try { decode(ex) } finally { ex.release() }
        }.getOrNull()
    }

    private fun decode(ex: MediaExtractor): Wav.Audio? {
        val track = (0 until ex.trackCount).firstOrNull {
            ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        ex.selectTrack(track)
        val fmt = ex.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
        var rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val out = ArrayList<Float>(rate * 4)
        try {
            codec.configure(fmt, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var guard = 0
            while (!outputDone && guard++ < 100_000) {
                if (!inputDone) {
                    val ii = codec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val buf = codec.getInputBuffer(ii)!!
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = codec.outputFormat
                    rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else if (oi >= 0) {
                    val ob = codec.getOutputBuffer(oi)!!
                    ob.position(info.offset)
                    ob.limit(info.offset + info.size)
                    val sb = ob.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val ch = maxOf(1, channels)
                    while (sb.remaining() >= ch) {
                        var s = 0f
                        repeat(ch) { s += sb.get() / 32768f }
                        out.add(s / ch)
                    }
                    codec.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
        return Wav.Audio(out.toFloatArray(), rate)
    }
}
