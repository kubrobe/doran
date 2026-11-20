package com.doranalley.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doranalley.audio.AudioPlayer
import com.doranalley.audio.KoreanAsr
import com.doranalley.audio.MicRecorder
import com.doranalley.core.Course
import com.doranalley.core.CourseUnit
import com.doranalley.core.Exercise
import com.doranalley.core.LessonBuilder
import com.doranalley.core.Progress
import com.doranalley.core.SpeechResult
import com.doranalley.data.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

enum class Tab { LEARN, PRACTICE, WORDS, PROFILE }

sealed interface Screen {
    data class Onboarding(val step: Int = 0) : Screen
    data class Home(val tab: Tab = Tab.LEARN) : Screen
    data object Lesson : Screen
    data class Complete(val summary: Summary) : Screen
    data class Streak(val days: Int) : Screen
    data object OutOfHearts : Screen
    data class Placed(val unit: CourseUnit?) : Screen
    data class Guidebook(val unit: CourseUnit) : Screen
    data object Settings : Screen
}

data class Summary(val xp: Int, val accuracy: Int, val seconds: Int, val perfect: Boolean, val title: String, val streakExtended: Int?)

enum class SessionType { LESSON, PRACTICE, PLACEMENT }

data class Feedback(val correct: Boolean, val solution: String, val note: String? = null)

data class Session(
    val type: SessionType,
    val unit: CourseUnit?,
    val lessonIndex: Int,
    val queue: List<Exercise>,
    val pos: Int = 0,
    val feedback: Feedback? = null,
    val mistakes: Int = 0,
    val graded: Int = 0,
    val combo: Int = 0,
    val requeued: Set<Int> = emptySet(),
    val startedAt: Long = System.currentTimeMillis(),
    /** Placement only: unit index of each question. */
    val placementUnits: List<Int> = emptyList(),
    val wrongInARow: Int = 0,
    /** Bumped every time an exercise is (re)shown so its UI state resets. */
    val serial: Int = 0,
) {
    val current: Exercise? get() = queue.getOrNull(pos)
    val fraction: Float get() = if (queue.isEmpty()) 0f else (pos + if (feedback != null) 1 else 0).toFloat() / queue.size
}

sealed interface SpeakState {
    data object Idle : SpeakState
    data object Recording : SpeakState
    data object Checking : SpeakState
    data class Scored(val result: SpeechResult, val take: File?, val attempt: Int) : SpeakState
    data class NotHeard(val attempt: Int) : SpeakState
    data class Error(val message: String) : SpeakState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val course: Course = store.loadCourse()
    val audio = AudioPlayer(app)
    val recorder = MicRecorder(app)
    val asr = KoreanAsr.get(app)
    private val rng = Random(System.nanoTime())
    private val builder = LessonBuilder(course, rng)

    private val _progress = MutableStateFlow(store.load())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(if (_progress.value.onboarded) Screen.Home() else Screen.Onboarding())
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _speak = MutableStateFlow<SpeakState>(SpeakState.Idle)
    val speak: StateFlow<SpeakState> = _speak.asStateFlow()

