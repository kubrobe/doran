package com.doranalley.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HangulTest {
    @Test fun audioKeyMatchesBuilder() = assertEquals("e9a95de0add7", Hangul.audioKey("안녕하세요"))

    @Test fun similarity() {
        assertEquals(1.0, Hangul.similarity("저는 학생이에요.", "저 는 학생 이에요"), 1e-9)
        assertEquals(1.0, Hangul.similarity("세탁기", "새탁기"), 1e-9) // ㅐ/ㅔ merged
        assertTrue(Hangul.similarity("저는 학생이에요", "저는 흑색이예요") in 0.6..0.95)
        assertTrue(Hangul.similarity("안녕하세요", "감사합니다") < 0.4)
        assertEquals(0.0, Hangul.similarity("안녕하세요", ""), 1e-9)
    }

    @Test fun scoreThresholds() {
        assertTrue(SpeechResult.of("화장실이 어디예요?", "화장실 이 어디 예요").passed)
        assertFalse(SpeechResult.of("화장실이 어디예요?", "지하철 역").passed)
    }

    @Test fun numbers() {
        assertEquals("삼백이", Hangul.sino(302))
        assertEquals("십이", Hangul.sino(12))
        assertEquals("만", Hangul.sino(10000))
        assertEquals("삼천", Hangul.sino(3000))
        assertEquals("열두", Hangul.native(12, true))
        assertEquals("스무", Hangul.native(20, true))
        assertEquals(1.0, Hangul.bestSimilarity("열두 시에 점심을 먹어요.", "12시에 점심을 먹어요"), 1e-9)
        assertEquals(1.0, Hangul.bestSimilarity("저는 여섯 시에 퇴근해요.", "저는 6시에 퇴근해요"), 1e-9)
        assertEquals(1.0, Hangul.bestSimilarity("이십 분쯤 걸려요.", "20분쯤 걸려요"), 1e-9)
        assertTrue(SpeechResult.of("학생회관 삼백이 호예요.", "학생회관 3백이 호해요").passed)
    }

    @Test fun marks() {
        val m = Hangul.markSyllables("저는 학생이에요.", "저는 흑생이에요")
        assertEquals("저는 학생이에요.".length, m.size)
        val syl = m.filter { Hangul.isSyllable(it.first) }
        assertEquals(Hangul.Mark.GOOD, syl[0].second)
        assertEquals(Hangul.Mark.CLOSE, syl[2].second)
        assertTrue(Hangul.markSyllables("안녕하세요", "").all { it.second == Hangul.Mark.MISSED })
    }
}

class CheckerTest {
    private fun s(ko: String, en: String = "x", alt: List<List<String>> = emptyList()) =
        Sentence("t", ko, en, alt, ko.replace(Regex("[.,?!]"), "").split(" "), en.split(" "), true)

    @Test fun koreanWordOrder() {
        val a = s("저는 매일 일곱 시에 일어나요.")
        assertTrue(Checker.koreanCorrect(a, listOf("저는", "매일", "일곱", "시에", "일어나요")))
        assertTrue(Checker.koreanCorrect(a, listOf("매일", "저는", "일곱", "시에", "일어나요")))
        assertTrue(Checker.koreanCorrect(a, listOf("일곱", "시에", "저는", "매일", "일어나요")))
        assertFalse(Checker.koreanCorrect(a, listOf("저는", "매일", "시에", "일곱", "일어나요")))
        assertFalse(Checker.koreanCorrect(a, listOf("일어나요", "저는", "매일", "일곱", "시에")))
        val b = s("제 이름은 민수예요.")
        assertFalse(Checker.koreanCorrect(b, listOf("이름은", "제", "민수예요")))
        assertFalse(Checker.koreanCorrect(b, listOf("제", "이름은")))
    }

