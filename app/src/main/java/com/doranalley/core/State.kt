package com.doranalley.core

/** How the learner's spoken replies are captured. */
enum class InputMode(val code: String) {
    /** Use Korean speech recognition when the device supports it, else record and self-confirm. */
    AUTO("auto"),
    /** Always record, then the learner confirms what he meant. */
    RECORD("record"),
    /** No microphone: choose the reply. */
    LISTEN("listen");

    companion object { fun of(c: String?) = entries.firstOrNull { it.code == c } ?: AUTO }
}

data class Settings(
    val lang: String = "uz",
    val inputMode: InputMode = InputMode.AUTO,
    /** null = follow the system animation setting. */
    val reducedMotion: Boolean? = null,
    val allowSynthetic: Boolean = true,
    /** Test tool: shifts the app clock so next-day recall can be checked today. */
    val timeOffsetHours: Long = 0,
    /** Learner-adjusted review intervals; null = use config. */
    val intervalsHours: List<Double>? = null,
    val showGlossAfterExchange: Boolean = true,
)

data class DiaryEntry(
    val id: String,
    val createdAt: Long,
    /** baseline | weekly | mirror */
    val kind: String,
    val promptId: String?,
    val phraseKo: String?,
    val file: String,
    val durationMs: Long,
)

data class Meta(
    val activeEpisode: String? = null,
    val completed: List<String> = emptyList(),
    val unlocks: List<String> = emptyList(),
    /** Mirror ladder step per phrase: 0 none, 1 heard, 2 imitated, 3 said from meaning. */
    val ladder: Map<String, Int> = emptyMap(),
    val recallVariantsDone: List<String> = emptyList(),
)

/** Converts state to and from JSON-ready maps. */
object Codec {

    fun settings(s: Settings): Map<String, Any?> = linkedMapOf(
        "lang" to s.lang, "inputMode" to s.inputMode.code, "reducedMotion" to s.reducedMotion,
        "allowSynthetic" to s.allowSynthetic, "timeOffsetHours" to s.timeOffsetHours,
        "intervalsHours" to s.intervalsHours, "showGlossAfterExchange" to s.showGlossAfterExchange,
    )

    fun settings(j: JObj): Settings = Settings(
        lang = j.strOrNull("lang")?.takeIf { it in SUPPORTED_LANGS } ?: "uz",
        inputMode = InputMode.of(j.strOrNull("inputMode")),
        reducedMotion = j["reducedMotion"] as? Boolean,
        allowSynthetic = j.bool("allowSynthetic", true),
        timeOffsetHours = j.long("timeOffsetHours"),
        intervalsHours = (j["intervalsHours"] as? List<*>)?.map { (it as Number).toDouble() },
        showGlossAfterExchange = j.bool("showGlossAfterExchange", true),
    )

    fun meta(m: Meta): Map<String, Any?> = linkedMapOf(
        "activeEpisode" to m.activeEpisode, "completed" to m.completed, "unlocks" to m.unlocks,
        "ladder" to m.ladder, "recallVariantsDone" to m.recallVariantsDone,
    )

    fun meta(j: JObj): Meta = Meta(
        activeEpisode = j.strOrNull("activeEpisode"), completed = j.strs("completed"), unlocks = j.strs("unlocks"),
        ladder = j.objOrNull("ladder")?.mapValues { (it.value as Number).toInt() } ?: emptyMap(),
        recallVariantsDone = j.strs("recallVariantsDone"),
    )

    fun diary(d: DiaryEntry): Map<String, Any?> = linkedMapOf(
        "id" to d.id, "createdAt" to d.createdAt, "kind" to d.kind, "promptId" to d.promptId,
        "phraseKo" to d.phraseKo, "file" to d.file, "durationMs" to d.durationMs,
    )

    fun diary(j: JObj) = DiaryEntry(
        j.str("id"), j.long("createdAt"), j.str("kind"), j.strOrNull("promptId"), j.strOrNull("phraseKo"),
        j.str("file"), j.long("durationMs"),
    )

