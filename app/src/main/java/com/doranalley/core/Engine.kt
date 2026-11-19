package com.doranalley.core

data class TranscriptEntry(
    /** partner | learner | narration */
    val kind: String,
    val speaker: String?,
    val ko: String?,
    /** For partner lines: gloss. For learner lines: the meaning he confirmed. For narration: the text. */
    val text: L,
    val nodeId: String,
    val words: List<String>,
    val audio: String?,
    /** Partner lines become tappable (meaning + save) only after the exchange is over. */
    val unlocked: Boolean,
    val evidence: String?,
    val help: Int,
)

data class TurnMemory(val hint: Int, val attempts: Int)

data class TurnOutcome(
    val episodeId: String,
    val turnId: String,
    val intentId: String,
    /** Communication success: the partner understood and the goal moved forward. */
    val success: Boolean,
    val clarification: Boolean,
    val targets: List<String>,
    /** Target expressions actually produced (recognized or self-confirmed). Separate from success. */
    val used: List<String>,
    val help: Int,
    val evidence: String,
    val heard: String?,
    val attempts: Int,
    val at: Long,
)

data class Progress(
    val episodeId: String,
    val nodeId: String,
    val transcript: List<TranscriptEntry>,
    val positions: Map<String, String>,
    val turns: Map<String, TurnMemory>,
    val flags: Set<String>,
    val outcomes: List<TurnOutcome>,
    val finished: Boolean,
    val startedAt: Long,
    val updatedAt: Long,
)

data class Reflection(
    val independent: List<String>,
    val assisted: List<String>,
    val notYet: List<String>,
    val clarifications: Int,
    val goals: Int,
    val takeaway: String?,
    val practicePoint: L,
    val unlocks: List<String>,
)

/** Pure state machine for one episode. Every function returns a new Progress. */
class Engine(val episode: Episode) {

    fun start(now: Long) = Progress(
        episodeId = episode.id, nodeId = episode.start, transcript = emptyList(),
        positions = episode.initialPositions, turns = emptyMap(), flags = emptySet(),
        outcomes = emptyList(), finished = false, startedAt = now, updatedAt = now,
    )

    fun current(p: Progress): Node = episode.node(p.nodeId)

    fun turn(p: Progress): Node.Turn? = current(p) as? Node.Turn

    fun memory(p: Progress): TurnMemory = p.turns[p.nodeId] ?: TurnMemory(0, 0)

    /** True for nodes that play by themselves (or after a tap for narration). Reaching End finishes the episode. */
    fun isAuto(p: Progress) = current(p).let {
        it is Node.Narration || it is Node.Line || it is Node.Move || (it is Node.End && !p.finished)
    }

    /** Consumes one narration/line/move node. Turns wait for the learner; End marks the episode finished. */
    fun step(p: Progress, now: Long): Progress = when (val n = current(p)) {
        is Node.Narration -> p.copy(
            nodeId = n.next, updatedAt = now,
            transcript = p.transcript + TranscriptEntry("narration", null, null, n.text, n.id, emptyList(), null, true, null, 0),
        )
        is Node.Line -> p.copy(
            nodeId = n.next, updatedAt = now,
            positions = if (p.positions[n.speaker] == null || p.positions[n.speaker] == "off")
                p.positions + (n.speaker to (p.positions["learner"] ?: "off")) else p.positions,
            transcript = p.transcript + TranscriptEntry("partner", n.speaker, n.ko, n.gloss, n.id, n.words, n.audio, false, null, 0),
        )
        is Node.Move -> p.copy(nodeId = n.next, positions = p.positions + n.moves, updatedAt = now)
        is Node.End -> p.copy(finished = true, transcript = unlockAll(p.transcript), updatedAt = now)
        is Node.Turn -> p
    }