    /** Ticks every 30 s so heart refill and streak displays stay fresh. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private var recordJob: Job? = null
    private var pendingStreak: Int? = null

    init {
        asr.warmUp()
        viewModelScope.launch { while (true) { delay(30_000); _now.value = System.currentTimeMillis() } }
    }

    private fun update(f: (Progress) -> Progress) {
        val p = f(_progress.value)
        _progress.value = p
        store.save(p)
    }

    fun today(): LocalDate = LocalDate.now()

    // ------------------------------------------------------------ navigation

    fun go(s: Screen) {
        audio.stop()
        _screen.value = s
    }

    fun back(): Boolean {
        when (val s = _screen.value) {
            is Screen.Home -> if (s.tab != Tab.LEARN) { go(Screen.Home(Tab.LEARN)); return true } else return false
            is Screen.Guidebook, Screen.Settings -> { go(Screen.Home(if (s == Screen.Settings) Tab.PROFILE else Tab.LEARN)); return true }
            is Screen.Onboarding -> if (s.step > 0) { go(Screen.Onboarding(s.step - 1)); return true } else return false
            Screen.Lesson -> return false // the lesson screen shows its own "quit?" dialog
            else -> { go(Screen.Home()); return true }
        }
    }

    // ------------------------------------------------------------ onboarding

    fun setGoal(xp: Int) = update { it.copy(dailyGoal = xp) }

    fun finishOnboarding(placement: Boolean) {
        update { it.copy(onboarded = true) }
        if (placement) startPlacement() else go(Screen.Home())
    }

    // ------------------------------------------------------------ settings

    fun setTheme(mode: String) = setSettings { it.copy(theme = mode) }

    fun setSettings(f: (com.doranalley.core.Settings) -> com.doranalley.core.Settings) = update { it.copy(settings = f(it.settings)) }

    fun resetProgress() {
        update { Progress(onboarded = false, settings = com.doranalley.core.Settings(theme = it.settings.theme)) }
        _session.value = null
        go(Screen.Onboarding())
    }

    // ------------------------------------------------------------ starting sessions

    fun startLesson(unit: CourseUnit, lessonIndex: Int) {
        val now = System.currentTimeMillis()
        if (_progress.value.heartsNow(now) <= 0) { go(Screen.OutOfHearts); return }
        val q = builder.lesson(unit, lessonIndex, _progress.value.filter(now))
        begin(Session(SessionType.LESSON, unit, lessonIndex, q))
    }

    /** Returns false when there is nothing to practise yet. */
    fun startPractice(mode: LessonBuilder.PracticeMode): Boolean {
        val p = _progress.value
        val studied = p.studiedUnits(course)
        val q = builder.practice(studied, p.weakness(), mode, p.filter(System.currentTimeMillis()))
        if (q.isEmpty()) return false
        begin(Session(SessionType.PRACTICE, null, 0, q))
        return true
    }

    fun startPlacement() {
        val units = course.units
        val q = units.map { builder.placementQuestion(it) }
        begin(Session(SessionType.PLACEMENT, null, 0, q, placementUnits = units.map { it.index }))
    }

    private fun begin(s: Session) {
        _speak.value = SpeakState.Idle
        _session.value = s
        go(Screen.Lesson)
        autoplay()
    }

    private fun autoplay() {
        val ex = _session.value?.current ?: return
        ex.autoplay?.let { audio.play(it) }
    }

    fun replayCurrent(slow: Boolean = false) {
        val ex = _session.value?.current ?: return
        val text = when (ex) {
            is Exercise.Speak -> ex.ko
            is Exercise.BuildKorean -> ex.sentence.ko
            is Exercise.FillBlank -> ex.fill.full
            is Exercise.PickKorean -> ex.word.ko
            else -> ex.autoplay
        } ?: return
        audio.play(text, slow)
    }

    // ------------------------------------------------------------ answering

