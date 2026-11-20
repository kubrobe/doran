package com.doranalley.core

import java.security.MessageDigest

/** Hangul helpers and the pronunciation score. Pure Kotlin so it is unit tested on the JVM. */
object Hangul {
    private const val BASE = 0xAC00
    private const val LAST = 0xD7A3

    fun isSyllable(c: Char) = c.code in BASE..LAST

    /** Only Hangul syllables, no spaces or punctuation. */
    fun syllables(s: String): String = s.filter { isSyllable(it) }

    /**
     * Jamo codes for comparison. Vowels that sound the same in modern Seoul Korean are merged
     * (ㅐ/ㅔ, ㅒ/ㅖ, ㅙ/ㅚ/ㅞ) so the learner is never marked down for a spelling-only difference.
     */
    fun jamo(s: String): IntArray {
        val out = ArrayList<Int>(s.length * 3)
        for (c in s) {
            if (!isSyllable(c)) continue
            val code = c.code - BASE
            val l = code / 588
            var v = (code % 588) / 28
            val t = code % 28
            v = when (v) {
                5 -> 1 // ㅔ -> ㅐ
                7 -> 3 // ㅖ -> ㅒ
                10, 11, 15 -> 15 // ㅙ ㅚ ㅞ
                else -> v
            }
            out += 100 + l
            out += 200 + v
            if (t != 0) out += 300 + t
        }
        return out.toIntArray()
    }

    fun levenshtein(a: IntArray, b: IntArray): Int {
        var prev = IntArray(b.size + 1) { it }
        var cur = IntArray(b.size + 1)
        for (i in 1..a.size) {
            cur[0] = i
            for (j in 1..b.size) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.size]
    }

    /** 0.0 .. 1.0, how close what was heard is to the target, measured letter by letter. */
    fun similarity(target: String, heard: String): Double {
        val t = jamo(target)
        val h = jamo(heard)
        if (t.isEmpty()) return if (h.isEmpty()) 1.0 else 0.0
        val d = levenshtein(t, h)
        return (1.0 - d.toDouble() / maxOf(t.size, h.size)).coerceIn(0.0, 1.0)
    }

    // ------------------------------------------------------------ numbers
    // The recognizer writes numbers as digits ("12시에"). Expand them into the ways they are read.

    private val sinoDigit = arrayOf("", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구")
    private val nativeOnes = arrayOf("", "하나", "둘", "셋", "넷", "다섯", "여섯", "일곱", "여덟", "아홉")
    private val nativeOnesShort = arrayOf("", "한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉")
    private val nativeTens = arrayOf("", "열", "스물", "서른", "마흔", "쉰", "예순", "일흔", "여든", "아흔")

    fun sino(n: Long): String {
        if (n == 0L) return "영"
        val units = listOf(100_000_000L to "억", 10_000L to "만")
        var rest = n
        val sb = StringBuilder()
        for ((u, name) in units) {
            if (rest >= u) { val q = rest / u; sb.append(if (q == 1L && name == "만") "" else sino4(q.toInt())).append(name); rest %= u }
        }
        if (rest > 0) sb.append(sino4(rest.toInt()))
        return sb.toString()
    }

    private fun sino4(n: Int): String {
        val sb = StringBuilder()
        val parts = listOf(1000 to "천", 100 to "백", 10 to "십")
        var r = n
        for ((u, name) in parts) {
            val d = r / u
            if (d > 0) { if (d > 1) sb.append(sinoDigit[d]); sb.append(name) }
            r %= u
        }
        if (r > 0) sb.append(sinoDigit[r])
        return sb.toString()
    }

    fun native(n: Int, short: Boolean): String? {
        if (n !in 1..99) return null
        val t = n / 10
        val o = n % 10
        if (n == 20 && short) return "스무"
        return nativeTens[t] + (if (short) nativeOnesShort[o] else nativeOnes[o])
    }

    /** All readings of the digits in [s] (capped), e.g. "12시" -> 십이시, 열두시, 열둘시. */
    fun expandDigits(s: String): List<String> {
        val runs = Regex("[0-9]+").findAll(s).toList()
        if (runs.isEmpty()) return listOf(s)
        var outs = listOf("")
        var last = 0
        for (m in runs) {
            val before = s.substring(last, m.range.first)
            val n = m.value.toLongOrNull() ?: 0L
            val opts = buildSet {
                add(sino(n))
                if (n in 1..99) { native(n.toInt(), false)?.let(::add); native(n.toInt(), true)?.let(::add) }
            }
            outs = outs.flatMap { o -> opts.map { o + before + it } }.take(64)
            last = m.range.last + 1
        }
        val tail = s.substring(last)
        return outs.map { it + tail }
    }

    /** Like [similarity], but digits in what was heard are read in the way that fits best. */
    fun bestSimilarity(target: String, heard: String): Double =
        expandDigits(heard).maxOf { similarity(target, it) }

    fun bestReading(target: String, heard: String): String =
        expandDigits(heard).maxByOrNull { similarity(target, it) } ?: heard

    enum class Mark { GOOD, CLOSE, MISSED }

    /**
     * Colours each syllable of the target: GOOD (heard exactly), CLOSE (most letters right), MISSED.
     * Non-Hangul characters (spaces, punctuation) are returned as GOOD so the sentence keeps its shape.
     */
    fun markSyllables(target: String, heard: String): List<Pair<Char, Mark>> {
        val ts = syllables(target)
        val hs = syllables(heard)
        val n = ts.length
        val m = hs.length
        // Alignment over syllables with a soft substitution cost based on jamo similarity.
        val cost = Array(n + 1) { DoubleArray(m + 1) }
        for (i in 0..n) cost[i][0] = i.toDouble()
        for (j in 0..m) cost[0][j] = j.toDouble()
        for (i in 1..n) for (j in 1..m) {
            val sub = 1.0 - similarity(ts[i - 1].toString(), hs[j - 1].toString())
            cost[i][j] = minOf(cost[i - 1][j] + 1, cost[i][j - 1] + 1, cost[i - 1][j - 1] + sub)
        }
        val marks = Array(n) { Mark.MISSED }
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val sub = 1.0 - similarity(ts[i - 1].toString(), hs[j - 1].toString())
            if (kotlin.math.abs(cost[i][j] - (cost[i - 1][j - 1] + sub)) < 1e-9) {
                marks[i - 1] = when {
                    sub < 1e-9 -> Mark.GOOD
                    sub <= 0.34 -> Mark.CLOSE
                    else -> Mark.MISSED
                }
                i--; j--
            } else if (kotlin.math.abs(cost[i][j] - (cost[i - 1][j] + 1)) < 1e-9) {
                i--
            } else {
                j--
            }
        }
        var k = 0
        return target.map { c ->
            if (isSyllable(c)) c to marks[k++] else c to Mark.GOOD
        }
    }

    /** File name stem of the bundled audio for a Korean text. Must match tools/build_course.py. */
    fun audioKey(text: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }.substring(0, 12)
    }
}

/** Result of one spoken attempt. */
data class SpeechResult(val heard: String, val score: Int) {
    val passed: Boolean get() = score >= PASS_SCORE
    companion object {
        const val PASS_SCORE = 65
        fun of(target: String, heard: String): SpeechResult {
            val reading = Hangul.bestReading(target, heard)
            return SpeechResult(reading, (Hangul.similarity(target, reading) * 100).toInt())
        }
    }
}
