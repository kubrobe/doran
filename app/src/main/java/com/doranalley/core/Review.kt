package com.doranalley.core

import java.time.Instant
import java.time.ZoneId

/** How much help the learner needed during one production attempt. */
object Help {
    const val NONE = 0
    /** Meaning cue in his language. */
    const val CUE = 1
    /** Beginning of the Korean expression. */
    const val START = 2
    /** Full model line with audio. */
    const val MODEL = 3
}

/** How the app knows what the learner said. Labelled honestly in the UI. */
enum class Evidence(val code: String) {
    /** Speech recognizer output, confirmed by the learner. */
    RECOGNIZED("recognized"),
    /** Learner recorded or spoke and then told the app what he meant. */
    SELF_CONFIRMED("self_confirmed"),
    /** Microphone declined: the learner chose a reply without speaking. No recall credit. */
    LISTENING_ONLY("listening_only");

    companion object { fun of(code: String?) = entries.firstOrNull { it.code == code } ?: SELF_CONFIRMED }
}

data class ItemContext(val ko: String, val gloss: L, val where: String, val at: Long)

data class HelpEvent(
    val at: Long,
    val help: Int,
    val evidence: String,
    /** independent | assisted | model | paraphrase | listening */
    val result: String,
    /** Episode or quest where it happened, so settings can be varied. */
    val setting: String,
)

data class ReviewItem(
    /** Language-independent id (vocab id, or "saved:<vocab>"). Never depends on the UI language. */
    val id: String,
    val ko: String,
    val meaning: L,
    val audio: String?,
    /** "lesson" or "saved". */
    val source: String,
    val contexts: List<ItemContext>,
    val introducedAt: Long,
    val stage: Int,
    val nextReview: Long,
    val lastPracticed: Long?,
    val history: List<HelpEvent>,
    /** Distinct local days (yyyy-MM-dd) with an independent use. */
    val independentDays: List<String>,
)

object Scheduler {
    private const val HOUR = 3_600_000L

    fun dayKey(at: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toString()

    fun introduce(
        id: String, ko: String, meaning: L, audio: String?, source: String,
        context: ItemContext?, now: Long, cfg: ReviewConfig,
    ): ReviewItem = ReviewItem(
        id = id, ko = ko, meaning = meaning, audio = audio, source = source,
        contexts = listOfNotNull(context), introducedAt = now, stage = 0,
        // First review: later the same day.
        nextReview = now + (cfg.intervalHours(0) * HOUR).toLong(),
        lastPracticed = null, history = emptyList(), independentDays = emptyList(),
    )

    fun addContext(item: ReviewItem, ctx: ItemContext): ReviewItem =
        if (item.contexts.any { it.ko == ctx.ko }) item else item.copy(contexts = item.contexts + ctx)

    fun isDue(item: ReviewItem, now: Long) = item.nextReview <= now

    fun due(items: Collection<ReviewItem>, now: Long): List<ReviewItem> =
        items.filter { isDue(it, now) }.sortedBy { it.nextReview }

    /**
     * Records one practice opportunity.
     * @param produced the learner said the expression himself (as heard or self-confirmed)
     * @param help the highest help level used before producing it
     */
    fun record(
        item: ReviewItem, now: Long, help: Int, produced: Boolean, evidence: Evidence,
        setting: String, cfg: ReviewConfig, zone: ZoneId = ZoneId.systemDefault(),
    ): ReviewItem {
        val result = when {
            evidence == Evidence.LISTENING_ONLY -> "listening"
            help >= Help.MODEL -> "model"
            !produced -> "paraphrase"
            help > Help.NONE -> "assisted"
            else -> "independent"
        }
        val event = HelpEvent(now, help, evidence.code, result, setting)
        val due = isDue(item, now)
        fun at(stage: Int) = now + (cfg.intervalHours(stage) * HOUR).toLong()
        val base = item.copy(history = item.history + event)
        return when (result) {
            // Communication succeeded without the target, or no speaking: keep the schedule as is,
            // so the expression stays available for a later practice opportunity.
            "listening", "paraphrase" -> base
            "independent" -> {
                val day = dayKey(now, zone)
                val days = if (day in item.independentDays) item.independentDays else item.independentDays + day
                if (due) {
                    val stage = item.stage + 1
                    base.copy(stage = stage, nextReview = at(stage), lastPracticed = now, independentDays = days)
                } else {
                    // Used before it was due (e.g. right after learning it): counts as a use, schedule unchanged.
                    base.copy(lastPracticed = now, independentDays = days)
                }
            }
            "assisted" -> {
                val stage = (item.stage - 1).coerceAtLeast(0)
                val next = if (due) at(stage) else minOf(item.nextReview, at(stage))
                base.copy(stage = stage, nextReview = next, lastPracticed = now)
            }
            else -> { // "model": difficult — bring it back sooner.
                base.copy(stage = 0, nextReview = now + (cfg.difficultHours * HOUR).toLong(), lastPracticed = now)
            }
        }
    }

    /** Picks a context different from the last one shown, so reviews vary the setting. */
    fun contextFor(item: ReviewItem): ItemContext? {
        if (item.contexts.isEmpty()) return null
        return item.contexts[item.history.size % item.contexts.size]
    }

    fun helpNeeded(item: ReviewItem): Int = item.history.lastOrNull()?.help ?: 0
}
