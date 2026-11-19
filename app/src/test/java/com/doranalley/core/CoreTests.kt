package com.doranalley.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class JsonTest {
    @Test fun roundTrip() {
        val src = mapOf("a" to 1L, "b" to listOf("강의실", true, null), "c" to mapOf("x" to 2.5, "q" to "say \"hi\"\n"))
        val back = Json.parse(Json.write(src))
        assertEquals(src, back)
    }
}

class KoTextTest {
    private fun n(s: String) = KoText.numbers(KoText.normalize(s))

    @Test fun digits() = assertEquals(listOf(302), n("학생회관 302호예요"))
    @Test fun sinoKorean() = assertEquals(listOf(302), n("삼백이 호요?"))
    @Test fun digitByDigit() = assertEquals(listOf(302), n("삼, 공, 이"))
    @Test fun spacedDigits() = assertEquals(listOf(302), n("3 0 2호"))
    @Test fun ordinarySyllablesAreNotNumbers() {
        assertTrue(n("강의실이 어디예요? 오늘 사무실에 가요").isEmpty())
    }
    @Test fun parseKorean() {
        assertEquals(312, KoText.parseKorean("삼백십이"))
        assertEquals(200, KoText.parseKorean("이백"))
        assertEquals(110, KoText.parseKorean("백십"))
    }
    @Test fun usedForms() {
        val used = KoText.used("오늘 강의실이 학생회관으로 바꼈어요", TestContent.vocab.values.filter { it.target })
        assertEquals(setOf("classroom", "changed"), used)
    }
}

class ContentTest {
    @Test fun allEpisodesValid() {
        for (ep in TestContent.episodes) {
            val problems = ContentValidator.validate(ep, TestContent.characters, TestContent.vocab, TestContent.campus)
            assertTrue("${ep.id}: $problems", problems.isEmpty())
        }
    }

    @Test fun recallVariantsBuilt() {
        val recall = TestContent.episodes.filter { it.parent == "ep01_classroom_moved" }
        assertEquals(3, recall.size)
        val union = recall.first { it.id.endsWith("union") }
        val line = union.node("rl_ok") as Node.Line
        assertEquals("아, 학생회관으로요? 알겠어요. 같이 가요!", line.ko)
        assertEquals("street_haksaeng", (union.node("rm_go") as Node.Move).moves["learner"])
    }

    @Test fun campusPlaceholdersResolved() {
        val l = TestContent.ep01.node("l_room") as Node.Line
        assertEquals("학생회관 302호예요.", l.ko)
        assertEquals("201호 강의실", TestContent.campus.location("old_room")!!.sign)
    }

    @Test fun uiStringsHaveAllLanguages() {
        val missing = TestContent.ui.flatMap { (k, l) -> SUPPORTED_LANGS.filter { l[it].isNullOrBlank() }.map { "$k.$it" } }
        assertTrue(missing.toString(), missing.isEmpty())
    }

    @Test fun vocabAndCharactersTranslated() {
        val missing = mutableListOf<String>()
        TestContent.vocab.values.forEach { v -> SUPPORTED_LANGS.filter { v.meaning[it].isNullOrBlank() }.forEach { missing += "${v.id}.$it" } }
        TestContent.characters.values.forEach { c -> SUPPORTED_LANGS.filter { c.role[it].isNullOrBlank() }.forEach { missing += "${c.id}.$it" } }
        TestContent.config.daily.forEach { d -> SUPPORTED_LANGS.filter { d.label[it].isNullOrBlank() }.forEach { missing += "daily.$it" } }
        assertTrue(missing.toString(), missing.isEmpty())
    }
}

class MatcherTest {
    private val ep = TestContent.ep01
    private fun m(turn: String, vararg said: String) = IntentMatcher.match(said.toList(), TestContent.turn(ep, turn).intents)
    private fun id(r: MatchResult) = (r as? MatchResult.Matched)?.intent?.id

    @Test fun askWhereAlternatives() {
        assertEquals("ask_where", id(m("t_where", "강의실이 어디예요?")))
        assertEquals("ask_where", id(m("t_where", "어디로 가면 돼요?")))
        assertEquals("ask_where", id(m("t_where", "몇 호예요")))
        assertEquals("ask_repeat", id(m("t_where", "다시 한 번 말해 주세요")))
    }

    @Test fun roomNumberCheck() {
        assertEquals("confirm_ok", id(m("t_confirm", "학생회관 302호요?")))
        assertEquals("confirm_ok", id(m("t_confirm", "삼백이 호요?")))
        assertEquals("wrong_number", id(m("t_confirm", "학생회관 301호요?")))
        assertEquals("wrong_building", id(m("t_confirm", "인문관이요?")))
        assertEquals("ask_repeat", id(m("t_confirm", "학생회관 몇 호요?")))
    }

