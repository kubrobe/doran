package com.doranalley.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Korean speech recognition through the device's own recognizer (free, no API key).
 * Whether Korean is actually supported is only known after a first attempt, so errors are
 * reported back and the app falls back to "record, then confirm what you meant".
 * Must be used from the main thread.
 */
class KoRecognizer(private val context: Context) {

    enum class Failure { NOTHING_HEARD, UNAVAILABLE, NO_PERMISSION }

    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var level by mutableFloatStateOf(0f)
        private set

    private var sr: SpeechRecognizer? = null

    fun isAvailable(): Boolean = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    fun start(onResult: (List<String>) -> Unit, onFailure: (Failure) -> Unit) {
        destroy()
        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (recognizer == null) { onFailure(Failure.UNAVAILABLE); return }
        sr = recognizer
        partial = ""
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                listening = false
                val alts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }
                if (alts.isEmpty()) onFailure(Failure.NOTHING_HEARD) else onResult(alts)
            }
            override fun onError(error: Int) {
                listening = false
                onFailure(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Failure.NOTHING_HEARD
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Failure.NO_PERMISSION
                        // Network, server, busy, client, and (API 31+) language not supported/unavailable (12, 13).
                        else -> Failure.UNAVAILABLE
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
            // Give him time to finish speaking before anything is judged.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
        }
        try {
            listening = true
            recognizer.startListening(intent)
        } catch (e: Exception) {
            listening = false
            onFailure(Failure.UNAVAILABLE)
        }
    }

    /** Ends listening; results still arrive through the callback. */
    fun finish() { runCatching { sr?.stopListening() } }

    fun destroy() {
        runCatching { sr?.cancel() }
        runCatching { sr?.destroy() }
        sr = null
        listening = false
        partial = ""
    }
}
