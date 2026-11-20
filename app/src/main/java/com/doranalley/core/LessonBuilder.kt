package com.doranalley.core

import kotlin.random.Random

/** Which exercise types are allowed right now (the learner can pause speaking or listening). */
data class ExerciseFilter(val speaking: Boolean = true, val listening: Boolean = true)

/**
 * Builds lessons, practice sessions and placement questions from the course.
 * Deterministic for a given [Random], so tests can run every lesson with many seeds.
 */
class LessonBuilder(private val course: Course, private val rng: Random) {

    companion object {
        private val EN_STOP = setOf(
            "a", "an", "the", "is", "am", "are", "it", "it's", "to", "at", "in", "on", "of", "for", "too", "also",
            "really", "very", "please", "me", "my", "you", "your", "i", "i'm", "do", "does", "did", "will", "be",
            "so", "and", "but", "some", "all", "just", "oh", "yes", "no", "okay", "this", "that", "there", "here",
            "get", "go", "let's", "what", "where", "how",
        )
        private val KO_STOP = setOf(
            "좀", "정말", "진짜", "너무", "아주", "그냥", "같이", "지금", "오늘", "다", "잘", "안", "못", "조금", "많이",
            "자주", "일찍", "벌써", "또", "다시", "먼저", "저기", "여기", "네", "아", "그거", "이거",
        )
    }

    // ---------------------------------------------------------------- lessons

    fun lesson(unit: CourseUnit, lessonIndex: Int, filter: ExerciseFilter): List<Exercise> {
        val list = if (unit.isHangul) hangulLesson(unit, lessonIndex, filter) else regularLesson(unit, lessonIndex, filter)
        return list.ifEmpty { listOf(fallbackChoice(unit)) }
    }

    private fun <T> chunk(items: List<T>, parts: Int, index: Int): List<T> {
        if (items.isEmpty()) return emptyList()
        val size = (items.size + parts - 1) / parts
        return items.drop(size * index).take(size)
    }

    private fun regularLesson(u: CourseUnit, k: Int, f: ExerciseFilter): List<Exercise> {
        val review = k >= u.lessonCount - 1
        val out = mutableListOf<Exercise>()
        if (!review) {
            val newWords = chunk(u.words, 3, k)
            val oldWords = u.words.take(u.words.indexOf(newWords.firstOrNull()).coerceAtLeast(0))
            val newSents = chunk(u.sentences, 3, k)
            val oldSents = u.sentences.take(u.sentences.indexOf(newSents.firstOrNull()).coerceAtLeast(0))
            val intros = newWords.map { wordMeaning(u, it, isNew = true) }
            out += intros
            newWords.shuffled(rng).firstOrNull()?.let { out += pickKorean(u, it) }
            val rest = mutableListOf<Exercise>()
            matchWords(u, (newWords + oldWords.shuffled(rng)).take(5))?.let { rest += it }
            newSents.forEachIndexed { i, s ->
                when (i % 3) {
                    0 -> { rest += buildEnglish(u, s); rest += speakOrBuild(u, s, f) }
                    1 -> { rest += buildKorean(u, s); rest += listenOrBuild(u, s, f) }
                    else -> { rest += buildEnglish(u, s); rest += buildKorean(u, s) }
                }
            }
            u.fills.getOrNull(k)?.let { rest += fillBlank(it) }
            oldSents.shuffled(rng).take(2).forEach { rest += if (rng.nextBoolean()) buildKorean(u, it) else listenOrBuild(u, it, f) }
            (newWords + oldWords).shuffled(rng).firstOrNull()?.let { rest += listenPickWord(u, it, f) }
            rest.shuffle(rng)
            out += rest
        } else {
            val words = u.words.shuffled(rng)
            val sents = u.sentences.shuffled(rng)
            matchWords(u, words.take(5))?.let { out += it }
            words.drop(5).take(2).forEach { out += wordMeaning(u, it, isNew = false) }
            sents.take(3).forEach { out += buildKorean(u, it) }
            sents.drop(3).take(2).forEach { out += buildEnglish(u, it) }
            sents.drop(5).take(2).forEach { out += listenOrBuild(u, it, f) }
            sents.drop(7).take(2).forEach { out += speakOrBuild(u, it, f) }
            u.fills.shuffled(rng).forEach { out += fillBlank(it) }
            words.firstOrNull()?.let { out += pickKorean(u, it) }
            val first = out.removeAt(0)
            out.shuffle(rng)
            out.add(0, first)
        }
        return out
    }

