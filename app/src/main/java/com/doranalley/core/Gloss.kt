package com.doranalley.core

/**
 * Tap-for-meaning hints, like the dotted words in Duolingo.
 * Korean: a word from the course or the glossary, optionally followed by a particle or ending.
 * English: the Korean words whose meaning is exactly that English word.
 */
class Gloss(words: List<Word>, extra: Map<String, String>) {
    private val ko: Map<String, List<String>>
    private val en: Map<String, List<String>>

    init {
        val m = LinkedHashMap<String, MutableList<String>>()
        fun add(k: String, v: String) { val l = m.getOrPut(k) { mutableListOf() }; if (v !in l) l += v }
        words.forEach { add(it.ko, it.en) }
        extra.forEach { (k, v) -> add(k, v) }
        ko = m
        val e = LinkedHashMap<String, MutableList<String>>()
        words.forEach { w ->
            val key = w.en.substringBefore(" (").lowercase().trim()
            if (key.isNotEmpty() && !key.contains(' ')) { val l = e.getOrPut(key) { mutableListOf() }; if (w.ko !in l) l += w.ko }
        }
        en = e
    }

    companion object {
        /** Particles and endings that can follow a word, with what they do. Longest first. */
        val SUFFIXES: List<Pair<String, String>> = listOf(
            "이에요" to "is / am / are", "예요" to "is / am / are", "이세요" to "is (polite)", "이었어요" to "was", "였어요" to "was",
            "에서" to "at / in (where it happens)", "에게" to "to (someone)", "한테" to "to (someone)", "까지" to "until / to",
            "부터" to "from", "으로" to "to / by / with", "하고" to "and / with", "께서" to "(polite subject)",
            "은" to "(topic)", "는" to "(topic)", "이" to "(subject)", "가" to "(subject)", "을" to "(object)", "를" to "(object)",
            "에" to "at / to / in", "도" to "also / too", "의" to "of / 's", "로" to "to / by / with", "께" to "to (polite)",
            "요" to "(polite ending)",
        ).sortedByDescending { it.first.length }

        private fun clean(t: String) = t.trim().trim('.', ',', '?', '!', '“', '”', '"')
    }

    /** Meaning lines for a Korean word as it appears in a sentence, or null when unknown. */
    fun korean(token: String): List<String>? {
        val t = clean(token)
        if (t.isEmpty()) return null
        ko[t]?.let { return it.map { m -> "$t = $m" } }
        for ((suf, meaning) in SUFFIXES) {
            if (t.length > suf.length && t.endsWith(suf)) {
                val stem = t.dropLast(suf.length)
                ko[stem]?.let { ms -> return ms.map { "$stem = $it" } + "$suf = $meaning" }
            }
        }
        return null
    }

    /** Korean words for an English word, or null. */
    fun english(token: String): List<String>? {
        val t = clean(token).lowercase()
        return en[t]?.map { "$it" }
    }
}
