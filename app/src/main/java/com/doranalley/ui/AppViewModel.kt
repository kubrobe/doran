package com.doranalley.ui

import android.app.Application
import android.provider.Settings.Global
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doranalley.audio.AudioHub
import com.doranalley.audio.KoRecognizer
import com.doranalley.audio.Recorder
import com.doranalley.core.DiaryEntry
import com.doranalley.core.Engine
import com.doranalley.core.Episode
import com.doranalley.core.Evidence
import com.doranalley.core.Help
import com.doranalley.core.InputMode
import com.doranalley.core.ItemContext
import com.doranalley.core.KoText
import com.doranalley.core.MatchResult
import com.doranalley.core.Meta
import com.doranalley.core.Node
import com.doranalley.core.Progress
import com.doranalley.core.ReviewConfig
import com.doranalley.core.ReviewItem
import com.doranalley.core.Scheduler
import com.doranalley.core.Settings
import com.doranalley.core.TranscriptEntry
import com.doranalley.core.TurnOutcome
import com.doranalley.core.VocabSpec
import com.doranalley.data.Content
import com.doranalley.data.ContentRepository
import com.doranalley.data.Store
import com.doranalley.score.Acoustic
import com.doranalley.score.Hangul
import com.doranalley.score.Wav
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class Screen {
    data object Home : Screen()
    data class Play(val episodeId: String) : Screen()
    data class Mirror(val episodeId: String) : Screen()
    data object Recall : Screen()
    data object Quest : Screen()
    data object Diary : Screen()
    data object Settings : Screen()
}

/** State of the learner's current turn in an episode. */
sealed class TurnUi {
    data object Idle : TurnUi()
    data object Listening : TurnUi()
    data object Recording : TurnUi()
    /** Recognizer output waiting for the learner to confirm what the app heard. */
    data class Heard(val heard: String, val result: MatchResult) : TurnUi()
    /** Learner tells the app what he meant: after recording (self-confirmed) or without the mic (listening only). */
    data class Choose(val take: String?, val listeningOnly: Boolean, val heard: String?) : TurnUi()
}

/** One-shot capture used by recall cards, the mirror and the diary. */
data class Captured(val key: String, val alternatives: List<String>, val file: String?, val durationMs: Long)

/** What a take should be rated against: the Korean phrase and its model recording. */
data class RateTarget(val ko: String, val audio: String?, val allowExtra: Boolean = false)

/**
 * Pronunciation feedback for one capture key.
 * [sound] compares the recording with the model's sound; [text] compares what the recognizer heard with the phrase.
 */
data class Rating(
    val key: String,
    val target: RateTarget,
    val sound: Acoustic.Result? = null,
    val soundBusy: Boolean = false,
    /** True when there was no model sound to compare with (no recording and no Korean voice). */
    val noModel: Boolean = false,
    val text: Hangul.Result? = null,
    val confidence: Float? = null,
)

/** A request to open the phone's own speech-input screen (used when in-app recognition fails). */
data class SystemAsk(val key: String, val id: Long = System.nanoTime())