    fun item(i: ReviewItem): Map<String, Any?> = linkedMapOf(
        "id" to i.id, "ko" to i.ko, "meaning" to i.meaning, "audio" to i.audio, "source" to i.source,
        "contexts" to i.contexts.map { linkedMapOf("ko" to it.ko, "gloss" to it.gloss, "where" to it.where, "at" to it.at) },
        "introducedAt" to i.introducedAt, "stage" to i.stage, "nextReview" to i.nextReview,
        "lastPracticed" to i.lastPracticed,
        "history" to i.history.map {
            linkedMapOf("at" to it.at, "help" to it.help, "evidence" to it.evidence, "result" to it.result, "setting" to it.setting)
        },
        "independentDays" to i.independentDays,
    )

    fun item(j: JObj) = ReviewItem(
        id = j.str("id"), ko = j.str("ko"), meaning = j.loc("meaning"), audio = j.strOrNull("audio"),
        source = j.strOrNull("source") ?: "lesson",
        contexts = j.objs("contexts").map { ItemContext(it.str("ko"), it.loc("gloss"), it.strOrNull("where") ?: "", it.long("at")) },
        introducedAt = j.long("introducedAt"), stage = j.int("stage"), nextReview = j.long("nextReview"),
        lastPracticed = (j["lastPracticed"] as? Number)?.toLong(),
        history = j.objs("history").map { HelpEvent(it.long("at"), it.int("help"), it.str("evidence"), it.str("result"), it.strOrNull("setting") ?: "") },
        independentDays = j.strs("independentDays"),
    )

    fun outcome(o: TurnOutcome): Map<String, Any?> = linkedMapOf(
        "episodeId" to o.episodeId, "turnId" to o.turnId, "intentId" to o.intentId, "success" to o.success,
        "clarification" to o.clarification, "targets" to o.targets, "used" to o.used, "help" to o.help,
        "evidence" to o.evidence, "heard" to o.heard, "attempts" to o.attempts, "at" to o.at,
    )

    fun outcome(j: JObj) = TurnOutcome(
        j.str("episodeId"), j.str("turnId"), j.str("intentId"), j.bool("success"), j.bool("clarification"),
        j.strs("targets"), j.strs("used"), j.int("help"), j.str("evidence"), j.strOrNull("heard"),
        j.int("attempts"), j.long("at"),
    )

    fun progress(p: Progress): Map<String, Any?> = linkedMapOf(
        "episodeId" to p.episodeId, "nodeId" to p.nodeId,
        "transcript" to p.transcript.map {
            linkedMapOf(
                "kind" to it.kind, "speaker" to it.speaker, "ko" to it.ko, "text" to it.text, "nodeId" to it.nodeId,
                "words" to it.words, "audio" to it.audio, "unlocked" to it.unlocked, "evidence" to it.evidence, "help" to it.help,
            )
        },
        "positions" to p.positions,
        "turns" to p.turns.mapValues { linkedMapOf("hint" to it.value.hint, "attempts" to it.value.attempts) },
        "flags" to p.flags.toList(), "outcomes" to p.outcomes.map(::outcome),
        "finished" to p.finished, "startedAt" to p.startedAt, "updatedAt" to p.updatedAt,
    )

    @Suppress("UNCHECKED_CAST")
    fun progress(j: JObj) = Progress(
        episodeId = j.str("episodeId"), nodeId = j.str("nodeId"),
        transcript = j.objs("transcript").map {
            TranscriptEntry(
                it.str("kind"), it.strOrNull("speaker"), it.strOrNull("ko"), it.loc("text"), it.str("nodeId"),
                it.strs("words"), it.strOrNull("audio"), it.bool("unlocked"), it.strOrNull("evidence"), it.int("help"),
            )
        },
        positions = j.strMap("positions"),
        turns = (j.objOrNull("turns") ?: emptyMap()).mapValues { (_, v) -> (v as JObj).let { TurnMemory(it.int("hint"), it.int("attempts")) } },
        flags = j.strs("flags").toSet(), outcomes = j.objs("outcomes").map(::outcome),
        finished = j.bool("finished"), startedAt = j.long("startedAt"), updatedAt = j.long("updatedAt"),
    )
}