    /** Called by an exercise when the learner presses CHECK (or finishes a match / speaking task). */
    fun submit(correct: Boolean, note: String? = null, penalty: Boolean = true) {
        val s = _session.value ?: return
        if (s.feedback != null) return
        val ex = s.current ?: return
        val now = System.currentTimeMillis()
        val graded = ex.kind != Exercise.Kind.INTRO
        val firstTime = s.pos !in s.requeued
        if (graded && ex.kind != Exercise.Kind.MATCH && s.type != SessionType.PLACEMENT && firstTime) {
            update { it.record(ex.itemId, correct, now) }
        }
        var next = s.copy(
            feedback = Feedback(correct, ex.solution, note),
            graded = s.graded + if (graded) 1 else 0,
            mistakes = s.mistakes + if (!correct) 1 else 0,
            combo = if (correct) s.combo + 1 else 0,
        )
        if (!correct && penalty) {
            when (s.type) {
                SessionType.LESSON -> {
                    update { it.loseHeart(now) }
                    // Wrong answers come back at the end of the lesson (once).
                    if (s.pos !in s.requeued && ex !is Exercise.Speak) {
                        next = next.copy(queue = next.queue + ex, requeued = next.requeued + next.queue.size)
                    }
                }
                SessionType.PRACTICE -> if (s.pos !in s.requeued && ex !is Exercise.Speak) {
                    next = next.copy(queue = next.queue + ex, requeued = next.requeued + next.queue.size)
                }
                SessionType.PLACEMENT -> next = next.copy(wrongInARow = s.wrongInARow + 1)
            }
        } else if (s.type == SessionType.PLACEMENT) {
            next = next.copy(wrongInARow = 0)
        }
        _session.value = next
        if (_progress.value.settings.sounds && graded && (correct || penalty)) {
            audio.effect(if (correct) AudioPlayer.Sfx.CORRECT else AudioPlayer.Sfx.WRONG)
        }
    }

    /** CONTINUE after feedback (or after an intro card). */
    fun next() {
        val s = _session.value ?: return
        val ex = s.current
        if (s.feedback == null && ex !is Exercise.LetterIntro) return
        audio.stop()
        recorder.stop()
        _speak.value = SpeakState.Idle
        if (s.type == SessionType.PLACEMENT && s.feedback?.correct == false && s.wrongInARow >= 2) {
            finishPlacement(s.placementUnits[s.pos - 1].coerceAtLeast(0))
            return
        }
        if (s.type == SessionType.LESSON && _progress.value.heartsNow(System.currentTimeMillis()) <= 0) {
            _session.value = null
            go(Screen.OutOfHearts)
            return
        }
        val pos = s.pos + 1
        if (pos >= s.queue.size) { finish(s); return }
        _session.value = s.copy(pos = pos, feedback = null, serial = s.serial + 1)
        autoplay()
    }

    fun skipExercise(pause: Exercise.Kind) {
        val s = _session.value ?: return
        val now = System.currentTimeMillis()
        when (pause) {
            Exercise.Kind.SPEAK -> update { it.copy(speakingPausedUntil = now + Progress.PAUSE_MS) }
            Exercise.Kind.LISTEN -> update { it.copy(listeningPausedUntil = now + Progress.PAUSE_MS) }
            else -> {}
        }
        recorder.stop(); audio.stop()
        _speak.value = SpeakState.Idle
        // Drop the rest of that kind of exercise from this session.
        val keep = s.queue.filterIndexed { i, e -> i <= s.pos || e.kind != pause }
        val trimmed = keep.toMutableList().also { it.removeAt(s.pos) }
        if (s.pos >= trimmed.size) { finish(s.copy(queue = trimmed)); return }
        _session.value = s.copy(queue = trimmed, feedback = null, serial = s.serial + 1,
            requeued = s.requeued.filter { it < s.pos }.toSet())
        autoplay()
    }

    fun quitSession() {
        recorder.stop(); audio.stop()
        val s = _session.value
        _session.value = null
        if (s?.type == SessionType.PLACEMENT) { finishOnboardingQuit(); return }
        go(Screen.Home())
    }

    private fun finishOnboardingQuit() = go(Screen.Home())

    fun stopPlacementHere() {
        val s = _session.value ?: return
        finishPlacement(s.placementUnits.getOrElse(s.pos) { course.units.size })
    }

    private fun finishPlacement(unitIndex: Int) {
        _session.value = null
        update { it.placeAt(course, unitIndex).addXp(10, today()) }
        go(Screen.Placed(course.units.getOrNull(unitIndex)))
    }