const val TURN_KEY = "turn"

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val content: Content = ContentRepository.load(app)
    private val store = Store(app)
    val audio = AudioHub(app)
    val recorder = Recorder()
    val recognizer = KoRecognizer(app)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(1)

    var settings by mutableStateOf(store.loadSettings()); private set
    var meta by mutableStateOf(store.loadMeta()); private set
    var review by mutableStateOf(store.loadReview()); private set
    var outcomesLog by mutableStateOf(store.loadOutcomes()); private set
    var diary by mutableStateOf(store.loadDiary()); private set

    var screen by mutableStateOf<Screen>(Screen.Home)

    // ----- episode play state -----
    var engine by mutableStateOf<Engine?>(null); private set
    var progress by mutableStateOf<Progress?>(null); private set
    var turnUi by mutableStateOf<TurnUi>(TurnUi.Idle); private set
    var awaitingTap by mutableStateOf(false); private set
    private var autoJob: Job? = null

    // ----- device capabilities -----
    var micGranted by mutableStateOf(false)
    var micDenied by mutableStateOf(false)
    /** Set when the recognizer failed in a way that means Korean recognition is not usable on this device. */
    var recognitionBroken by mutableStateOf(false); private set
    var notice by mutableStateOf<String?>(null)

    // ----- capture (recall / mirror / diary) -----
    var capturing by mutableStateOf<String?>(null); private set
    var captured by mutableStateOf<Captured?>(null); private set
    private var captureTarget: RateTarget? = null
    /** Pronunciation feedback per capture key. */
    var ratings by mutableStateOf<Map<String, Rating>>(emptyMap()); private set

    // ----- fallbacks -----
    /** Set to open the phone's speech-input screen; MainActivity launches it and reports back. */
    var systemAsk by mutableStateOf<SystemAsk?>(null); private set
    private var systemDialogFailed = false
    /** Runs once the microphone permission is granted (so one tap is enough). */
    private var pendingMic: (() -> Unit)? = null

    init {
        recorder.onAutoStop = {
            when {
                turnUi == TurnUi.Recording -> finishSpeaking()
                capturing != null -> stopCapture()
                else -> {}
            }
        }
        // Reopening mid-lesson returns straight to the scene.
        meta.activeEpisode?.let { id ->
            val p = store.loadProgress(id)
            if (p != null && !p.finished && content.episodes.containsKey(id)) openEpisode(id)
        }
    }

    // ================= basics =================

    fun now(): Long = System.currentTimeMillis() + settings.timeOffsetHours * 3_600_000L
    val lang get() = settings.lang
    val reviewConfig: ReviewConfig
        get() = settings.intervalsHours?.let { content.config.review.copy(intervalsHours = it) } ?: content.config.review

    val reducedMotion: Boolean
        get() = settings.reducedMotion ?: runCatching {
            Global.getFloat(getApplication<Application>().contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)

    private fun persist(block: Store.() -> Unit) { viewModelScope.launch(io) { store.block() } }

    fun updateSettings(f: (Settings) -> Settings) {
        settings = f(settings)
        val s = settings
        persist { saveSettings(s) }
    }

    private fun updateMeta(f: (Meta) -> Meta) {
        meta = f(meta)
        val m = meta
        persist { saveMeta(m) }
    }

    private fun updateReview(items: Map<String, ReviewItem>) {
        review = items
        persist { saveReview(items) }
    }

    fun setLang(code: String) = updateSettings { it.copy(lang = code) }

    fun go(s: Screen) {
        stopAllAudio()
        if (s !is Screen.Play) { autoJob?.cancel(); awaitingTap = false }
        screen = s
    }

    fun back() {
        when (screen) {
            Screen.Home -> {}
            is Screen.Play -> { go(Screen.Home) }
            else -> go(Screen.Home)
        }
    }

    fun stopAllAudio() {
        audio.stop()
        if (recorder.isRecording) recorder.cancel()
        recognizer.destroy()
        capturing = null
        if (turnUi == TurnUi.Listening || turnUi == TurnUi.Recording) turnUi = TurnUi.Idle
    }

    // ================= microphone permission =================

    /** Asks for the microphone and remembers what to do once it is allowed. */
    private fun needMic(requestPermission: () -> Unit, then: () -> Unit) {
        pendingMic = then
        requestPermission()
    }

    /** Called by MainActivity with the permission result. */
    fun onMicPermission(granted: Boolean) {
        micGranted = granted
        micDenied = !granted
        val then = pendingMic
        pendingMic = null
        if (!granted) { notice = "notice_mic_denied"; return }
        // Let the permission dialog close and the screen resume before opening the microphone.
        if (then != null) viewModelScope.launch { delay(400); then() }
    }

    /** App went to the background: stop sound and the microphone, pause the scene. */
    fun onPause() {
        autoJob?.cancel()
        stopAllAudio()
    }

    /** Back in the foreground: pick the scene up where it stopped. */
    fun onResume() {
        // Back from installing a Korean voice: look again.
        if (audio.ttsState == AudioHub.TtsState.MISSING) audio.retryTts()
        if (screen is Screen.Play && progress?.finished == false) runAuto()
    }

    // ================= episodes =================

    fun episodeProgress(id: String): Progress? = store.loadProgress(id)

    fun openEpisode(id: String, restart: Boolean = false) {
        val ep = content.episodes[id] ?: return
        val e = Engine(ep)
        val saved = if (restart) null else store.loadProgress(id)?.takeIf { !it.finished }
        engine = e
        progress = saved ?: e.start(now()).also { p -> persist { saveProgress(p) } }
        turnUi = TurnUi.Idle
        awaitingTap = false
        updateMeta { it.copy(activeEpisode = id) }
        screen = Screen.Play(id)
        runAuto()
    }

    fun episodeFor(p: Progress?): Episode? = p?.let { content.episodes[it.episodeId] }

    private fun commit(p: Progress) {
        progress = p
        persist { saveProgress(p) }
    }

    /** Plays narration, lines and walks until the learner's next turn (or the end). */
    private fun runAuto() {
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            while (true) {
                val e = engine ?: break
                val p = progress ?: break
                when (val node = e.current(p)) {
                    is Node.Narration -> { awaitingTap = true; break }
                    is Node.Line -> {
                        commit(e.step(p, now()))
                        introduceFrom(node, e.episode)
                        speakLine(node)
                        delay(450)
                    }
                    is Node.Move -> {
                        commit(e.step(p, now()))
                        delay(if (reducedMotion) 250 else 1400)
                    }
                    is Node.End -> {
                        if (!p.finished) { commit(e.step(p, now())); onFinished(e.episode, node) }
                        break
                    }
                    is Node.Turn -> break
                }
            }
        }
    }

    fun continueNarration() {
        val e = engine ?: return
        val p = progress ?: return
        if (e.current(p) !is Node.Narration) return
        awaitingTap = false
        commit(e.step(p, now()))
        runAuto()
    }

    private var warnedNoVoice = false

    private suspend fun speakLine(node: Node.Line) {
        val c = content.characters[node.speaker]
        checkVolume()
        val src = audio.playReference(node.id, node.ko, node.audio, settings.allowSynthetic, c?.pitch ?: 1f, c?.rate ?: 1f)
        if (src == AudioHub.Source.NONE) {
            if (!warnedNoVoice) { warnedNoVoice = true; notice = noVoiceNotice() }
            delay(1200L + 70L * node.ko.length) // time to read when no voice is available
        }
    }

    private fun noVoiceNotice() = if (!settings.allowSynthetic) "notice_voice_off" else "notice_no_voice"

    private var warnedMuted = false

    /** Media volume at zero is the most common "no sound" cause; say so once per session. */
    private fun checkVolume() {
        if (audio.isMuted() && !warnedMuted) { warnedMuted = true; notice = "notice_volume_off" }
    }

    /** Plays something the learner tapped; tells him why if nothing could play. */
    private fun playTapped(key: String, ko: String, audioPath: String?, pitch: Float = 1f, rate: Float = 1f) {
        if (audio.isMuted()) notice = "notice_volume_off"
        viewModelScope.launch {
            val src = audio.playReference(key, ko, audioPath, settings.allowSynthetic, pitch, rate)
            if (src == AudioHub.Source.NONE) notice = noVoiceNotice()
        }
    }

    fun replayEntry(entry: TranscriptEntry) {
        val ko = entry.ko ?: return
        val c = entry.speaker?.let { content.characters[it] }
        playTapped("t:${entry.nodeId}", ko, entry.audio, c?.pitch ?: 1f, c?.rate ?: 1f)
    }

    fun replayLastLine() {
        progress?.transcript?.lastOrNull { it.kind == "partner" }?.let(::replayEntry)
    }

    /** Target expressions enter the review list when he first hears them, with the sentence as context. */
    private fun introduceFrom(line: Node.Line, ep: Episode) {
        val targets = content.targetVocab()
        val ids = (line.words.filter { content.vocab[it]?.target == true } + KoText.used(line.ko, targets)).toSet()
        if (ids.isEmpty()) return
        var items = review
        for (id in ids) {
            val v = content.vocab[id] ?: continue
            val ctx = ItemContext(line.ko, line.gloss, ep.id, now())
            items = items + (id to (items[id]?.let { Scheduler.addContext(it, ctx) } ?: introduce(v, ctx, "lesson", id)))
        }
        updateReview(items)
    }

    private fun introduce(v: VocabSpec, ctx: ItemContext?, source: String, id: String = v.id) =
        Scheduler.introduce(id, v.ko, v.meaning, v.audio, source, ctx, now(), reviewConfig)

    fun hint() {
        val e = engine ?: return
        val p = progress ?: return
        commit(e.hint(p, now()))
    }

    fun playModel() {
        val t = progress?.let { engine?.turn(it) } ?: return
        playTapped("model:${t.id}", t.hints.model, t.hints.modelAudio)
    }

    /** Main microphone button during a turn. */
    fun speak(requestPermission: () -> Unit) {
        audio.stop()
        when {
            settings.inputMode == InputMode.LISTEN -> chooseWithoutSpeaking()
            !micGranted -> needMic(requestPermission) { speak(requestPermission) }
            settings.inputMode == InputMode.AUTO && !recognitionBroken && recognizer.isAvailable() -> listen()
            settings.inputMode == InputMode.AUTO && !recognitionBroken && systemDialogUsable() -> askSystem(TURN_KEY)
            else -> record()
        }
    }

    private fun listen() {
        turnUi = TurnUi.Listening
        recognizer.start(
            onResult = { alts, _ -> showHeard(alts) },
            onFailure = { f ->
                turnUi = TurnUi.Idle
                handleRecognitionFailure(f, TURN_KEY, retry = { listen() }, fallback = { record() })
            },
        )
    }

    private fun showHeard(alts: List<String>) {
        val p = progress
        val e = engine
        turnUi = if (p != null && e != null) TurnUi.Heard(alts.first(), e.match(p, alts)) else TurnUi.Idle
    }

    /**
     * Recognition failed. Tries, in order: another recognition service on the phone, the phone's own
     * speech-input screen, and finally plain recording (he then confirms what he meant).
     */
    private fun handleRecognitionFailure(f: KoRecognizer.Failure, key: String, retry: () -> Unit, fallback: () -> Unit) {
        // Run the next step after the recognizer has let go of the microphone.
        fun later(step: () -> Unit) { viewModelScope.launch { delay(300); step() } }
        when (f) {
            KoRecognizer.Failure.NOTHING_HEARD -> notice = "notice_not_heard"
            KoRecognizer.Failure.NO_PERMISSION -> { micGranted = false; notice = "notice_mic_needed" }
            KoRecognizer.Failure.NETWORK -> { notice = "notice_recognition_network"; later(fallback) }
            KoRecognizer.Failure.NO_KOREAN, KoRecognizer.Failure.UNAVAILABLE -> when {
                recognizer.tryNextService() -> later(retry)
                systemDialogUsable() -> later { askSystem(key) }
                else -> {
                    recognitionBroken = true
                    notice = if (f == KoRecognizer.Failure.NO_KOREAN) "notice_recognition_no_korean" else "notice_recognition_unavailable"
                    later(fallback)
                }
            }
        }
    }

    // ----- the phone's own speech-input screen -----

    private fun systemDialogUsable(): Boolean {
        if (systemDialogFailed) return false
        val pm = getApplication<Application>().packageManager
        return runCatching {
            android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(pm) != null
        }.getOrDefault(false)
    }

    private fun askSystem(key: String) {
        if (key == TURN_KEY) turnUi = TurnUi.Listening else capturing = key
        systemAsk = SystemAsk(key)
    }

    /** MainActivity started the request; clear it so it isn't launched twice. */
    fun systemAskLaunched() { systemAsk = null }

    /**
     * Result from the phone's speech-input screen. [alternatives] null means it could not run at all,
     * empty means he cancelled or nothing was heard.
     */
    fun onSystemRecognition(key: String, alternatives: List<String>?) {
        systemAsk = null
        if (alternatives == null) {
            systemDialogFailed = true
            recognitionBroken = true
            notice = "notice_recognition_unavailable"
            if (key == TURN_KEY) { turnUi = TurnUi.Idle; record() } else { capturing = null; recordCapture(key) }
            return
        }
        if (key == TURN_KEY) {
            if (alternatives.isEmpty()) { turnUi = TurnUi.Idle; notice = "notice_not_heard" } else showHeard(alternatives)
        } else {
            capturing = null
            if (alternatives.isEmpty()) notice = "notice_not_heard" else deliverRecognized(key, alternatives, null)
        }
    }

    fun finishSpeaking() {
        when (turnUi) {
            TurnUi.Listening -> recognizer.finish()
            TurnUi.Recording -> {
                val r = recorder.stop()
                when {
                    r == null -> { turnUi = TurnUi.Idle; notice = "notice_too_short" }
                    recorder.lastTakeWasSilent() -> {
                        r.first.delete()
                        turnUi = TurnUi.Idle
                        notice = "notice_mic_silent"
                    }
                    else -> turnUi = TurnUi.Choose(r.first.absolutePath, listeningOnly = false, heard = null)
                }
            }
            else -> {}
        }
    }

    private fun record() {
        val t = progress?.let { engine?.turn(it) } ?: return
        val f = File(store.scratchDir, "reply_${t.id}_${System.currentTimeMillis()}.wav")
        if (recorder.start("turn", f, maxMs = 30_000)) turnUi = TurnUi.Recording
        else { notice = "notice_record_failed"; chooseWithoutSpeaking() }
    }

    /** Listening-only route: used when the microphone is declined or off. */
    fun chooseWithoutSpeaking() { turnUi = TurnUi.Choose(null, listeningOnly = true, heard = null) }

    /** "That's not what I said": he spoke, but picks the meaning himself (self-confirmed). */
    fun correctHeard() {
        val h = (turnUi as? TurnUi.Heard)?.heard
        turnUi = TurnUi.Choose(null, listeningOnly = false, heard = h)
    }

    fun retry() { recognizer.destroy(); if (recorder.isRecording) recorder.cancel(); turnUi = TurnUi.Idle }

    /** How clearly a recognized reply came through, against the model line and the accepted examples. */
    fun clarityFor(heard: String): Hangul.Result? {
        val t = progress?.let { engine?.turn(it) } ?: return null
        val refs = (listOf(t.hints.model) + t.intents.flatMap { it.examples }).filter { it.isNotBlank() }
        return refs.map { Hangul.compare(it, heard) }.maxByOrNull { it.score }
    }

    fun playTake(path: String) {
        if (audio.isMuted()) notice = "notice_volume_off"
        viewModelScope.launch { if (!audio.playFile(File(path))) notice = "notice_play_failed" }
    }

    /** Confirms a recognized reply. Used targets are detected in the recognized text. */
    fun confirmHeard(intentId: String) {
        val h = turnUi as? TurnUi.Heard ?: return
        val t = progress?.let { engine?.turn(it) } ?: return
        val used = KoText.used(h.heard, t.targets.mapNotNull { content.vocab[it] })
        resolve(intentId, h.heard, used, Evidence.RECOGNIZED)
    }

    /** The partner reacts to something he did not understand (natural clarification). */
    fun sendUnclear() {
        val e = engine ?: return
        val p = progress ?: return
        val (heard, ev) = when (val u = turnUi) {
            is TurnUi.Heard -> u.heard to Evidence.RECOGNIZED
            is TurnUi.Choose -> u.heard to (if (u.listeningOnly) Evidence.LISTENING_ONLY else Evidence.SELF_CONFIRMED)
            else -> null to Evidence.SELF_CONFIRMED
        }
        turnUi = TurnUi.Idle
        commit(e.noMatch(p, heard, ev, now()))
        runAuto()
    }

    fun confirmChoice(intentId: String, used: Set<String>) {
        val c = turnUi as? TurnUi.Choose ?: return
        resolve(intentId, c.heard, if (c.listeningOnly) emptySet() else used,
            if (c.listeningOnly) Evidence.LISTENING_ONLY else Evidence.SELF_CONFIRMED)
    }

    private fun resolve(intentId: String, heard: String?, used: Set<String>, ev: Evidence) {
        val e = engine ?: return
        val p = progress ?: return
        val (np, outcome) = e.resolve(p, intentId, heard, used, ev, now())
        turnUi = TurnUi.Idle
        commit(np)
        applyOutcome(outcome)
        runAuto()
    }

    /** Communication success and vocabulary recall are recorded separately. */
    private fun applyOutcome(o: TurnOutcome) {
        val log = outcomesLog + o
        outcomesLog = log
        persist { saveOutcomes(log) }
        val ev = Evidence.of(o.evidence)
        var items = review
        for (t in o.targets) {
            val v = content.vocab[t] ?: continue
            val item = items[t] ?: introduce(v, null, "lesson")
            items = items + (t to Scheduler.record(item, now(), o.help, t in o.used, ev, o.episodeId, reviewConfig))
        }
        updateReview(items)
    }

    private fun onFinished(ep: Episode, end: Node.End) {
        updateMeta { m ->
            m.copy(
                activeEpisode = null,
                completed = (m.completed + (ep.parent ?: ep.id)).distinct(),
                unlocks = (m.unlocks + end.unlocks).distinct(),
                recallVariantsDone = if (ep.parent != null) (m.recallVariantsDone + ep.id).distinct() else m.recallVariantsDone,
            )
        }
    }

    // ================= words =================

    fun reviewIdFor(vocabId: String) = if (content.vocab[vocabId]?.target == true) vocabId else "saved:$vocabId"

    fun isSaved(vocabId: String) = review.containsKey(reviewIdFor(vocabId))

    /** Saves a word he tapped after the exchange, together with the sentence he heard. */
    fun saveWord(vocabId: String, entry: TranscriptEntry) {
        val v = content.vocab[vocabId] ?: return
        val id = reviewIdFor(vocabId)
        val ctx = ItemContext(entry.ko ?: v.ko, entry.text, progress?.episodeId ?: "", now())
        val item = review[id]?.let { Scheduler.addContext(it, ctx) } ?: introduce(v, ctx, "saved", id)
        updateReview(review + (id to item))
        notice = "notice_saved"
    }

    fun playKo(key: String, ko: String, audioPath: String? = null, slow: Boolean = false) {
        playTapped(key, ko, audioPath, rate = if (slow) 0.7f else 1f)
    }

    // ================= recall & quest =================

    fun dueItems(): List<ReviewItem> = Scheduler.due(review.values, now())

    fun nextDue(): ReviewItem? = review.values.filter { !Scheduler.isDue(it, now()) }.minByOrNull { it.nextReview }

    /** Five-minute quest: due items first, then the ones practiced longest ago. */
    fun questItems(): List<ReviewItem> {
        val due = dueItems()
        val rest = review.values.filter { it !in due }.sortedBy { it.lastPracticed ?: it.introducedAt }
        return (due + rest).take(3)
    }

    fun recordRecall(itemId: String, help: Int, produced: Boolean, ev: Evidence, setting: String) {
        val item = review[itemId] ?: return
        updateReview(review + (itemId to Scheduler.record(item, now(), help, produced, ev, setting, reviewConfig)))
    }

    /** Next-day twist: a recall variant of a finished episode, rotating through settings. */
    fun twistEpisode(): Episode? {
        val parent = meta.completed.firstOrNull { content.recallVariants(it).isNotEmpty() } ?: return null
        val variants = content.recallVariants(parent).sortedBy { it.id }
        val fresh = variants.filter { it.id !in meta.recallVariantsDone }
        val pool = fresh.ifEmpty { variants }
        return pool[((now() / 86_400_000L) % pool.size).toInt()]
    }

    // ================= capture for recall / mirror / diary =================

    fun canRecognize() = micGranted && settings.inputMode == InputMode.AUTO && !recognitionBroken &&
        (recognizer.isAvailable() || systemDialogUsable())

    /**
     * Captures one attempt for a recall card, the mirror or the diary.
     * With [recognize] it tries Korean recognition first and falls back to recording.
     * With a [rate] target the attempt is scored for pronunciation as soon as it ends.
     */
    fun startCapture(key: String, recognize: Boolean, requestPermission: () -> Unit, rate: RateTarget? = null) {
        audio.stop()
        if (!micGranted) { needMic(requestPermission) { startCapture(key, recognize, requestPermission, rate) }; return }
        // A recognizer check on the mirror keeps the recorded take; a fresh attempt elsewhere starts clean.
        if (captured?.key != key || !recognize) captured = null
        captureTarget = rate
        if (recognize && canRecognize()) {
            if (recognizer.isAvailable()) listenCapture(key) else askSystem(key)
        } else recordCapture(key)
    }

    private fun listenCapture(key: String) {
        capturing = key
        recognizer.start(
            onResult = { alts, conf -> capturing = null; deliverRecognized(key, alts, conf) },
            onFailure = { f ->
                capturing = null
                handleRecognitionFailure(f, key, retry = { listenCapture(key) }, fallback = { recordCapture(key) })
            },
        )
    }

    private fun deliverRecognized(key: String, alts: List<String>, conf: Float?) {
        val keepFile = captured?.takeIf { it.key == key }
        captured = Captured(key, alts, keepFile?.file, keepFile?.durationMs ?: 0)
        captureTarget?.let { t ->
            val r = Hangul.best(t.ko, alts, t.allowExtra)
            val old = ratings[key]?.takeIf { it.target == t }
            ratings = ratings + (key to (old ?: Rating(key, t)).copy(text = r, confidence = conf))
        }
    }

    private fun recordCapture(key: String) {
        val f = File(store.scratchDir, "${key.replace(Regex("[^A-Za-z0-9_]"), "_")}_${System.currentTimeMillis()}.wav")
        val max = if (key.startsWith("diary")) 180_000L else 30_000L
        if (recorder.start(key, f, maxMs = max)) capturing = key else notice = "notice_record_failed"
    }

    fun stopCapture() {
        val key = capturing ?: return
        if (recognizer.listening) { recognizer.finish(); return }
        if (!recorder.isRecording) return // the phone's speech screen is open; it reports back on its own
        val r = recorder.stop()
        capturing = null
        when {
            r == null -> notice = "notice_too_short"
            recorder.lastTakeWasSilent() -> { r.first.delete(); notice = "notice_mic_silent" }
            else -> {
                captured = Captured(key, emptyList(), r.first.absolutePath, r.second)
                captureTarget?.let { rateSound(key, it, r.first) }
            }
        }
    }

    /** Scores a recorded take against the model's sound, off the main thread. */
    private fun rateSound(key: String, target: RateTarget, take: File) {
        ratings = ratings + (key to Rating(key, target, soundBusy = true))
        viewModelScope.launch {
            val ref = audio.referencePcm(target.ko, target.audio, settings.allowSynthetic)
            val result = if (ref == null) null else withContext(Dispatchers.Default) {
                runCatching { Acoustic.compare(Wav.read(take), ref, target.ko.count { it in '\uAC00'..'\uD7A3' }) }.getOrNull()
            }
            val cur = ratings[key] ?: return@launch
            if (cur.target != target) return@launch
            ratings = ratings + (key to cur.copy(sound = result, soundBusy = false, noModel = ref == null))
        }
    }

    fun clearCapture(deleteFile: Boolean = true) {
        captured?.let { c ->
            c.file?.let { if (deleteFile && it.startsWith(store.scratchDir.absolutePath)) File(it).delete() }
            ratings = ratings - c.key
        }
        captured = null
    }

    /** Device check: give recognition another chance after it was switched off. */
    fun resetRecognition() {
        recognitionBroken = false
        systemDialogFailed = false
    }

    /** Clears the score for a key (e.g. when a new attempt starts). */
    fun clearRating(key: String) { ratings = ratings - key }

    // ================= mirror =================

    fun ladder(phraseId: String) = meta.ladder[phraseId] ?: 0

    fun markLadder(phraseId: String, step: Int) {
        if (ladder(phraseId) >= step) return
        updateMeta { it.copy(ladder = it.ladder + (phraseId to step)) }
    }

    /** A/B: the reference, then his own take. */
    fun playAB(key: String, ko: String, audioPath: String?, take: String) {
        if (audio.isMuted()) notice = "notice_volume_off"
        viewModelScope.launch {
            when (audio.playReference(key, ko, audioPath, settings.allowSynthetic)) {
                AudioHub.Source.INTERRUPTED -> return@launch
                AudioHub.Source.NONE -> notice = noVoiceNotice()
                else -> {}
            }
            delay(350)
            audio.playFile(File(take))
        }
    }

    // ================= diary =================

    /** Saves the current capture as a diary entry. Takes are only kept when he chooses. */
    fun saveCaptureToDiary(kind: String, promptId: String?, phraseKo: String?) {
        val c = captured ?: return
        val src = c.file?.let(::File) ?: return
        val id = UUID.randomUUID().toString()
        val name = "$id." + src.extension.ifBlank { "wav" }
        val dst = store.diaryFile(name)
        if (!src.renameTo(dst)) { src.copyTo(dst, overwrite = true); src.delete() }
        val entry = DiaryEntry(id, now(), kind, promptId, phraseKo, name, c.durationMs)
        val list = diary + entry
        diary = list
        captured = null
        persist { saveDiary(list) }
        notice = "notice_saved_diary"
    }

    fun deleteDiary(entry: DiaryEntry) {
        store.diaryFile(entry.file).delete()
        val list = diary - entry
        diary = list
        persist { saveDiary(list) }
    }

    fun diaryPath(entry: DiaryEntry) = store.diaryFile(entry.file)

    fun playDiary(entry: DiaryEntry) { viewModelScope.launch { audio.playFile(diaryPath(entry)) } }

    /** Side by side: baseline, then the latest weekly retell. */
    fun playCompare(a: DiaryEntry, b: DiaryEntry) {
        viewModelScope.launch {
            audio.playFile(diaryPath(a))
            delay(600)
            audio.playFile(diaryPath(b))
        }
    }

    fun independentExpressions(): List<ReviewItem> = review.values.filter { it.independentDays.isNotEmpty() }
    fun clarificationCount(): Int = outcomesLog.count { it.clarification }
    fun goalsReached(): Int = outcomesLog.count { it.success }

    // ================= settings / test tools =================

    fun shiftTime(hours: Long) = updateSettings { it.copy(timeOffsetHours = it.timeOffsetHours + hours) }
    fun resetTime() = updateSettings { it.copy(timeOffsetHours = 0) }

    fun resetProgress(keepDiary: Boolean) {
        stopAllAudio()
        autoJob?.cancel()
        engine = null
        progress = null
        viewModelScope.launch(io) { store.resetProgress(keepDiary) }
        meta = Meta()
        review = emptyMap()
        outcomesLog = emptyList()
        if (!keepDiary) diary = emptyList()
        screen = Screen.Home
    }

    override fun onCleared() {
        stopAllAudio()
        audio.release()
        recognizer.destroy()
        super.onCleared()
    }
}