    /** Raises the hint level for the current turn (cue -> start -> model). */
    fun hint(p: Progress, now: Long): Progress {
        val t = turn(p) ?: return p
        val m = memory(p)
        return p.copy(turns = p.turns + (t.id to m.copy(hint = (m.hint + 1).coerceAtMost(Help.MODEL))), updatedAt = now)
    }

    fun match(p: Progress, alternatives: List<String>): MatchResult {
        val t = turn(p) ?: return MatchResult.NoMatch(alternatives.firstOrNull() ?: "")
        return IntentMatcher.match(alternatives, t.intents)
    }

    /**
     * Applies the learner's confirmed reply.
     * @param used target expressions produced (detected in recognized text, or ticked by the learner)
     */
    fun resolve(
        p: Progress, intentId: String, heard: String?, used: Set<String>, evidence: Evidence, now: Long,
    ): Pair<Progress, TurnOutcome> {
        val t = turn(p) ?: error("Not at a turn")
        val intent = t.intents.firstOrNull { it.id == intentId } ?: error("Unknown intent $intentId")
        val m = memory(p)
        val produced = if (evidence == Evidence.LISTENING_ONLY) emptyList() else t.targets.filter { it in used }
        val outcome = TurnOutcome(
            episodeId = episode.id, turnId = t.id, intentId = intent.id, success = intent.success,
            clarification = intent.clarification, targets = t.targets, used = produced, help = m.hint,
            evidence = evidence.code, heard = heard, attempts = m.attempts + 1, at = now,
        )
        val flags = p.flags + intent.sets
        val next = intent.nextWhen.firstOrNull { b -> b.flags.all { it in flags } }?.next ?: intent.next
        val learner = TranscriptEntry("learner", "learner", heard, intent.meaning, t.id, emptyList(), null, true, evidence.code, m.hint)
        // Help used stays on record until the turn succeeds, even across clarification loops.
        val turns = if (intent.success) p.turns - t.id else p.turns + (t.id to m.copy(attempts = m.attempts + 1))
        val np = p.copy(
            nodeId = next, flags = flags, turns = turns, outcomes = p.outcomes + outcome,
            transcript = unlockAll(p.transcript) + learner, updatedAt = now,
        )
        return np to outcome
    }

    /** The partner did not understand: show what was heard and go to the turn's clarification branch. */
    fun noMatch(p: Progress, heard: String?, evidence: Evidence, now: Long): Progress {
        val t = turn(p) ?: return p
        val m = memory(p)
        val learner = TranscriptEntry("learner", "learner", heard, emptyMap(), t.id, emptyList(), null, true, evidence.code, m.hint)
        return p.copy(
            nodeId = t.fallback, turns = p.turns + (t.id to m.copy(attempts = m.attempts + 1)),
            transcript = unlockAll(p.transcript) + learner, updatedAt = now,
        )
    }

    fun reflection(p: Progress): Reflection {
        val end = episode.nodes.values.filterIsInstance<Node.End>().firstOrNull()
        val independent = mutableSetOf<String>()
        val assisted = mutableSetOf<String>()
        for (o in p.outcomes) {
            if (o.evidence == Evidence.LISTENING_ONLY.code) continue
            if (o.help >= Help.MODEL) { assisted += o.targets; continue }
            for (u in o.used) if (o.help == Help.NONE) independent += u else assisted += u
        }
        assisted -= independent
        val notYet = episode.targets.filter { it !in independent && it !in assisted }
        return Reflection(
            independent = episode.targets.filter { it in independent },
            assisted = episode.targets.filter { it in assisted },
            notYet = notYet,
            clarifications = p.outcomes.count { it.clarification },
            goals = p.outcomes.count { it.success },
            takeaway = end?.takeaway, practicePoint = end?.practicePoint ?: emptyMap(),
            unlocks = end?.unlocks ?: emptyList(),
        )
    }

    private fun unlockAll(t: List<TranscriptEntry>) = t.map { if (it.unlocked) it else it.copy(unlocked = true) }
}
