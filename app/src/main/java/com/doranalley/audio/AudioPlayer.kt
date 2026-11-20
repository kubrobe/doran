package com.doranalley.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.doranalley.core.Hangul
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Plays the bundled Korean recordings (assets/audio/<key>.mp3, slow: <key>_s.mp3),
 * the learner's own takes, and short sound effects. Call from the main thread.
 */
class AudioPlayer(context: Context) {
    private val app = context.applicationContext
    private val assets = app.assets
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val available: Set<String> = runCatching { assets.list("audio")?.toSet() }.getOrNull() ?: emptySet()

    private var player: MediaPlayer? = null
    private val _playing = MutableStateFlow<String?>(null)
    /** Id of what is playing right now ("ko:<text>", "ko-slow:<text>" or "take"), for button animations. */
    val playing: StateFlow<String?> = _playing.asStateFlow()

    private val _problems = MutableSharedFlow<Problem>(extraBufferCapacity = 4)
    val problems = _problems.asSharedFlow()

    enum class Problem { MUTED, MISSING, FAILED }

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val sfx = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val sfxIds = mutableMapOf<Sfx, Int>()

    enum class Sfx(val file: String) { CORRECT("sfx/correct.wav"), WRONG("sfx/wrong.wav"), COMPLETE("sfx/complete.wav") }

    init {
        Sfx.entries.forEach { s ->
            runCatching { assets.openFd(s.file).use { fd -> sfxIds[s] = sfx.load(fd, 1) } }
                .onFailure { Log.w(TAG, "sfx ${s.file}", it) }
        }
    }

    fun hasAudio(text: String) = "${Hangul.audioKey(text)}.mp3" in available

    fun isMuted() = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 }.getOrDefault(false)

    /** Plays the bundled recording for [text]. Returns false if there is no recording. */
    fun play(text: String, slow: Boolean = false): Boolean {
        val key = Hangul.audioKey(text)
        val name = if (slow && "${key}_s.mp3" in available) "${key}_s.mp3" else "$key.mp3"
        if (name !in available) {
            Log.w(TAG, "no audio for '$text' ($name)")
            _problems.tryEmit(Problem.MISSING)
            return false
        }
        if (isMuted()) _problems.tryEmit(Problem.MUTED)
        val id = (if (slow) "ko-slow:" else "ko:") + text
        return start(id) { mp ->
            assets.openFd("audio/$name").use { fd -> mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
        }
    }

    fun playFile(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        if (isMuted()) _problems.tryEmit(Problem.MUTED)
        return start("take") { mp -> mp.setDataSource(file.absolutePath) }
    }

    private fun start(id: String, source: (MediaPlayer) -> Unit): Boolean {
        stop()
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attrs)
            source(mp)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { done(it) }
            mp.setOnErrorListener { p, what, extra ->
                Log.e(TAG, "playback error $what/$extra for $id")
                _problems.tryEmit(Problem.FAILED)
                done(p); true
            }
            player = mp
            _playing.value = id
            mp.prepareAsync()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "cannot play $id", t)
            runCatching { mp.release() }
            if (player === mp) player = null
            _playing.value = null
            _problems.tryEmit(Problem.FAILED)
            false
        }
    }

    private fun done(mp: MediaPlayer) {
        runCatching { mp.release() }
        if (player === mp) {
            player = null
            _playing.value = null
        }
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
        _playing.value = null
    }

    fun effect(s: Sfx) {
        val id = sfxIds[s] ?: return
        runCatching { sfx.play(id, 0.6f, 0.6f, 1, 0, 1f) }
    }

    fun release() {
        stop()
        runCatching { sfx.release() }
    }

    companion object { private const val TAG = "AudioPlayer" }
}
