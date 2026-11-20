package com.doranalley.core

/** One tile in a word bank. The id keeps duplicate words apart. */
data class Tile(val id: Int, val text: String)

sealed interface Exercise {
    /** Learnable item this exercise practises (for review statistics). */
    val itemId: String
    /** Korean text that is played automatically when the exercise appears, if any. */
    val autoplay: String?
    /** What the solution looks like, shown in the feedback panel. */
    val solution: String
    val kind: Kind

    enum class Kind { INTRO, CHOICE, BUILD, MATCH, SPEAK, LISTEN }

    /** A Hangul letter shown for the first time. Not graded. */
    data class LetterIntro(val letter: Letter) : Exercise {
        override val itemId get() = letter.id
        override val autoplay get() = letter.say
        override val solution get() = letter.rom
        override val kind get() = Kind.INTRO
    }

    /** "What sound does ㅏ make?" */
    data class LetterToSound(val letter: Letter, val options: List<String>) : Exercise {
        override val itemId get() = letter.id
        override val autoplay: String? get() = null
        override val solution get() = letter.rom
        override val kind get() = Kind.CHOICE
    }

    /** "Which letter do you hear?" */
    data class SoundToLetter(val letter: Letter, val options: List<Letter>) : Exercise {
        override val itemId get() = letter.id
        override val autoplay get() = letter.say
        override val solution get() = letter.ch
        override val kind get() = Kind.LISTEN
    }

    /** "What does 물 mean?" */
    data class WordMeaning(val word: Word, val options: List<String>, val isNew: Boolean) : Exercise {
        override val itemId get() = word.id
        override val autoplay get() = word.ko
        override val solution get() = word.en
        override val kind get() = Kind.CHOICE
    }

    /** "Which one is 'water'?" Options are Korean words that play when tapped. */
    data class PickKorean(val word: Word, val options: List<Word>) : Exercise {
        override val itemId get() = word.id
        override val autoplay: String? get() = null
        override val solution get() = word.ko
        override val kind get() = Kind.CHOICE
    }

    /** "What do you hear?" Pick the Korean text that was played. */
    data class ListenPick(override val itemId: String, val ko: String, val options: List<String>) : Exercise {
        override val autoplay get() = ko
        override val solution get() = ko
        override val kind get() = Kind.LISTEN
    }

    /** Match pairs. left = Korean (plays audio when tapped if [leftAudio]), right = meaning. */
    data class MatchPairs(override val itemId: String, val pairs: List<Pair<String, String>>, val leftAudio: Boolean) : Exercise {
        override val autoplay: String? get() = null
        override val solution get() = ""
        override val kind get() = Kind.MATCH
    }

    /** Translate Korean -> English with a word bank. */
    data class BuildEnglish(val sentence: Sentence, val tiles: List<Tile>) : Exercise {
        override val itemId get() = sentence.id
        override val autoplay get() = sentence.ko
        override val solution get() = sentence.en
        override val kind get() = Kind.BUILD
    }

    /** Translate English -> Korean with a word bank. */
    data class BuildKorean(val sentence: Sentence, val tiles: List<Tile>) : Exercise {
        override val itemId get() = sentence.id
        override val autoplay: String? get() = null
        override val solution get() = sentence.ko
        override val kind get() = Kind.BUILD
    }

    /** "Tap what you hear." Korean word bank, audio only. */
    data class ListenBuild(val sentence: Sentence, val tiles: List<Tile>) : Exercise {
        override val itemId get() = sentence.id
        override val autoplay get() = sentence.ko
        override val solution get() = sentence.ko
        override val kind get() = Kind.LISTEN
    }

    /** Fill in the blank. */
    data class FillBlank(val fill: Fill, val options: List<String>) : Exercise {
        override val itemId get() = fill.id
        override val autoplay: String? get() = null
        override val solution get() = fill.full
        override val kind get() = Kind.CHOICE
    }

    /** Read the sentence out loud. */
    data class Speak(override val itemId: String, val ko: String, val en: String) : Exercise {
        override val autoplay: String? get() = null
        override val solution get() = ko
        override val kind get() = Kind.SPEAK
    }
}

/** Answer checking. Kept separate from the UI so every rule is unit tested. */
object Checker {
    private fun normEn(t: String) = t.lowercase().trim().trimEnd('.', ',', '?', '!')

    fun englishCorrect(s: Sentence, answer: List<String>): Boolean {
        val a = answer.map(::normEn)
        if (a == s.enTiles.map(::normEn)) return true
        return s.enAlt.any { alt -> alt.map(::normEn) == a }
    }

    private val particleEndings = listOf(
        "에서", "에게", "한테", "까지", "부터", "으로", "하고", "께서",
        "은", "는", "이", "가", "을", "를", "에", "로", "도", "께",
    )
    private val movableAdverbs = setOf(
        "오늘", "내일", "어제", "지금", "매일", "이따가", "벌써", "나중에", "자주", "같이", "일찍", "다시",
    )

    /**
     * Korean phrases that may swap places. A phrase ends at a word with a case/topic particle,
     * or at a free time adverb. The last phrase holds the verb and must stay at the end.
     */
    fun koreanChunks(tiles: List<String>): List<List<String>> {
        val chunks = mutableListOf<MutableList<String>>()
        var cur = mutableListOf<String>()
        tiles.forEachIndexed { i, t ->
            cur += t
            val last = i == tiles.lastIndex
            val ends = !last && (t in movableAdverbs || particleEndings.any { p -> t.length > p.length && t.endsWith(p) })
            if (ends) { chunks += cur; cur = mutableListOf() }
        }
        if (cur.isNotEmpty()) chunks += cur
        return chunks
    }

    fun koreanCorrect(s: Sentence, answer: List<String>): Boolean {
        if (answer == s.koTiles) return true
        if (answer.size != s.koTiles.size || answer.sorted() != s.koTiles.sorted()) return false
        val chunks = koreanChunks(s.koTiles)
        if (chunks.size < 2 || chunks.size > 6) return false
        val fixedEnd = chunks.last()
        val movable = chunks.dropLast(1)
        if (answer.takeLast(fixedEnd.size) != fixedEnd) return false
        return matchesPermutation(answer.dropLast(fixedEnd.size), movable)
    }

    private fun matchesPermutation(rest: List<String>, chunks: List<List<String>>): Boolean {
        if (rest.isEmpty()) return chunks.isEmpty()
        for (i in chunks.indices) {
            val c = chunks[i]
            if (rest.size >= c.size && rest.subList(0, c.size) == c) {
                val remaining = chunks.toMutableList().also { it.removeAt(i) }
                if (matchesPermutation(rest.drop(c.size), remaining)) return true
            }
        }
        return false
    }
}