    @Test fun ownWordsExplanation() {
        assertEquals("tell_full", id(m("t_tell", "오늘 강의실이 학생회관 302호로 바뀌었어요")))
        assertEquals("tell_full", id(m("t_tell", "학생회관 3층 302호에서 해요")))
        assertEquals("tell_building", id(m("t_tell", "학생회관이에요")))
        assertEquals("tell_room", id(m("t_tell", "302호예요")))
        assertEquals("wrong_building", id(m("t_tell", "인문관이에요")))
        assertEquals("wrong_number", id(m("t_tell", "학생회관 203호예요")))
        assertTrue(m("t_tell", "잘 모르겠어요") is MatchResult.NoMatch)
    }

    @Test fun meetingMoved() {
        assertEquals("explain_ok", id(m("t_explain", "아니요, 카페로 바뀌었어요")))
        assertEquals("only_no", id(m("t_explain", "아니요, 도서관 아니에요")))
        assertEquals("says_library", id(m("t_explain", "네, 도서관이에요")))
    }

    @Test fun disagreeingAlternativesAreAmbiguous() {
        val r = m("t_confirm", "학생회관 302호요", "학생회관 312호요")
        assertTrue(r is MatchResult.Ambiguous)
        assertEquals(setOf("confirm_ok", "wrong_number"), (r as MatchResult.Ambiguous).candidates.map { it.id }.toSet())
    }

    @Test fun laterAlternativeUsedWhenTopFails() {
        assertEquals("ask_where", id(m("t_where", "음…", "강의실 어디예요")))
    }
}

class EngineTest {
    private val ep = TestContent.ep01
    private val e = Engine(ep)
    private var now = 1_700_000_000_000L

    private fun runAuto(p0: Progress): Progress {
        var p = p0
        while (e.isAuto(p)) p = e.step(p, now++)
        return p
    }

    private fun say(p: Progress, text: String, ev: Evidence = Evidence.RECOGNIZED): Progress {
        val r = e.match(p, listOf(text))
        return if (r is MatchResult.Matched) {
            val used = KoText.used(text, TestContent.vocab.values)
            runAuto(e.resolve(p, r.intent.id, text, used, ev, now++).first)
        } else runAuto(e.noMatch(p, text, ev, now++))
    }

    @Test fun wholeEpisodeWithDetours() {
        var p = runAuto(e.start(now))
        assertEquals("t_where", p.nodeId)
        // Partner lines are not tappable until the exchange is over.
        assertFalse(p.transcript.last { it.kind == "partner" }.unlocked)
        p = say(p, "뭐라고요?")
        assertEquals("t_where", p.nodeId)
        p = say(p, "어디로 가면 돼요?")
        assertEquals("t_confirm", p.nodeId)
        assertEquals("hallway", p.positions["learner"])
        p = say(p, "학생회관 301호요?")           // wrong number -> natural clarification
        assertEquals("t_confirm", p.nodeId)
        assertEquals("l_wrong_number", p.transcript.last { it.kind == "partner" }.nodeId)
        p = say(p, "302호요?")
        assertEquals("t_tell", p.nodeId)
        assertEquals("street_inmun", p.positions["learner"])
        p = say(p, "인문관이에요")                  // she walks to the wrong building and comes back
        assertEquals("t_tell", p.nodeId)
        assertTrue(p.transcript.any { it.nodeId == "l_wrong_back" })
        p = say(p, "학생회관이에요")                 // building only -> she asks which room
        assertEquals("t_tell", p.nodeId)
        p = say(p, "302호요")                       // remembers the building -> done
        assertEquals("t_explain", p.nodeId)
        assertEquals("library", p.positions["seoyeon"])
        p = say(p, "아니요, 카페로 바뀌었어요")
        assertTrue(p.finished)
        assertEquals("cafe", p.positions["learner"])
        assertTrue(p.transcript.all { it.unlocked })
    }

    @Test fun peekingLeavesAssistedRecord() {
        var p = runAuto(e.start(now))
        p = say(p, "어디예요?")
        p = say(p, "302호요?")
        assertEquals("t_tell", p.nodeId)
        p = e.hint(e.hint(e.hint(p, now), now), now)   // cue, start, model
        assertEquals(Help.MODEL, e.memory(p).hint)
        p = say(p, "잘 모르겠어요")                     // fails once after peeking
        assertEquals(Help.MODEL, e.memory(p).hint)      // help level survives the clarification loop
        p = say(p, "오늘 강의실이 학생회관 302호로 바뀌었어요")
        val o = p.outcomes.last { it.turnId == "t_tell" }
        assertTrue(o.success)
        assertEquals(Help.MODEL, o.help)
        val r = e.reflection(p)
        assertTrue("changed" in r.assisted)
        assertFalse("changed" in r.independent)
    }

