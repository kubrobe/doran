package com.doranalley.core

import org.json.JSONArray
import org.json.JSONObject

data class Letter(val id: String, val ch: String, val rom: String, val say: String, val hint: String, val group: String)
data class Word(val id: String, val ko: String, val en: String)
data class Sentence(
    val id: String,
    val ko: String,
    val en: String,
    val enAlt: List<List<String>>,
    val koTiles: List<String>,
    val enTiles: List<String>,
    val speak: Boolean,
)
data class Fill(
    val id: String,
    val before: String,
    val answer: String,
    val after: String,
    val en: String,
    val options: List<String>,
    val full: String,
)

data class CourseUnit(
    val index: Int,
    val id: String,
    val title: String,
    val section: String,
    val blurb: String,
    val tips: List<String>,
    val letters: List<Letter>,
    val words: List<Word>,
    val sentences: List<Sentence>,
    val fills: List<Fill>,
) {
    val isHangul: Boolean get() = letters.isNotEmpty()
    /** Number of nodes on the path for this unit. The last one is always the unit review. */
    val lessonCount: Int get() = if (isHangul) 3 else 4
    val number: Int get() = index + 1
}

class Course(val units: List<CourseUnit>, val glossaryExtra: Map<String, String> = emptyMap()) {
    /** Tap-for-meaning hints for every word in the course. */
    val gloss: Gloss by lazy { Gloss(units.flatMap { it.words }, glossaryExtra) }

    private val byId = units.associateBy { it.id }
    fun unit(id: String): CourseUnit? = byId[id]
    val sections: List<String> = units.map { it.section }.distinct()

    /** Every learnable item id -> a short label (for review lists). */
    val itemLabels: Map<String, String> by lazy {
        buildMap {
            units.forEach { u ->
                u.letters.forEach { put(it.id, it.ch) }
                u.words.forEach { put(it.id, it.ko) }
                u.sentences.forEach { put(it.id, it.ko) }
                u.fills.forEach { put(it.id, it.full) }
            }
        }
    }

    companion object {
        fun parse(json: String): Course {
            val root = JSONObject(json)
            val arr = root.getJSONArray("units")
            val units = (0 until arr.length()).map { i -> parseUnit(i, arr.getJSONObject(i)) }
            val g = root.optJSONObject("glossary")
            val extra = g?.keys()?.asSequence()?.associateWith { g.getString(it) } ?: emptyMap()
            return Course(units, extra)
        }

        private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

        private fun parseUnit(index: Int, o: JSONObject): CourseUnit {
            val letters = o.getJSONArray("letters").let { a ->
                (0 until a.length()).map { a.getJSONObject(it) }.map {
                    Letter(it.getString("id"), it.getString("ch"), it.getString("rom"), it.getString("say"),
                        it.getString("hint"), it.optString("group", ""))
                }
            }
            val words = o.getJSONArray("words").let { a ->
                (0 until a.length()).map { a.getJSONObject(it) }.map { Word(it.getString("id"), it.getString("ko"), it.getString("en")) }
            }
            val sentences = o.getJSONArray("sentences").let { a ->
                (0 until a.length()).map { a.getJSONObject(it) }.map { s ->
                    val alts = s.getJSONArray("enAlt").let { aa -> (0 until aa.length()).map { aa.getJSONArray(it).strings() } }
                    Sentence(s.getString("id"), s.getString("ko"), s.getString("en"), alts,
                        s.getJSONArray("koTiles").strings(), s.getJSONArray("enTiles").strings(), s.getBoolean("speak"))
                }
            }
            val fills = o.getJSONArray("fills").let { a ->
                (0 until a.length()).map { a.getJSONObject(it) }.map { f ->
                    Fill(f.getString("id"), f.getString("before"), f.getString("answer"), f.getString("after"),
                        f.getString("en"), f.getJSONArray("options").strings(), f.getString("full"))
                }
            }
            return CourseUnit(index, o.getString("id"), o.getString("title"), o.getString("section"), o.getString("blurb"),
                o.getJSONArray("tips").strings(), letters, words, sentences, fills)
        }
    }
}
