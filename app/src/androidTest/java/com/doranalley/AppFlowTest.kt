package com.doranalley

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.doranalley.core.Exercise
import com.doranalley.core.Hangul
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.MainActivity
import com.doranalley.ui.Screen
import com.doranalley.ui.SpeakState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createEmptyComposeRule()
    @get:Rule val mic: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var vm: AppViewModel
    private val shotKinds = mutableSetOf<String>()

    @Before fun start() {
        TestUtil.clearAppData()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { vm = it.vm }
        rule.waitForIdle()
    }

    @After fun stop() = scenario.close()

    private fun tap(tag: String) {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        val node = rule.onAllNodesWithTag(tag).onFirst()
        try { node.performScrollTo() } catch (_: Throwable) { /* not in a scrollable container */ }
        node.performClick()
        rule.waitForIdle()
    }

    private fun onboard(placement: Boolean) {
        TestUtil.screenshot(rule, "onboarding_welcome")
        tap("getStarted")
        tap("goal:30"); TestUtil.screenshot(rule, "onboarding_goal")
        tap("continue")
        TestUtil.screenshot(rule, "onboarding_mic")
        tap("continue")
        TestUtil.screenshot(rule, "onboarding_level")
        tap(if (placement) "startPlacement" else "startScratch")
    }

    /** Answers the current exercise correctly (or wrongly) through the UI. */
    private fun answer(correct: Boolean, shots: Boolean = true) {
        val s = vm.session.value ?: return
        val ex = s.current ?: return
        val kind = ex.javaClass.simpleName
        val shoot = shots && shotKinds.add(kind)
        if (shoot) TestUtil.screenshot(rule, "ex_$kind")
        when (ex) {
            is Exercise.LetterIntro -> { tap("check"); return }
            is Exercise.LetterToSound -> tap("option:" + if (correct) ex.letter.rom else ex.options.first { it != ex.letter.rom })
            is Exercise.SoundToLetter -> tap("option:" + if (correct) ex.letter.ch else ex.options.first { it != ex.letter }.ch)
            is Exercise.WordMeaning -> tap("option:" + if (correct) ex.word.en else ex.options.first { it != ex.word.en })
            is Exercise.PickKorean -> tap("option:" + if (correct) ex.word.ko else ex.options.first { it != ex.word }.ko)
            is Exercise.ListenPick -> tap("option:" + if (correct) ex.ko else ex.options.first { it != ex.ko })
            is Exercise.FillBlank -> tap("option:" + if (correct) ex.fill.answer else ex.options.first { it != ex.fill.answer })
            is Exercise.MatchPairs -> {
                ex.pairs.forEachIndexed { i, (l, r) ->
                    tap("matchL:$l"); tap("matchR:$r")
                    if (shoot && i == 1) TestUtil.screenshot(rule, "ex_MatchPairs_progress")
                }
                rule.waitUntil(5_000) { vm.session.value?.feedback != null }
                tap("continue"); return
            }
            is Exercise.BuildEnglish -> {
                val words = if (correct) ex.sentence.enTiles else ex.sentence.enTiles.reversed().let { if (it == ex.sentence.enTiles) listOf(ex.tiles.first { t -> t.text !in ex.sentence.enTiles }.text) else it }
                words.forEach { tap("bank:$it") }
            }
            is Exercise.BuildKorean -> buildKo(ex.sentence.koTiles, ex.tiles.map { it.text }, correct)
            is Exercise.ListenBuild -> buildKo(ex.sentence.koTiles, ex.tiles.map { it.text }, correct)
            is Exercise.Speak -> {
                if (correct) {
                    // Feed the bundled recording through the real recognizer and the real score screen.
                    val pcm = TestUtil.decodeAsset("audio/${Hangul.audioKey(ex.ko)}.mp3")
                    vm.scoreForTest(ex.ko, pcm)
                    rule.waitUntil(60_000) { vm.speak.value is SpeakState.Scored || vm.speak.value is SpeakState.Error }
                    rule.waitForIdle()
                    if (shotKinds.add("SpeakScored")) TestUtil.screenshot(rule, "ex_Speak_scored")
                    val st = vm.speak.value
                    if (st is SpeakState.Scored && st.result.passed) {
                        rule.waitUntil(5_000) { vm.session.value?.feedback != null }
                        tap("continue"); return
                    }
                }
                tap("cantSpeak"); return
            }
        }
        if (shoot) TestUtil.screenshot(rule, "ex_${kind}_answered")
        tap("check")
        rule.waitUntil(5_000) { vm.session.value?.feedback != null }
        if (shoot || !correct) TestUtil.screenshot(rule, "feedback_${kind}_${if (correct) "right" else "wrong"}")
        assertEquals("feedback for $ex", correct, vm.session.value!!.feedback!!.correct)
        tap("continue")
    }

    private fun buildKo(answer: List<String>, tiles: List<String>, correct: Boolean) {
        val words = if (correct) answer else listOf(tiles.firstOrNull { it !in answer } ?: answer.last())
        words.forEach { tap("bank:$it") }
    }

    private fun playLesson(maxSteps: Int = 60, wrongAt: Set<Int> = emptySet()) {
        var step = 0
        while (vm.screen.value == Screen.Lesson && step < maxSteps) {
            answer(correct = step !in wrongAt)
            step++
        }
    }

    @Test fun firstLessonFromScratch() {
        onboard(placement = false)
        TestUtil.screenshot(rule, "home_path_new")
        tap("node:h1:0")
        TestUtil.screenshot(rule, "lesson_popup")
        tap("startLesson")
        playLesson()
        assertTrue(vm.screen.value is Screen.Complete)
        TestUtil.screenshot(rule, "lesson_complete")
        tap("continue")
        assertTrue(vm.screen.value is Screen.Streak)
        TestUtil.screenshot(rule, "streak")
        tap("continue")
        assertEquals(1, vm.progress.value.unitLessons["h1"])
        assertEquals(1, vm.progress.value.streak)
        TestUtil.screenshot(rule, "home_after_lesson")
    }

    @Test fun mistakesCostHeartsAndComeBack() {
        onboard(placement = false)
        tap("node:h1:0"); tap("startLesson")
        val before = vm.session.value!!.queue.size
        // Answer the 2nd and 3rd exercises wrong (index 0 is an intro card).
        playLesson(wrongAt = setOf(1, 3))
        assertTrue(vm.screen.value is Screen.Complete)
        assertEquals(3, vm.progress.value.heartsNow(System.currentTimeMillis()))
        assertTrue(before > 0)
    }

    @Test fun placementThenRegularLessonWithSpeaking() {
        onboard(placement = true)
        // Answer the Hangul and first few units right, then two wrong in a row.
        var i = 0
        while (vm.screen.value == Screen.Lesson && i < 40) {
            answer(correct = i < 9, shots = i < 9)
            i++
        }
        assertTrue("screen ${vm.screen.value}", vm.screen.value is Screen.Placed)
        TestUtil.screenshot(rule, "placement_result")
        tap("continue")
        val cur = vm.progress.value.current(vm.course)!!
        assertEquals(9, cur.first.index)
        TestUtil.screenshot(rule, "home_after_placement")
        // Play the first lesson of the placed unit (contains word banks, matching and speaking).
        vm.startLesson(cur.first, 0)
        rule.waitForIdle()
        playLesson()
        assertTrue(vm.screen.value is Screen.Complete)
        tap("continue")
    }

    @Test fun everyScreenOpens() {
        onboard(placement = false)
        tap("tabPractice"); TestUtil.screenshot(rule, "tab_practice")
        tap("practiceMixed") // nothing learned yet: shows a hint, does not crash
        TestUtil.screenshot(rule, "tab_practice_empty")
        tap("tabWords"); TestUtil.screenshot(rule, "tab_words_empty")
        tap("tabProfile"); TestUtil.screenshot(rule, "tab_profile")
        tap("openSettings"); TestUtil.screenshot(rule, "settings")
        tap("testSound")
        tap("testMic"); TestUtil.screenshot(rule, "settings_mictest")
        tap("back")
        tap("tabLearn")
        tap("guide:h1"); TestUtil.screenshot(rule, "guidebook_h1")
        tap("back")
        tap("heartsChip"); TestUtil.screenshot(rule, "hearts_dialog")
        // Unit review and later units through the view model (path is tested above).
        val p = vm.progress.value
        vm.go(Screen.Guidebook(vm.course.units[14]))
        rule.waitForIdle(); TestUtil.screenshot(rule, "guidebook_u9")
        vm.go(Screen.Home())
        rule.waitForIdle()
        assertEquals(p, vm.progress.value)
    }

    @Test fun practiceAfterLearning() {
        onboard(placement = false)
        vm.startLesson(vm.course.units[6], 0) // Greetings, lesson 1
        rule.waitForIdle()
        playLesson()
        tap("continue"); tap("continue")
        tap("tabWords"); TestUtil.screenshot(rule, "tab_words")
        tap("tabPractice"); tap("practiceMixed")
        assertEquals(Screen.Lesson, vm.screen.value)
        playLesson()
        assertTrue(vm.screen.value is Screen.Complete)
        tap("continue")
        tap("tabProfile"); TestUtil.screenshot(rule, "tab_profile_after")
    }

    @Test fun outOfHearts() {
        onboard(placement = false)
        vm.startLesson(vm.course.units[6], 0)
        rule.waitForIdle()
        var n = 0
        while (vm.screen.value == Screen.Lesson && n < 30) {
            val ex = vm.session.value?.current
            answer(correct = ex is Exercise.LetterIntro || ex is Exercise.MatchPairs || ex is Exercise.Speak, shots = false)
            n++
        }
        assertEquals(Screen.OutOfHearts, vm.screen.value)
        TestUtil.screenshot(rule, "out_of_hearts")
        tap("noThanks")
        assertTrue(vm.screen.value is Screen.Home)
    }

    @Suppress("unused") private val keep = hasTestTag("x")
    @Suppress("unused") private fun scroll() = rule.onNodeWithTag("path").performScrollToNode(hasTestTag("node:h1:0"))
}