    private fun finish(s: Session) {
        val now = System.currentTimeMillis()
        _session.value = null
        if (s.type == SessionType.PLACEMENT) { finishPlacement(course.units.size); return }
        val perfect = s.mistakes == 0
        val base = when {
            s.type == SessionType.PRACTICE -> 10
            s.unit != null && s.lessonIndex >= s.unit.lessonCount - 1 -> 15
            else -> 10
        }
        val xp = base + if (perfect) 5 else 0
        val before = _progress.value
        val extendedBefore = before.extendedToday(today())
        update { p ->
            var n = p.addXp(xp, today()).copy(
                lessonsCompleted = p.lessonsCompleted + 1,
                perfectLessons = p.perfectLessons + if (perfect) 1 else 0,
            )
            if (s.type == SessionType.LESSON && s.unit != null) n = n.completeNode(s.unit, s.lessonIndex)
            if (s.type == SessionType.PRACTICE) n = n.gainHeart(now)
            n
        }
        if (_progress.value.settings.sounds) audio.effect(AudioPlayer.Sfx.COMPLETE)
        val graded = s.graded.coerceAtLeast(1)
        val accuracy = (100 * (graded - s.mistakes).coerceAtLeast(0) / graded)
        pendingStreak = if (!extendedBefore) _progress.value.streak else null
        val title = when (s.type) {
            SessionType.PRACTICE -> "Practice complete!"
            else -> if (s.unit != null && s.lessonIndex >= s.unit.lessonCount - 1) "Unit ${s.unit.number} complete!" else "Lesson complete!"
        }
        go(Screen.Complete(Summary(xp, accuracy, ((now - s.startedAt) / 1000).toInt(), perfect, title, pendingStreak)))
    }

    fun afterComplete() {
        val st = pendingStreak
        pendingStreak = null
        if (st != null) go(Screen.Streak(st)) else go(Screen.Home())
    }

    // ------------------------------------------------------------ hearts

    fun refillForTesting() = update { it.refillHearts(System.currentTimeMillis()) }

    // ------------------------------------------------------------ speaking

    fun startSpeaking(target: String) {
        if (_speak.value == SpeakState.Recording || _speak.value == SpeakState.Checking) return
        val attempt = when (val s = _speak.value) {
            is SpeakState.Scored -> s.attempt + 1
            is SpeakState.NotHeard -> s.attempt + 1
            else -> 1
        }
        audio.stop()
        _speak.value = SpeakState.Recording
        recordJob = viewModelScope.launch {
            when (val r = recorder.record()) {
                is MicRecorder.Result.Failed -> _speak.value = SpeakState.Error(r.message())
                is MicRecorder.Result.Ok -> {
                    if (!r.heardVoice) { _speak.value = SpeakState.NotHeard(attempt); return@launch }
                    score(target, r.samples, r.file, attempt)
                }
            }
        }
    }

    private suspend fun score(target: String, samples: FloatArray, take: File?, attempt: Int) {
        _speak.value = SpeakState.Checking
        val heard = asr.recognize(samples)
        _speak.value = when {
            heard == null -> SpeakState.Error("The speech checker could not start on this phone.")
            heard.isBlank() -> SpeakState.NotHeard(attempt)
            else -> SpeakState.Scored(SpeechResult.of(target, heard), take, attempt)
        }
    }

    /** Test hook: score prepared 16 kHz audio as if the learner had just said it. */
    fun scoreForTest(target: String, samples: FloatArray) {
        recordJob = viewModelScope.launch { score(target, samples, null, 1) }
    }

    private fun MicRecorder.Result.Failed.message() = reason

    fun stopSpeaking() = recorder.stop()

    fun playTake() {
        val s = _speak.value
        if (s is SpeakState.Scored && s.take != null) audio.playFile(s.take) else audio.playFile(recorder.lastTake)
    }

    fun resetSpeak() { _speak.value = SpeakState.Idle }

    override fun onCleared() {
        recorder.stop()
        audio.release()
        super.onCleared()
    }
}