    @Test fun paraphraseSucceedsWithoutRecallCredit() {
        var p = runAuto(e.start(now))
        p = say(p, "어디로 가면 돼요?")       // no 강의실 / 몇 호
        val o = p.outcomes.last()
        assertTrue(o.success)
        assertTrue(o.used.isEmpty())
        assertTrue("classroom" in e.reflection(p).notYet)
    }

    @Test fun listeningOnlyGivesNoRecallCredit() {
        var p = runAuto(e.start(now))
        val (np, o) = e.resolve(p, "ask_where", null, setOf("classroom"), Evidence.LISTENING_ONLY, now)
        assertTrue(o.used.isEmpty())
        p = runAuto(np)
        assertEquals("t_confirm", p.nodeId)
    }

    @Test fun resumeAfterSaveMidLesson() {
        var p = runAuto(e.start(now))
        p = say(p, "강의실이 어디예요?")
        p = e.hint(p, now)
        val restored = Codec.progress(Json.parseObject(Json.write(Codec.progress(p))))
        assertEquals(p, restored)
        val cont = say(restored, "302호요?")
        assertEquals("t_tell", cont.nodeId)
    }
}

class SchedulerTest {
    private val cfg = ReviewConfig()
    private val zone = ZoneId.of("Asia/Tashkent")
    private val h = 3_600_000L
    private val t0 = 1_700_000_000_000L
    private fun item(now: Long = t0) = Scheduler.introduce("changed", "바뀌었어요", mapOf("en" to "changed"), null, "lesson", null, now, cfg)

    @Test fun firstReviewLaterThatDay() {
        val i = item()
        assertEquals(t0 + 4 * h, i.nextReview)
        assertFalse(Scheduler.isDue(i, t0 + 3 * h))
        assertTrue(Scheduler.isDue(i, t0 + 4 * h))
    }

    @Test fun ladderOfIntervals() {
        var i = item()
        var now = i.nextReview
        i = Scheduler.record(i, now, Help.NONE, true, Evidence.RECOGNIZED, "recall", cfg, zone)
        assertEquals(now + 24 * h, i.nextReview)
        now = i.nextReview
        i = Scheduler.record(i, now, Help.NONE, true, Evidence.SELF_CONFIRMED, "recall", cfg, zone)
        assertEquals(now + 72 * h, i.nextReview)
        now = i.nextReview
        i = Scheduler.record(i, now, Help.NONE, true, Evidence.SELF_CONFIRMED, "recall", cfg, zone)
        assertEquals(now + 168 * h, i.nextReview)
        assertEquals(3, i.independentDays.size)
    }

    @Test fun difficultComesBackSooner() {
        var i = item()
        i = Scheduler.record(i, i.nextReview, Help.NONE, true, Evidence.RECOGNIZED, "recall", cfg, zone)
        val now = i.nextReview
        i = Scheduler.record(i, now, Help.MODEL, true, Evidence.RECOGNIZED, "recall", cfg, zone)
        assertEquals(0, i.stage)
        assertEquals(now + 1 * h, i.nextReview)
        assertEquals("model", i.history.last().result)
    }

    @Test fun paraphraseKeepsSchedule() {
        val i = item()
        val after = Scheduler.record(i, t0 + h, Help.NONE, false, Evidence.RECOGNIZED, "ep", cfg, zone)
        assertEquals(i.nextReview, after.nextReview)
        assertEquals("paraphrase", after.history.last().result)
    }

    @Test fun independentDaysAreDistinctDays() {
        var i = item()
        i = Scheduler.record(i, t0 + h, Help.NONE, true, Evidence.RECOGNIZED, "ep", cfg, zone)
        i = Scheduler.record(i, t0 + 2 * h, Help.NONE, true, Evidence.RECOGNIZED, "ep", cfg, zone)
        assertEquals(1, i.independentDays.size)
        // Early use does not push the first review.
        assertEquals(t0 + 4 * h, i.nextReview)
    }

    @Test fun dueStateIndependentOfInterfaceLanguage() {
        // Items carry every translation and are keyed by a language-free id,
        // so switching the interface language cannot change what is due.
        val saved = Scheduler.introduce("saved:library", "도서관",
            mapOf("en" to "library", "ru" to "библиотека", "uz" to "kutubxona"), null, "saved", null, t0, cfg)
        val stored = Codec.item(Json.parseObject(Json.write(Codec.item(saved))))
        val dueAt = t0 + 5 * h
        assertEquals(listOf("saved:library"), Scheduler.due(listOf(stored), dueAt).map { it.id })
        for (lang in SUPPORTED_LANGS) {
            val s = Codec.settings(Json.parseObject(Json.write(Codec.settings(Settings(lang = lang)))))
            assertEquals(lang, s.lang)
            assertEquals(1, Scheduler.due(listOf(stored), dueAt).size)
            assertTrue(stored.meaning.pick(lang).isNotBlank())
        }
    }
}