    private fun hangulLesson(u: CourseUnit, k: Int, f: ExerciseFilter): List<Exercise> {
        val out = mutableListOf<Exercise>()
        if (k < 2) {
            val letters = chunk(u.letters, 2, k)
            val before = if (k == 1) chunk(u.letters, 2, 0) else emptyList()
            letters.forEach { out += Exercise.LetterIntro(it); out += letterToSound(u, it) }
            val rest = mutableListOf<Exercise>()
            letters.forEach { rest += soundToLetterOr(u, it, f) }
            before.shuffled(rng).take(2).forEach { rest += letterToSound(u, it) }
            rest.shuffle(rng)
            out += rest
            matchLetters(u, (letters + before.shuffled(rng)).take(5))?.let { out += it }
        } else {
            u.words.forEach { out += wordMeaning(u, it, isNew = true) }
            val rest = mutableListOf<Exercise>()
            u.letters.shuffled(rng).take(3).forEach { rest += soundToLetterOr(u, it, f) }
            u.letters.shuffled(rng).take(2).forEach { rest += letterToSound(u, it) }
            u.words.shuffled(rng).take(2).forEach { rest += listenPickWord(u, it, f) }
            u.words.shuffled(rng).firstOrNull()?.let { rest += pickKorean(u, it) }
            rest.shuffle(rng)
            out += rest
            matchLetters(u, u.letters.shuffled(rng).take(5))?.let { out += it }
            matchWords(u, u.words.shuffled(rng).take(5))?.let { out += it }
        }
        return out
    }

    private fun fallbackChoice(u: CourseUnit): Exercise =
        if (u.words.isNotEmpty()) wordMeaning(u, u.words.first(), isNew = false) else letterToSound(u, u.letters.first())

    // ---------------------------------------------------------------- practice & placement

    /**
     * A practice session from what the learner has already studied, weakest items first.
     * [weakness] maps item id -> how much it needs review (higher = weaker).
     */
    fun practice(units: List<CourseUnit>, weakness: Map<String, Double>, mode: PracticeMode, f: ExerciseFilter, size: Int = 12): List<Exercise> {
        if (units.isEmpty()) return emptyList()
        data class Cand(val u: CourseUnit, val id: String, val make: () -> Exercise?)
        val cands = mutableListOf<Cand>()
        for (u in units) {
            for (l in u.letters) cands += Cand(u, l.id) { if (rng.nextBoolean()) letterToSound(u, l) else soundToLetterOr(u, l, f) }
            for (w in u.words) cands += Cand(u, w.id) {
                when (mode) {
                    PracticeMode.LISTENING -> if (f.listening) listenPickWord(u, w, f) else null
                    PracticeMode.SPEAKING -> null
                    else -> if (rng.nextBoolean()) wordMeaning(u, w, false) else pickKorean(u, w)
                }
            }
            for (s in u.sentences) cands += Cand(u, s.id) {
                when (mode) {
                    PracticeMode.LISTENING -> if (f.listening) listenBuild(u, s) else null
                    PracticeMode.SPEAKING -> if (f.speaking && s.speak) speak(s) else null
                    else -> when (rng.nextInt(3)) { 0 -> buildEnglish(u, s); 1 -> buildKorean(u, s); else -> listenOrBuild(u, s, f) }
                }
            }
            for (fl in u.fills) cands += Cand(u, fl.id) { if (mode == PracticeMode.MIXED || mode == PracticeMode.MISTAKES) fillBlank(fl) else null }
        }
        val pool = when (mode) {
            PracticeMode.MISTAKES -> cands.filter { (weakness[it.id] ?: 0.0) > 0.0 }.ifEmpty { cands }
            else -> cands
        }
        // Weighted by weakness with a little randomness so sessions vary.
        val ordered = pool.sortedByDescending { (weakness[it.id] ?: 0.0) + rng.nextDouble() * 0.8 }
        val out = mutableListOf<Exercise>()
        for (c in ordered) {
            if (out.size >= size) break
            c.make()?.let { out += it }
        }
        return out
    }

    enum class PracticeMode { MIXED, MISTAKES, LISTENING, SPEAKING }

    /** One question that checks whether the learner already knows a unit. Never audio-only. */
    fun placementQuestion(u: CourseUnit): Exercise = when {
        u.isHangul -> letterToSound(u, u.letters.random(rng))
        else -> when (rng.nextInt(3)) {
            0 -> buildKorean(u, u.sentences.random(rng))
            1 -> buildEnglish(u, u.sentences.random(rng))
            else -> u.fills.randomOrNull(rng)?.let { fillBlank(it) } ?: buildKorean(u, u.sentences.random(rng))
        }
    }

    // ---------------------------------------------------------------- exercise makers

    private fun earlierWords(u: CourseUnit) = course.units.take(u.index).flatMap { it.words }

    fun wordMeaning(u: CourseUnit, w: Word, isNew: Boolean): Exercise.WordMeaning {
        val pool = (u.words + earlierWords(u).reversed()).map { it.en }.distinct().filter { !it.equals(w.en, true) }
        val distract = (pool.take(12).shuffled(rng)).take(3)
        return Exercise.WordMeaning(w, (distract + w.en).shuffled(rng), isNew)
    }

