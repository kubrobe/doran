package com.doranalley.core

/** Localized text keyed by language code: "uz" (Latin), "ru", "en". */
typealias L = Map<String, String>

fun L.pick(lang: String): String = this[lang] ?: this["en"] ?: values.firstOrNull() ?: ""

enum class Lang(val code: String, val label: String, val short: String) {
    UZ("uz", "O‘zbekcha", "UZ"),
    RU("ru", "Русский", "RU"),
    EN("en", "English", "EN");

    companion object {
        fun of(code: String?): Lang = entries.firstOrNull { it.code == code } ?: EN
    }
}

val SUPPORTED_LANGS = listOf("uz", "ru", "en")

// ---------- Campus ----------

data class Building(
    val id: String,
    val ko: String,
    val name: L,
    val x: Float,
    val color: Long,
    val floors: Int,
)

data class Location(
    val id: String,
    /** "corridor" (indoors) or "street" (the campus street). */
    val backdrop: String,
    val ko: String,
    val name: L,
    /** Building in front of which characters stand on the street. */
    val building: String?,
    /** Readable sign drawn on a door or wall, e.g. "201호 강의실". */
    val sign: String?,
    val showMap: Boolean,
)

data class Campus(
    val nameKo: String,
    val name: L,
    val neighborhoodKo: String,
    val neighborhood: L,
    val buildings: List<Building>,
    val locations: Map<String, Location>,
) {
    fun building(id: String?): Building? = buildings.firstOrNull { it.id == id }
    fun location(id: String?): Location? = locations[id]
}

// ---------- Characters & vocabulary ----------

data class CharacterSpec(
    val id: String,
    val nameKo: String,
    val name: L,
    val role: L,
    val relationship: L,
    val color: Long,
    val hair: Long,
    val pitch: Float,
    val rate: Float,
    val recurring: Boolean,
    val unlockedByDefault: Boolean,
)

data class VocabSpec(
    val id: String,
    val ko: String,
    val meaning: L,
    /** Surface forms that count as "used" when found in what the learner said. */
    val forms: List<String>,
    val audio: String?,
    val note: L,
    val target: Boolean,
)

// ---------- Episodes ----------

data class Hints(val cue: L, val start: String, val model: String, val modelAudio: String?)

data class Branch(val flags: List<String>, val next: String)

data class IntentSpec(
    val id: String,
    /** What the learner meant, shown in his language for confirmation and manual choice. */
    val meaning: L,
    /** Any one group must be fully present (all tokens of the group). Empty = no token requirement. */
    val any: List<List<String>>,
    /** None of these may be present. */
    val none: List<String>,
    /** If set, the reply must contain this number (e.g. a room number). */
    val number: String?,
    /** If set, the reply must contain some number that is NOT this one. */
    val numberNot: String?,
    /** If true, the reply must not contain any number. */
    val noNumber: Boolean,
    val examples: List<String>,
    val next: String,
    val nextWhen: List<Branch>,
    val sets: List<String>,
    /** True when this reply moves the real-world goal forward. */
    val success: Boolean,
    /** True when the learner asked to repeat/clarify/slow down. */
    val clarification: Boolean,
    val priority: Int,
    /** Shown in the manual "what did you mean" list. */
    val manual: Boolean,
)

sealed class Node {
    abstract val id: String

    data class Narration(override val id: String, val text: L, val next: String) : Node()

    data class Line(
        override val id: String,
        val speaker: String,
        val ko: String,
        val gloss: L,
        val audio: String?,
        val words: List<String>,
        val pose: String,
        val next: String,
    ) : Node()

    /** Moves characters ("learner" or a character id) to locations; "off" removes them from view. */
    data class Move(override val id: String, val moves: Map<String, String>, val next: String) : Node()

    data class Turn(
        override val id: String,
        val partner: String,
        val task: L,
        val targets: List<String>,
        val intents: List<IntentSpec>,
        val fallback: String,
        val hints: Hints,
    ) : Node()

    data class End(
        override val id: String,
        val takeaway: String,
        val practicePoint: L,
        val unlocks: List<String>,
    ) : Node()
}

data class MirrorPhrase(
    val id: String,
    val ko: String,
    val gloss: L,
    val audio: String?,
    val listenFor: L,
    val vocab: String?,
)

data class RecallVariant(val id: String, val label: L, val vars: Map<String, String>)

data class Episode(
    val id: String,
    val title: L,
    val objective: L,
    val targets: List<String>,
    val start: String,
    val initialPositions: Map<String, String>,
    val nodes: Map<String, Node>,
    val mirrorPhrases: List<MirrorPhrase>,
    val reviewStatus: String,
    /** For recall variants: the parent episode id. */
    val parent: String?,
    val variantLabel: L,
) {
    fun node(id: String): Node = nodes[id] ?: error("Episode $id has no node '$id'")
}

data class EpisodeIndexEntry(
    val id: String,
    val file: String?,
    val title: L,
    val summary: L,
    /** "playable" or "planned". */
    val status: String,
    val setting: L,
)

data class DailyBlock(val minutes: Int, val label: L, val action: String)

data class DiaryPrompt(val id: String, val kind: String, val text: L, val koStarter: String)

data class ReviewConfig(
    /** Later that day, next day, three days later, a week later. Adjustable pilot timings. */
    val intervalsHours: List<Double> = listOf(4.0, 24.0, 72.0, 168.0),
    val afterLastHours: Double = 336.0,
    /** How soon a difficult item (model peeked / not yet) comes back. */
    val difficultHours: Double = 1.0,
) {
    fun intervalHours(stage: Int): Double =
        if (stage < intervalsHours.size) intervalsHours[stage.coerceAtLeast(0)] else afterLastHours
}

data class AppConfig(
    val review: ReviewConfig,
    val daily: List<DailyBlock>,
    val diaryPrompts: List<DiaryPrompt>,
    val sideQuestMinutes: Int,
)
