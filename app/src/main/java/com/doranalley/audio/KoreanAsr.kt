package com.doranalley.audio

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Offline Korean speech recognition (sherpa-onnx zipformer, bundled in assets/asr).
 * Runs on one background thread; nothing leaves the phone.
 */
class KoreanAsr(context: Context) {
    private val app = context.applicationContext
    private val thread = Executors.newSingleThreadExecutor { r -> Thread(r, "korean-asr").apply { priority = Thread.NORM_PRIORITY } }
    private val dispatcher = thread.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var recognizer: OfflineRecognizer? = null

    sealed interface State {
        data object Loading : State
        data object Ready : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    fun warmUp() {
        scope.launch { ensureLoaded() }
    }

    private fun ensureLoaded(): OfflineRecognizer? {
        recognizer?.let { return it }
        if (_state.value is State.Failed) return null
        return try {
            val t0 = System.currentTimeMillis()
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = "asr/encoder.int8.onnx",
                        decoder = "asr/decoder.int8.onnx",
                        joiner = "asr/joiner.int8.onnx",
                    ),
                    tokens = "asr/tokens.txt",
                    numThreads = 2,
                    debug = false,
                ),
                decodingMethod = "greedy_search",
            )
            val r = OfflineRecognizer(app.assets, config)
            recognizer = r
            _state.value = State.Ready
            Log.i(TAG, "recognizer ready in ${System.currentTimeMillis() - t0} ms")
            r
        } catch (t: Throwable) {
            Log.e(TAG, "recognizer failed to load", t)
            _state.value = State.Failed(t.message ?: t.javaClass.simpleName)
            null
        }
    }

    /** Returns the recognized text, or null if the recognizer is unavailable. */
    suspend fun recognize(samples: FloatArray, sampleRate: Int = 16000): String? = withContext(dispatcher) {
        val r = ensureLoaded() ?: return@withContext null
        try {
            // A little silence on both sides helps the model with very short answers.
            val padded = FloatArray(samples.size + sampleRate * 3 / 4)
            System.arraycopy(samples, 0, padded, sampleRate / 4, samples.size)
            val stream = r.createStream()
            try {
                stream.acceptWaveform(padded, sampleRate)
                r.decode(stream)
                r.getResult(stream).text.trim()
            } finally {
                stream.release()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "recognition failed", t)
            null
        }
    }

    companion object { private const val TAG = "KoreanAsr" }
}