    fun pickKorean(u: CourseUnit, w: Word): Exercise.PickKorean {
        val pool = (u.words + earlierWords(u).reversed()).distinctBy { it.ko }.filter { it.ko != w.ko && it.en != w.en }
        val distract = pool.take(12).shuffled(rng).take(2)
        return Exercise.PickKorean(w, (distract + w).shuffled(rng))
    }

    private fun listenPickWord(u: CourseUnit, w: Word, f: ExerciseFilter): Exercise {
        if (!f.listening) return wordMeaning(u, w, false)
        val pool = (u.words + earlierWords(u).reversed()).map { it.ko }.distinct().filter { it != w.ko }
        val distract = pool.take(10).shuffled(rng).take(3)
        return Exercise.ListenPick(w.id, w.ko, (distract + w.ko).shuffled(rng))
    }

    private fun letterToSound(u: CourseUnit, l: Letter): Exercise.LetterToSound {
        val earlier = course.units.take(u.index).flatMap { it.letters }
        val pool = (u.letters + earlier.reversed()).map { it.rom }.distinct().filter { it != l.rom }
        val distract = pool.take(10).shuffled(rng).take(3)
        return Exercise.LetterToSound(l, (distract + l.rom).shuffled(rng))
    }

    private fun soundToLetterOr(u: CourseUnit, l: Letter, f: ExerciseFilter): Exercise {
        if (!f.listening) return letterToSound(u, l)
        val pool = u.letters.filter { it.id != l.id && it.say != l.say && (l.group.isEmpty() || it.group != l.group) }
        val distract = pool.shuffled(rng).take(3)
        return Exercise.SoundToLetter(l, (distract + l).shuffled(rng))
    }

    private fun matchWords(u: CourseUnit, words: List<Word>): Exercise.MatchPairs? {
        val ws = words.distinctBy { it.ko }.distinctBy { it.en.lowercase() }
        if (ws.size < 3) return null
        return Exercise.MatchPairs("${u.id}.match", ws.map { it.ko to it.en }, leftAudio = true)
    }

    private fun matchLetters(u: CourseUnit, letters: List<Letter>): Exercise.MatchPairs? {
        val ls = letters.distinctBy { it.ch }.distinctBy { it.rom }
        if (ls.size < 3) return null
        return Exercise.MatchPairs("${u.id}.matchl", ls.map { it.ch to it.rom }, leftAudio = false)
    }

    private fun bankSize(answer: Int) = (answer / 2 + 2).coerceIn(2, 4)

    fun buildEnglish(u: CourseUnit, s: Sentence): Exercise.BuildEnglish {
        val answerLower = s.enTiles.map { it.lowercase() }.toSet() + s.enAlt.flatten().map { it.lowercase() }
        val pool = u.sentences.filter { it.id != s.id }.flatMap { it.enTiles }
            .filter { it.lowercase() !in answerLower && it.lowercase() !in EN_STOP }
            .distinctBy { it.lowercase() }
        val distract = pool.shuffled(rng).take(bankSize(s.enTiles.size))
        return Exercise.BuildEnglish(s, tiles(s.enTiles + distract))
    }

    fun buildKorean(u: CourseUnit, s: Sentence): Exercise.BuildKorean =
        Exercise.BuildKorean(s, tiles(s.koTiles + koDistractors(u, s)))

    private fun listenBuild(u: CourseUnit, s: Sentence): Exercise.ListenBuild =
        Exercise.ListenBuild(s, tiles(s.koTiles + koDistractors(u, s)))

    private fun listenOrBuild(u: CourseUnit, s: Sentence, f: ExerciseFilter): Exercise =
        if (f.listening) listenBuild(u, s) else buildKorean(u, s)

    private fun speakOrBuild(u: CourseUnit, s: Sentence, f: ExerciseFilter): Exercise =
        if (f.speaking && s.speak) speak(s) else buildKorean(u, s)

    private fun speak(s: Sentence) = Exercise.Speak(s.id, s.ko, s.en)

    private fun koDistractors(u: CourseUnit, s: Sentence): List<String> {
        val answer = s.koTiles.toSet()
        val pool = u.sentences.filter { it.id != s.id }.flatMap { it.koTiles }
            .filter { it !in answer && it !in KO_STOP }
            .distinct()
        return pool.shuffled(rng).take(bankSize(s.koTiles.size))
    }

    fun fillBlank(fl: Fill) = Exercise.FillBlank(fl, (fl.options + fl.answer).shuffled(rng))

    private fun tiles(words: List<String>): List<Tile> =
        words.shuffled(rng).mapIndexed { i, t -> Tile(i, t) }
}