    @Test fun english() {
        val a = s("x", "I get up at seven every day", listOf("Every day I get up at seven".split(" ")))
        assertTrue(Checker.englishCorrect(a, "I get up at seven every day".split(" ")))
        assertTrue(Checker.englishCorrect(a, "every day i get up at seven".split(" ")))
        assertFalse(Checker.englishCorrect(a, "I get up at every seven day".split(" ")))
    }
}

class ProgressTest {
    private val d = LocalDate.of(2026, 11, 20)

    @Test fun streak() {
        var p = Progress().addXp(10, d)
        assertEquals(1, p.streak)
        p = p.addXp(10, d)
        assertEquals(1, p.streak); assertEquals(20, p.xpOn(d))
        p = p.addXp(10, d.plusDays(1))
        assertEquals(2, p.streak)
        assertEquals(2, p.visibleStreak(d.plusDays(2)))
        assertEquals(0, p.visibleStreak(d.plusDays(3)))
        p = p.addXp(10, d.plusDays(3))
        assertEquals(1, p.streak); assertEquals(2, p.longestStreak)
        assertEquals(40, p.totalXp)
    }

    @Test fun hearts() {
        val t = 1_000_000L
        var p = Progress()
        p = p.loseHeart(t).loseHeart(t)
        assertEquals(3, p.heartsNow(t))
        assertEquals(4, p.heartsNow(t + Progress.HEART_REFILL_MS))
        assertEquals(5, p.heartsNow(t + 10 * Progress.HEART_REFILL_MS))
        repeat(10) { p = p.loseHeart(t) }
        assertEquals(0, p.heartsNow(t))
        assertEquals(1, p.gainHeart(t).heartsNow(t))
        assertEquals(5, p.copy(settings = Settings(unlimitedHearts = true)).heartsNow(t))
    }

    @Test fun jsonRoundTrip() {
        val p = Progress(onboarded = true, dailyGoal = 30, unitLessons = mapOf("u1" to 2), streak = 3, lastStreakDay = "2026-11-20",
            hearts = 2, heartsUpdatedAt = 5, items = mapOf("u1.w1" to ItemStat(3, 1, 2, 9)), settings = Settings(false, false, true, true))
        assertEquals(p, Progress.fromJson(p.toJson()))
        assertEquals(Progress(), Progress.fromJson("{}"))
    }

    @Test fun pathAndPlacement() {
        val course = Course.parse(java.io.File("src/main/assets/course/course.json").readText())
        var p = Progress()
        val first = course.units[0]
        assertEquals(first to 0, p.current(course))
        assertTrue(p.isUnlocked(course, first, 0)); assertFalse(p.isUnlocked(course, first, 1))
        p = p.completeNode(first, 0)
        assertEquals(first to 1, p.current(course))
        p = p.completeNode(first, 0) // replay does not advance
        assertEquals(first to 1, p.current(course))
        p = p.placeAt(course, 8)
        assertEquals(course.units[8] to 0, p.current(course))
        assertTrue(p.isUnlocked(course, course.units[3], 2))
        assertEquals(8, p.studiedUnits(course).size)
    }
}

class GlossTest {
    private val course = Course.parse(java.io.File("src/main/assets/course/course.json").readText())
    private val g = course.gloss

    @Test fun korean() {
        assertEquals(listOf("학생 = student", "이에요 = is / am / are"), g.korean("학생이에요."))
        assertEquals("커피 = coffee", g.korean("커피를")!!.first())
        assertEquals(listOf("안녕하세요 = hello"), g.korean("안녕하세요,"))
        assertEquals(null, g.korean("ㅋㅋ"))
    }

    @Test fun english() {
        assertEquals(listOf("학생"), g.english("student"))
        assertEquals(null, g.english("xyz"))
    }

    @Test fun almostEveryWordInASentenceHasAHint() {
        val tiles = course.units.flatMap { it.sentences }.flatMap { it.koTiles }
        val missing = tiles.filter { g.korean(it) == null }
        assertTrue("no hint for: ${missing.distinct()}", missing.size <= tiles.size / 20)
    }
}
