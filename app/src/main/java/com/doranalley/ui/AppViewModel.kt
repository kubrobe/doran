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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val content: Content = ContentRepository.load(app)
    private val store = Store(app)
    val audio = AudioHub(app)
    val recorder = Recorder(app)
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

    init {
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

    /** App went to the background: stop sound and the microphone, pause the scene. */
    fun onPause() {
        autoJob?.cancel()
        stopAllAudio()
    }

    /** Back in the foreground: pick the scene up where it stopped. */
    fun onResume() {
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

    private suspend fun speakLine(node: Node.Line) {
        val c = content.characters[node.speaker]
        val src = audio.playReference(node.id, node.ko, node.audio, settings.allowSynthetic, c?.pitch ?: 1f, c?.rate ?: 1f)
        if (src == AudioHub.Source.NONE) delay(1200L + 70L * node.ko.length) // time to read when no voice is available
    }

    fun replayEntry(entry: TranscriptEntry) {
        val ko = entry.ko ?: return
        val c = entry.speaker?.let { content.characters[it] }
        viewModelScope.launch { audio.playReference("t:${entry.nodeId}", ko, entry.audio, settings.allowSynthetic, c?.pitch ?: 1f, c?.rate ?: 1f) }
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
        viewModelScope.launch { audio.playReference("model:${t.id}", t.hints.model, t.hints.modelAudio, settings.allowSynthetic) }
    }

    /** Main microphone button during a turn. */
    fun speak(requestPermission: () -> Unit) {
        audio.stop()
        when {
            settings.inputMode == InputMode.LISTEN -> chooseWithoutSpeaking()
            !micGranted -> requestPermission()
            settings.inputMode == InputMode.AUTO && !recognitionBroken && recognizer.isAvailable() -> listen()
            else -> record()
        }
    }

    private fun listen() {
        turnUi = TurnUi.Listening
        recognizer.start(
            onResult = { alts ->
                val p = progress
                val e = engine
                turnUi = if (p != null && e != null) TurnUi.Heard(alts.first(), e.match(p, alts)) else TurnUi.Idle
            },
            onFailure = { f ->
                turnUi = TurnUi.Idle
                when (f) {
                    KoRecognizer.Failure.NOTHING_HEARD -> notice = "notice_not_heard"
                    KoRecognizer.Failure.NO_PERMISSION -> { micGranted = false; notice = "notice_mic_needed" }
                    KoRecognizer.Failure.UNAVAILABLE -> { recognitionBroken = true; notice = "notice_recognition_unavailable" }
                }
            },
        )
    }

    fun finishSpeaking() {
        when (turnUi) {
            TurnUi.Listening -> recognizer.finish()
            TurnUi.Recording -> {
                val r = recorder.stop()
                turnUi = TurnUi.Choose(r?.first?.absolutePath, listeningOnly = false, heard = null)
            }
            else -> {}
        }
    }

    private fun record() {
        val t = progress?.let { engine?.turn(it) } ?: return
        val f = File(store.scratchDir, "reply_${t.id}_${System.currentTimeMillis()}.m4a")
        if (recorder.start("turn", f)) turnUi = TurnUi.Recording
        else { notice = "notice_record_failed"; chooseWithoutSpeaking() }
    }

    /** Listening-only route: used when the microphone is declined or off. */
    fun chooseWithoutSpeaking() { turnUi = TurnUi.Choose(null, listeningOnly = true, heard = null) }

    /** "That's not what I said": he spoke, but picks the meaning himself (self-confirmed). */
    fun correctHeard() {
        val h = (turnUi as? TurnUi.Heard)?.heard
        turnUi = TurnUi.Choose(null, listeningOnly = false, heard = h)
    }

    fun retry() { recognizer.destroy(); turnUi = TurnUi.Idle }

    fun playTake(path: String) { viewModelScope.launch { audio.playFile(File(path)) } }

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

    fun playKo(key: String, ko: String, audioPath: String? = null) {
        viewModelScope.launch { audio.playReference(key, ko, audioPath, settings.allowSynthetic) }
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

    fun canRecognize() = micGranted && settings.inputMode == InputMode.AUTO && !recognitionBroken && recognizer.isAvailable()

    fun startCapture(key: String, recognize: Boolean, requestPermission: () -> Unit) {
        audio.stop()
        if (!micGranted) { requestPermission(); return }
        captured = null
        if (recognize && canRecognize()) {
            capturing = key
            recognizer.start(
                onResult = { alts -> capturing = null; captured = Captured(key, alts, null, 0) },
                onFailure = { f ->
                    capturing = null
                    if (f == KoRecognizer.Failure.UNAVAILABLE) recognitionBroken = true
                    notice = if (f == KoRecognizer.Failure.NOTHING_HEARD) "notice_not_heard" else "notice_recognition_unavailable"
                },
            )
        } else {
            val f = File(store.scratchDir, "${key.replace(Regex("[^A-Za-z0-9_]"), "_")}_${System.currentTimeMillis()}.m4a")
            if (recorder.start(key, f)) capturing = key else notice = "notice_record_failed"
        }
    }

    fun stopCapture() {
        val key = capturing ?: return
        if (recognizer.listening) { recognizer.finish(); return }
        val r = recorder.stop()
        capturing = null
        captured = r?.let { Captured(key, emptyList(), it.first.absolutePath, it.second) }
    }

    fun clearCapture(deleteFile: Boolean = true) {
        captured?.file?.let { if (deleteFile && it.startsWith(store.scratchDir.absolutePath)) File(it).delete() }
        captured = null
    }

    // ================= mirror =================

    fun ladder(phraseId: String) = meta.ladder[phraseId] ?: 0

    fun markLadder(phraseId: String, step: Int) {
        if (ladder(phraseId) >= step) return
        updateMeta { it.copy(ladder = it.ladder + (phraseId to step)) }
    }

    /** A/B: the reference, then his own take. */
    fun playAB(key: String, ko: String, audioPath: String?, take: String) {
        viewModelScope.launch {
            audio.playReference(key, ko, audioPath, settings.allowSynthetic)
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
        val name = "$id.m4a"
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
