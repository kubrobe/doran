package com.doranalley.core

import org.json.JSONObject
import java.time.LocalDate

data class ItemStat(val seen: Int = 0, val wrong: Int = 0, val streak: Int = 0, val last: Long = 0L) {
    /** 0..1, how well this item is known. */
    val strength: Double get() = if (seen == 0) 0.0 else (0.25 * streak + 0.5 * (seen - wrong).coerceAtLeast(0) / seen).coerceIn(0.0, 1.0)
}

data class Settings(
    val sounds: Boolean = true,
    val speaking: Boolean = true,
    val listening: Boolean = true,
    val unlimitedHearts: Boolean = false,
    /** "dark" (default), "light" or "system". */
    val theme: String = "dark",
)

/** Everything saved on the phone. Pure data + rules; [Store] persists it. */
data class Progress(
    val onboarded: Boolean = false,
    val dailyGoal: Int = 20,
    /** unit id -> number of finished nodes (0..lessonCount). */
    val unitLessons: Map<String, Int> = emptyMap(),
    /** "yyyy-mm-dd" -> XP earned that day. */
    val xpByDay: Map<String, Int> = emptyMap(),
    val streak: Int = 0,
    val lastStreakDay: String? = null,
    val longestStreak: Int = 0,
    val hearts: Int = MAX_HEARTS,
    val heartsUpdatedAt: Long = 0L,
    val items: Map<String, ItemStat> = emptyMap(),
    val settings: Settings = Settings(),
    val speakingPausedUntil: Long = 0L,
    val listeningPausedUntil: Long = 0L,
    val lessonsCompleted: Int = 0,
    val perfectLessons: Int = 0,
) {
    companion object {
        const val MAX_HEARTS = 5
        const val HEART_REFILL_MS = 20L * 60 * 1000
        const val PAUSE_MS = 15L * 60 * 1000

        fun fromJson(s: String): Progress {
            val o = JSONObject(s)
            fun map(name: String): Map<String, Int> = o.optJSONObject(name)?.let { m -> m.keys().asSequence().associateWith { m.getInt(it) } } ?: emptyMap()
            val items = o.optJSONObject("items")?.let { m ->
                m.keys().asSequence().associateWith { k ->
                    val a = m.getJSONObject(k)
                    ItemStat(a.optInt("s"), a.optInt("w"), a.optInt("k"), a.optLong("t"))
                }
            } ?: emptyMap()
            val st = o.optJSONObject("settings") ?: JSONObject()
            return Progress(
                onboarded = o.optBoolean("onboarded"),
                dailyGoal = o.optInt("dailyGoal", 20),
                unitLessons = map("unitLessons"),
                xpByDay = map("xpByDay"),
                streak = o.optInt("streak"),
                lastStreakDay = o.optString("lastStreakDay").ifEmpty { null },
                longestStreak = o.optInt("longestStreak"),
                hearts = o.optInt("hearts", MAX_HEARTS),
                heartsUpdatedAt = o.optLong("heartsUpdatedAt"),
                items = items,
                settings = Settings(
                    sounds = st.optBoolean("sounds", true),
                    speaking = st.optBoolean("speaking", true),
                    listening = st.optBoolean("listening", true),
                    unlimitedHearts = st.optBoolean("unlimitedHearts", false),
                    theme = st.optString("theme", "dark").ifBlank { "dark" },
                ),
                speakingPausedUntil = o.optLong("speakingPausedUntil"),
                listeningPausedUntil = o.optLong("listeningPausedUntil"),
                lessonsCompleted = o.optInt("lessonsCompleted"),
                perfectLessons = o.optInt("perfectLessons"),
            )
        }
    }

    fun toJson(): String {
        val o = JSONObject()
        o.put("onboarded", onboarded)
        o.put("dailyGoal", dailyGoal)
        o.put("unitLessons", JSONObject(unitLessons))
        o.put("xpByDay", JSONObject(xpByDay))
        o.put("streak", streak)
        o.put("lastStreakDay", lastStreakDay ?: "")
        o.put("longestStreak", longestStreak)
        o.put("hearts", hearts)
        o.put("heartsUpdatedAt", heartsUpdatedAt)
        val it = JSONObject()
        items.forEach { (k, v) -> it.put(k, JSONObject().put("s", v.seen).put("w", v.wrong).put("k", v.streak).put("t", v.last)) }
        o.put("items", it)
        o.put("settings", JSONObject()
            .put("sounds", settings.sounds).put("speaking", settings.speaking)
            .put("listening", settings.listening).put("unlimitedHearts", settings.unlimitedHearts)
            .put("theme", settings.theme))
        o.put("speakingPausedUntil", speakingPausedUntil)
        o.put("listeningPausedUntil", listeningPausedUntil)
        o.put("lessonsCompleted", lessonsCompleted)
        o.put("perfectLessons", perfectLessons)
        return o.toString()
    }

    // ------------------------------------------------------------ path

    fun lessonsDone(u: CourseUnit) = (unitLessons[u.id] ?: 0).coerceIn(0, u.lessonCount)
    fun unitDone(u: CourseUnit) = lessonsDone(u) >= u.lessonCount

    /** The node the learner should do next, or null when the whole course is finished. */
    fun current(course: Course): Pair<CourseUnit, Int>? =
        course.units.firstOrNull { !unitDone(it) }?.let { it to lessonsDone(it) }

    fun isUnlocked(course: Course, u: CourseUnit, lesson: Int): Boolean {
        val cur = current(course) ?: return true
        return u.index < cur.first.index || (u.index == cur.first.index && lesson <= cur.second)
    }

    fun studiedUnits(course: Course): List<CourseUnit> {
        val cur = current(course)
        return course.units.filter { u -> lessonsDone(u) > 0 || (cur != null && u.index < cur.first.index) || cur == null }
    }

    fun completeNode(u: CourseUnit, lesson: Int): Progress {
        val done = lessonsDone(u)
        if (lesson != done) return this
        return copy(unitLessons = unitLessons + (u.id to (done + 1)))
    }

    /** Placement: every unit before [unitIndex] is finished. */
    fun placeAt(course: Course, unitIndex: Int): Progress {
        val m = unitLessons.toMutableMap()
        course.units.forEach { u -> if (u.index < unitIndex) m[u.id] = u.lessonCount }
        return copy(unitLessons = m)
    }

    // ------------------------------------------------------------ XP and streak

    fun xpOn(day: LocalDate) = xpByDay[day.toString()] ?: 0

    fun addXp(xp: Int, today: LocalDate): Progress {
        val key = today.toString()
        val newDays = (xpByDay + (key to ((xpByDay[key] ?: 0) + xp)))
            .filterKeys { runCatching { LocalDate.parse(it) }.getOrNull()?.isAfter(today.minusDays(400)) == true }
        val last = lastStreakDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val newStreak = when {
            last == today -> streak
            last == today.minusDays(1) -> streak + 1
            else -> 1
        }
        return copy(xpByDay = newDays, streak = newStreak, lastStreakDay = key, longestStreak = maxOf(longestStreak, newStreak))
    }

    /** Streak as the learner sees it: it is broken once a whole day is missed. */
    fun visibleStreak(today: LocalDate): Int {
        val last = lastStreakDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 0
        return if (last == today || last == today.minusDays(1)) streak else 0
    }

    fun extendedToday(today: LocalDate) = lastStreakDay == today.toString()

    val totalXp: Int get() = xpByDay.values.sum()

    // ------------------------------------------------------------ hearts

    /** Hearts after refilling for the time that has passed. */
    fun heartsNow(now: Long): Int {
        if (settings.unlimitedHearts) return MAX_HEARTS
        if (hearts >= MAX_HEARTS) return MAX_HEARTS
        val gained = ((now - heartsUpdatedAt).coerceAtLeast(0) / HEART_REFILL_MS).toInt()
        return (hearts + gained).coerceAtMost(MAX_HEARTS)
    }

    /** Milliseconds until the next heart, or 0 when full. */
    fun nextHeartIn(now: Long): Long {
        if (heartsNow(now) >= MAX_HEARTS) return 0
        val elapsed = (now - heartsUpdatedAt).coerceAtLeast(0) % HEART_REFILL_MS
        return HEART_REFILL_MS - elapsed
    }

    private fun withHearts(h: Int, now: Long): Progress {
        val current = heartsNow(now)
        // Keep the refill clock running from the last full refill step.
        val anchor = if (current >= MAX_HEARTS) now else heartsUpdatedAt + ((now - heartsUpdatedAt).coerceAtLeast(0) / HEART_REFILL_MS) * HEART_REFILL_MS
        return copy(hearts = h.coerceIn(0, MAX_HEARTS), heartsUpdatedAt = anchor)
    }

    fun loseHeart(now: Long): Progress = if (settings.unlimitedHearts) this else withHearts(heartsNow(now) - 1, now)
    fun gainHeart(now: Long): Progress = withHearts(heartsNow(now) + 1, now)
    fun refillHearts(now: Long): Progress = copy(hearts = MAX_HEARTS, heartsUpdatedAt = now)

    // ------------------------------------------------------------ items

    fun record(itemId: String, correct: Boolean, now: Long): Progress {
        val s = items[itemId] ?: ItemStat()
        val n = ItemStat(s.seen + 1, s.wrong + if (correct) 0 else 1, if (correct) s.streak + 1 else 0, now)
        return copy(items = items + (itemId to n))
    }

    fun weakness(): Map<String, Double> = items.mapValues { (_, s) -> 1.0 - s.strength + if (s.streak == 0 && s.wrong > 0) 1.0 else 0.0 }

    // ------------------------------------------------------------ speaking / listening pause

    fun filter(now: Long) = ExerciseFilter(
        speaking = settings.speaking && now >= speakingPausedUntil,
        listening = settings.listening && now >= listeningPausedUntil,
    )
}
