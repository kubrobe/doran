package com.doranalley

import android.Manifest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.doranalley.core.Exercise
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.MainActivity
import com.doranalley.ui.Screen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign accessibility checks: on the main screens and the first lesson, every tappable element
 * must be at least 48 x 48 dp and have something for TalkBack to read. A second pass repeats the
 * walk at the largest font scale (2.0) and saves screenshots for review. Inline word hints
 * ("hint:" tags) are text links and are exempt from the size rule.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val rule = createEmptyComposeRule()
    @get:Rule val mic: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var vm: AppViewModel
    private val problems = mutableListOf<String>()

    private fun shell(cmd: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd).close()
    }

    private fun launch(fontScale: Float) {
        shell("settings put system font_scale $fontScale")
        Thread.sleep(500)
        TestUtil.clearAppData()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity { vm = it.vm }
        rule.waitForIdle()
    }

    @After fun stop() {
        scenario?.close()
        shell("settings put system font_scale 1.0")
    }

    private fun tap(tag: String) {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        val node = rule.onAllNodesWithTag(tag).onFirst()
        try { node.performScrollTo() } catch (_: Throwable) { /* not in a scrollable container */ }
        node.performClick()
        rule.waitForIdle()
    }

    private fun label(n: SemanticsNode): String =
        listOfNotNull(
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" "),
            n.config.getOrNull(SemanticsProperties.EditableText)?.text,
        ).joinToString(" ").trim()

    /** Records every tappable node on screen that is smaller than 48 dp or has no label. */
    private fun check(screen: String) {
        rule.waitForIdle()
        val density = TestUtil.ctx.resources.displayMetrics.density
        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { n ->
            val tag = n.config.getOrNull(SemanticsProperties.TestTag)
            if (tag?.startsWith("hint:") == true) return@forEach
            val w = n.size.width / density
            val h = n.size.height / density
            val text = label(n)
            if (w < 47.5f || h < 47.5f) problems += "$screen: '${text.take(40)}' (tag=$tag) is ${w.toInt()}x${h.toInt()} dp"
            if (text.isEmpty()) problems += "$screen: tappable element without a label (tag=$tag, ${w.toInt()}x${h.toInt()} dp)"
        }
    }

    private fun walk(prefix: String, shots: Boolean) {
        fun visit(name: String) { check("$prefix$name"); if (shots) TestUtil.screenshot(rule, "$prefix$name") }
        visit("welcome")
        tap("getStarted"); visit("goal")
        tap("continue"); visit("mic")
        tap("continue"); visit("level")
        tap("startScratch"); visit("learn")
        tap("tabPractice"); visit("practice")
        tap("tabWords"); visit("words")
        tap("tabProfile"); visit("profile")
        tap("openSettings"); visit("settings")
        tap("back")
        tap("tabLearn")
        tap("node:h1:0"); tap("startLesson")
        // First exercises of the first lesson: the letter card, then answer the next ones correctly.
        var step = 0
        while (vm.screen.value == Screen.Lesson && step < 4) {
            val ex = vm.session.value?.current ?: break
            visit("lesson_${step}_${ex.javaClass.simpleName}")
            when (ex) {
                is Exercise.LetterIntro -> { tap("check"); step++; continue }
                is Exercise.LetterToSound -> tap("option:${ex.letter.rom}")
                is Exercise.SoundToLetter -> tap("option:${ex.letter.ch}")
                else -> break
            }
            tap("check")
            rule.waitUntil(5_000) { vm.session.value?.feedback != null }
            visit("lesson_${step}_feedback")
            tap("continue")
            step++
        }
    }

    @Test fun touchTargetsAndLabels() {
        launch(1.0f)
        walk("a11y_", shots = false)
        assertTrue("Accessibility problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun largestFontScale() {
        launch(2.0f)
        walk("font2x_", shots = true)
        assertTrue("Accessibility problems at 2x font:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
