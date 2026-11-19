package com.doranalley.core

/** Korean text helpers used to interpret what the learner said. */
object KoText {

    /** Lowercase, drop whitespace and punctuation. Hangul, digits and Latin letters are kept. */
    fun normalize(s: String): String = buildString {
        for (c in s.lowercase()) {
            if (c.isLetterOrDigit()) append(c)
        }
    }

    private val digitWords = mapOf(
        '영' to 0, '공' to 0, '일' to 1, '이' to 2, '삼' to 3, '사' to 4,
        '오' to 5, '육' to 6, '칠' to 7, '팔' to 8, '구' to 9,
    )
    private val units = mapOf('십' to 10, '백' to 100, '천' to 1000)
    private fun isNumeralChar(c: Char) = c in digitWords || c in units

    /**
     * Extracts numbers from a normalized utterance: Arabic digits ("302호") and spoken Sino-Korean
     * numbers ("삼백이 호", "삼공이"). Because 이/사/오/구 are also ordinary syllables, a Korean
     * numeral run is only read as a number when followed by 호, or when it is at least three syllables
     * long and contains 백/공/영.
     */
    fun numbers(normalized: String): List<Int> {
        val out = mutableListOf<Int>()
        var i = 0
        val s = normalized
        while (i < s.length) {
            val c = s[i]
            if (c.isDigit()) {
                val start = i
                while (i < s.length && s[i].isDigit()) i++
                s.substring(start, i).take(9).toIntOrNull()?.let(out::add)
                continue
            }
            if (isNumeralChar(c)) {
                val start = i
                while (i < s.length && isNumeralChar(s[i])) i++
                val run = s.substring(start, i)
                val followedByHo = s.getOrNull(i) == '호'
                val looksNumeric = run.length >= 3 && run.any { it == '백' || it == '공' || it == '영' }
                if (followedByHo || looksNumeric) {
                    // A particle 이 can trail the number ("302호예요" is fine, but "삼백이" is 302).
                    parseKorean(run)?.let(out::add)
                }
                continue
            }
            i++
        }
        return out
    }

    /** "삼백이" -> 302, "삼백십이" -> 312, "삼공이" -> 302 (digit by digit), "이백" -> 200. */
    fun parseKorean(run: String): Int? {
        if (run.isEmpty()) return null
        if (run.none { it in units }) {
            val digits = run.map { digitWords[it] ?: return null }
            return digits.joinToString("").toIntOrNull()
        }
        var total = 0
        var pending = -1
        for (c in run) {
            val d = digitWords[c]
            val u = units[c]
            when {
                d != null -> { if (pending >= 0) return null; pending = d }
                u != null -> { total += (if (pending < 0) 1 else pending) * u; pending = -1 }
                else -> return null
            }
        }
        if (pending >= 0) total += pending
        return total
    }

    fun contains(normalized: String, token: String): Boolean {
        val t = normalize(token)
        return t.isNotEmpty() && normalized.contains(t)
    }

    /** Which vocabulary items appear in an utterance. */
    fun used(utterance: String, vocab: Collection<VocabSpec>): Set<String> {
        val n = normalize(utterance)
        return vocab.filter { v -> v.forms.any { contains(n, it) } }.map { it.id }.toSet()
    }
}

sealed class MatchResult {
    data class Matched(val intent: IntentSpec, val heard: String) : MatchResult()
    /** More than one meaning fits; ask the learner which one he meant. */
    data class Ambiguous(val candidates: List<IntentSpec>, val heard: String) : MatchResult()
    data class NoMatch(val heard: String) : MatchResult()
}

object IntentMatcher {

    data class Scored(val intent: IntentSpec, val score: Int)

    /** Scores every intent of a turn against one utterance. Returns matches, best first. */
    fun score(utterance: String, intents: List<IntentSpec>): List<Scored> {
        val n = KoText.normalize(utterance)
        if (n.isEmpty()) return emptyList()
        val nums = KoText.numbers(n)
        val out = mutableListOf<Scored>()
        for (intent in intents) {
            if (intent.examples.any { KoText.normalize(it) == n }) { out += Scored(intent, 100); continue }
            if (intent.none.any { KoText.contains(n, it) }) continue
            var score = 0
            if (intent.any.isNotEmpty()) {
                val best = intent.any.filter { g -> g.all { KoText.contains(n, it) } }.maxOfOrNull { it.size }
                    ?: continue
                score += best
            }
            val want = intent.number
            val avoid = intent.numberNot
            val hasNumberRule = want != null || avoid != null
            if (want != null) {
                if (nums.none { it.toString() == want }) continue
                score += 1
            }
            if (avoid != null) {
                if (nums.isEmpty() || nums.all { it.toString() == avoid }) continue
                score += 1
            }
            if (intent.noNumber && nums.isNotEmpty()) continue
            if (intent.any.isEmpty() && !hasNumberRule) continue
            out += Scored(intent, score)
        }
        return out.sortedWith(compareByDescending<Scored> { it.score }.thenByDescending { it.intent.priority })
    }

    private fun best(utterance: String, intents: List<IntentSpec>): List<IntentSpec> {
        val scored = score(utterance, intents)
        if (scored.isEmpty()) return emptyList()
        val top = scored.first()
        // Ties at the same score and priority that lead to different places are genuinely ambiguous.
        return scored.filter { it.score == top.score && it.intent.priority == top.intent.priority }
            .map { it.intent }.distinctBy { it.next }
    }

    /**
     * Matches recognizer alternatives (best first). The first alternative that matches decides,
     * unless it is itself ambiguous or the next matching alternative points somewhere else.
     */
    fun match(alternatives: List<String>, intents: List<IntentSpec>): MatchResult {
        val alts = alternatives.map { it.trim() }.filter { it.isNotEmpty() }
        if (alts.isEmpty()) return MatchResult.NoMatch("")
        val matched = alts.map { it to best(it, intents) }.filter { it.second.isNotEmpty() }
        if (matched.isEmpty()) return MatchResult.NoMatch(alts.first())
        val (heard, firstBest) = matched.first()
        if (firstBest.size > 1) return MatchResult.Ambiguous(firstBest, heard)
        val second = matched.getOrNull(1)?.second?.firstOrNull()
        val primary = firstBest.first()
        // Only the top two recognizer guesses are considered for disagreement.
        val secondIsTopTwo = matched.getOrNull(1)?.first?.let { alts.indexOf(it) <= 1 } == true
        if (second != null && secondIsTopTwo && second.next != primary.next && second.id != primary.id) {
            return MatchResult.Ambiguous(listOf(primary, second), heard)
        }
        return MatchResult.Matched(primary, heard)
    }
}
