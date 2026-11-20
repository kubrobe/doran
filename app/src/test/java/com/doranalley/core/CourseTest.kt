package com.doranalley.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class CourseTest {
    private val course = Course.parse(File("src/main/assets/course/course.json").readText())
    private val audioDir = File("src/main/assets/audio")

    private fun check(ex: Exercise, f: ExerciseFilter) {
        when (ex) {
            is Exercise.LetterIntro -> {}
            is Exercise.LetterToSound -> { assertTrue(ex.letter.rom in ex.options); assertEquals(ex.options.size, ex.options.toSet().size); assertTrue(ex.options.size >= 3) }
            is Exercise.SoundToLetter -> {
                assertTrue(f.listening)
                assertTrue(ex.letter in ex.options); assertEquals(ex.options.size, ex.options.map { it.ch }.toSet().size)
                ex.options.filter { it != ex.letter }.forEach { o ->
                    assertTrue("confusable ${o.ch}/${ex.letter.ch}", ex.letter.group.isEmpty() || o.group != ex.letter.group)
                    assertTrue(o.say != ex.letter.say)
                }
                assertTrue(ex.options.size >= 3)
            }
            is Exercise.WordMeaning -> { assertTrue(ex.word.en in ex.options); assertEquals(ex.options.size, ex.options.map { it.lowercase() }.toSet().size); assertTrue(ex.options.size >= 3) }
            is Exercise.PickKorean -> { assertTrue(ex.word in ex.options); assertEquals(ex.options.size, ex.options.map { it.ko }.toSet().size); assertEquals(ex.options.size, ex.options.map { it.en }.toSet().size); assertTrue(ex.options.size >= 3) }
            is Exercise.ListenPick -> { assertTrue(f.listening); assertTrue(ex.ko in ex.options); assertEquals(ex.options.size, ex.options.toSet().size); assertTrue(ex.options.size >= 3) }
            is Exercise.MatchPairs -> {
                assertTrue(ex.pairs.size >= 3)
                assertEquals(ex.pairs.size, ex.pairs.map { it.first }.toSet().size)
                assertEquals(ex.pairs.size, ex.pairs.map { it.second }.toSet().size)
            }
            is Exercise.BuildEnglish -> {
                val bank = ex.tiles.map { it.text }.toMutableList()
                ex.sentence.enTiles.forEach { assertTrue("tile $it missing", bank.remove(it)) }
                assertTrue(Checker.englishCorrect(ex.sentence, ex.sentence.enTiles))
                assertEquals(ex.tiles.size, ex.tiles.map { it.id }.toSet().size)
                // A distractor must never be a word of the answer.
                bank.forEach { d -> assertFalse("distractor $d is in the answer", ex.sentence.enTiles.any { it.equals(d, true) }) }
            }
            is Exercise.BuildKorean -> {
                val bank = ex.tiles.map { it.text }.toMutableList()
                ex.sentence.koTiles.forEach { assertTrue("tile $it missing", bank.remove(it)) }
                assertTrue(Checker.koreanCorrect(ex.sentence, ex.sentence.koTiles))
                bank.forEach { d -> assertFalse(d in ex.sentence.koTiles) }
            }
            is Exercise.ListenBuild -> {
                assertTrue(f.listening)
                val bank = ex.tiles.map { it.text }.toMutableList()
                ex.sentence.koTiles.forEach { assertTrue(bank.remove(it)) }
                assertTrue(Checker.koreanCorrect(ex.sentence, ex.sentence.koTiles))
            }
            is Exercise.FillBlank -> { assertTrue(ex.fill.answer in ex.options); assertEquals(ex.options.size, ex.options.toSet().size); assertTrue(ex.options.size >= 3) }
            is Exercise.Speak -> { assertTrue(f.speaking); assertTrue(Hangul.syllables(ex.ko).isNotEmpty()) }
        }
    }

    @Test fun courseLoads() {
        assertEquals(26, course.units.size)
        assertTrue(course.units.first().isHangul)
        course.units.forEachIndexed { i, u -> assertEquals(i, u.index) }
    }

    @Test fun everyLessonIsValidForManySeeds() {
        val filters = listOf(ExerciseFilter(), ExerciseFilter(speaking = false), ExerciseFilter(listening = false), ExerciseFilter(false, false))
        for (seed in 0 until 40) {
            val b = LessonBuilder(course, Random(seed))
            for (u in course.units) for (k in 0 until u.lessonCount) for (f in filters) {
                val lesson = b.lesson(u, k, f)
                assertTrue("${u.id} lesson $k has ${lesson.size}", lesson.size in 6..24)
                lesson.forEach { ex ->
                    try { check(ex, f) } catch (e: AssertionError) { throw AssertionError("${u.id}/$k seed $seed $f: $ex: ${e.message}") }
                }
                if (!f.speaking) assertTrue(lesson.none { it is Exercise.Speak })
                if (!f.listening) assertTrue(lesson.none { it.kind == Exercise.Kind.LISTEN })
            }
        }
    }

    @Test fun regularLessonsTeachSpeakingAndListening() {
        val b = LessonBuilder(course, Random(1))
        for (u in course.units.filter { !it.isHangul }) {
            val all = (0 until u.lessonCount).flatMap { b.lesson(u, it, ExerciseFilter()) }
            assertTrue("${u.id} no speaking", all.any { it is Exercise.Speak } || u.sentences.none { it.speak })
            assertTrue("${u.id} no listening", all.any { it.kind == Exercise.Kind.LISTEN })
        }
    }

    @Test fun practiceAndPlacement() {
        val b = LessonBuilder(course, Random(3))
        for (mode in LessonBuilder.PracticeMode.entries) {
            val p = b.practice(course.units.take(10), emptyMap(), mode, ExerciseFilter())
            assertTrue("$mode empty", p.isNotEmpty())
            p.forEach { check(it, ExerciseFilter()) }
        }
        course.units.forEach { u -> repeat(10) { check(b.placementQuestion(u), ExerciseFilter()) } }
        assertTrue(b.practice(emptyList(), emptyMap(), LessonBuilder.PracticeMode.MIXED, ExerciseFilter()).isEmpty())
    }

    @Test fun everyKoreanLineHasAudio() {
        val missing = mutableListOf<String>()
        fun need(text: String, slow: Boolean) {
            val k = Hangul.audioKey(text)
            if (!File(audioDir, "$k.mp3").exists()) missing += text
            if (slow && !File(audioDir, "${k}_s.mp3").exists()) missing += "$text (slow)"
        }
        course.units.forEach { u ->
            u.letters.forEach { need(it.say, false) }
            u.words.forEach { need(it.ko, false) }
            u.sentences.forEach { need(it.ko, true) }
            u.fills.forEach { need(it.full, true) }
        }
        need("안녕하세요", false)
        assertTrue("missing audio: ${missing.take(10)} (${missing.size})", missing.isEmpty())
    }
}
