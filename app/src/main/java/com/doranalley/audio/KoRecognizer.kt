package com.doranalley.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Korean speech recognition through the phone's own recognizer (free, no API key).
 *
 * Phones differ a lot here, so this class is defensive:
 * - prefers Google's recognition service when installed, and tries the others if one fails;
 * - a watchdog ends a session that never starts or never answers, instead of "listening" forever;
 * - each session reports exactly once (recognizers often send an error right after the results);
 * - stopping early returns what was heard so far.
 * Must be used from the main thread.
 */
class KoRecognizer(private val context: Context) {

    enum class Failure {
        /** Nothing recognizable was said. Try again. */
        NOTHING_HEARD,
        /** Needs an internet connection (server-based recognizer). */
        NETWORK,
        /** The recognizer works but has no Korean. */
        NO_KOREAN,
        /** Microphone permission missing. */
        NO_PERMISSION,
        /** The recognizer could not be used at all. */
        UNAVAILABLE,
    }

    var listening by mutableStateOf(false)
        private set
    /** True once the recognizer says it is ready for speech. */
    var ready by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var level by mutableFloatStateOf(0f)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var sr: SpeechRecognizer? = null
    private var session = 0
    private var serviceIndex = 0

    /** Recognition services on the phone, Google's first. */
    private val services: List<ComponentName?> by lazy {
        val found = runCatching {
            context.packageManager.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
                .map { ComponentName(it.serviceInfo.packageName, it.serviceInfo.name) }
        }.getOrDefault(emptyList())
        val rank = { c: ComponentName ->
            when (c.packageName) {
                "com.google.android.googlequicksearchbox" -> 0
                "com.google.android.tts" -> 1
                else -> 2
            }
        }
        val sorted = found.sortedBy(rank)
        // null = the system default, used when the query returns nothing (package visibility, odd ROMs).
        if (sorted.isEmpty()) listOf(null) else sorted + listOf(null)
    }

    fun isAvailable(): Boolean = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    /** Name of the service in use, for the device check. */
    fun serviceName(): String = services.getOrNull(serviceIndex)?.packageName ?: "default"

    /** Moves to the next recognition service after a hard failure. Returns false when none are left. */
    fun tryNextService(): Boolean {
        if (serviceIndex + 1 >= services.size) return false
        serviceIndex++
        return true
    }

    fun start(onResult: (alternatives: List<String>, confidence: Float?) -> Unit, onFailure: (Failure) -> Unit) {
        start(onResult, onFailure, attempt = 0)
    }

    private fun start(
        onResult: (alternatives: List<String>, confidence: Float?) -> Unit,
        onFailure: (Failure) -> Unit,
        attempt: Int,
    ) {
        destroy()
        val id = ++session
        stopping = false
        var done = false
        fun fail(f: Failure) {
            if (done || id != session) return
            done = true
            main.removeCallbacksAndMessages(null)
            listening = false
            ready = false
            runCatching { sr?.cancel() }
            onFailure(f)
        }
        fun succeed(alts: List<String>, conf: Float?) {
            if (done || id != session) return
            done = true
            main.removeCallbacksAndMessages(null)
            listening = false
            ready = false
            onResult(alts, conf)
        }
        succeedFn = { a, c -> succeed(a, c) }
        failFn = { f -> fail(f) }
        val comp = services.getOrNull(serviceIndex)
        val recognizer = runCatching {
            if (comp != null) SpeechRecognizer.createSpeechRecognizer(context, comp)
            else SpeechRecognizer.createSpeechRecognizer(context)
        }.getOrNull()
        if (recognizer == null) { onFailure(Failure.UNAVAILABLE); return }
        sr = recognizer
        partial = ""
        level = 0f
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (id != session) return
                ready = true
                main.removeCallbacksAndMessages(null)
                // If it never answers, give up after a generous limit.
                main.postDelayed({ timeoutToPartial(id) }, 20_000)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { if (id == session) level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (id != session) return
                // Results should follow soon; if they don't, use what was heard so far.
                main.removeCallbacksAndMessages(null)
                main.postDelayed({ timeoutToPartial(id) }, 6_000)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (id != session) return
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }?.let { partial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val alts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }
                val conf = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()?.takeIf { it >= 0f }
                when {
                    alts.isNotEmpty() -> succeed(alts, conf)
                    partial.isNotBlank() -> succeed(listOf(partial), null)
                    else -> fail(Failure.NOTHING_HEARD)
                }
            }
            override fun onError(error: Int) {
                if (id != session || done) return
                // Some recognizers report "no match" while partial text is on screen; keep that text.
                if ((error == 6 || error == 7 || error == 5) && partial.isNotBlank()) { succeed(listOf(partial), null); return }
                // "Client"/"busy" right after a previous session usually clears on a second try.
                if ((error == 5 || error == 8) && !stopping && attempt == 0) {
                    done = true
                    main.removeCallbacksAndMessages(null)
                    main.postDelayed({ if (id == session) start(onResult, onFailure, attempt = 1) }, 400)
                    return
                }
                // He tapped Stop before saying anything.
                if (error == 5 && stopping) { fail(Failure.NOTHING_HEARD); return }
                fail(
                    when (error) {
                        6, 7 -> Failure.NOTHING_HEARD                // speech timeout, no match
                        9 -> Failure.NO_PERMISSION                   // insufficient permissions
                        1, 2, 4, 11 -> Failure.NETWORK               // network timeout, network, server, server disconnected
                        12, 13 -> Failure.NO_KOREAN                  // language not supported / unavailable
                        else -> Failure.UNAVAILABLE                  // audio, client, busy, too many requests
                    }
                )
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Give him time to finish speaking before anything is judged.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
        try {
            listening = true
            ready = false
            recognizer.startListening(intent)
            // A recognizer that never gets ready is treated as unusable, not left "listening" forever.
            main.postDelayed({ if (!ready) fail(Failure.UNAVAILABLE) }, 5_000)
        } catch (e: Exception) {
            fail(Failure.UNAVAILABLE)
        }
    }

    private var stopping = false
    private var succeedFn: ((List<String>, Float?) -> Unit)? = null
    private var failFn: ((Failure) -> Unit)? = null

    /** The recognizer went quiet: use what was heard so far, or report that nothing was heard. */
    private fun timeoutToPartial(id: Int) {
        if (id != session) return
        val p = partial
        if (p.isNotBlank()) {
            runCatching { sr?.cancel() }
            succeedFn?.invoke(listOf(p), null)
        } else failFn?.invoke(Failure.NOTHING_HEARD)
    }

    /** Ends listening. Results arrive through the callback; if they don't come, the partial text is used. */
    fun finish() {
        stopping = true
        runCatching { sr?.stopListening() }
        val id = session
        main.removeCallbacksAndMessages(null)
        main.postDelayed({ timeoutToPartial(id) }, 4_000)
    }

    fun destroy() {
        session++
        succeedFn = null
        failFn = null
        main.removeCallbacksAndMessages(null)
        runCatching { sr?.cancel() }
        runCatching { sr?.destroy() }
        sr = null
        listening = false
        ready = false
        partial = ""
        level = 0f
    }
}
