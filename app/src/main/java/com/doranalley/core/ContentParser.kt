package com.doranalley.core

/**
 * Turns content JSON into model objects. Pure Kotlin: the Android layer only supplies file text.
 *
 * Placeholders in Korean lines and intent tokens keep campus names easy to change:
 *   {b:haksaeng}  -> building's Korean name from campus.json
 *   {campus}      -> campus Korean name
 *   {var:place}   -> a recall-variant variable
 */
object ContentParser {

    fun campus(text: String): Campus {
        val j = Json.parseObject(text)
        val buildings = j.objs("buildings").map {
            Building(
                id = it.str("id"), ko = it.str("ko"), name = it.loc("name"),
                x = it.double("x", 0.5).toFloat(), color = color(it.strOrNull("color"), 0xFFC8A27A),
                floors = it.int("floors", 3),
            )
        }
        val locations = j.objs("locations").associate {
            it.str("id") to Location(
                id = it.str("id"), backdrop = it.strOrNull("backdrop") ?: "street",
                ko = it.str("ko"), name = it.loc("name"), building = it.strOrNull("building"),
                sign = it.strOrNull("sign"), showMap = it.bool("showMap"),
            )
        }
        val c = Campus(
            nameKo = j.str("nameKo"), name = j.loc("name"),
            neighborhoodKo = j.str("neighborhoodKo"), neighborhood = j.loc("neighborhood"),
            buildings = buildings, locations = emptyMap(),
        )
        // Location signs may also use placeholders.
        return c.copy(locations = locations.mapValues { (_, l) ->
            l.copy(ko = subst(l.ko, c, emptyMap()), sign = l.sign?.let { subst(it, c, emptyMap()) })
        })
    }

    fun characters(text: String): Map<String, CharacterSpec> =
        Json.parseObject(text).objs("characters").associate {
            it.str("id") to CharacterSpec(
                id = it.str("id"), nameKo = it.str("nameKo"), name = it.loc("name"),
                role = it.loc("role"), relationship = it.loc("relationship"),
                color = color(it.strOrNull("color"), 0xFF6B8F71), hair = color(it.strOrNull("hair"), 0xFF2B2420),
                pitch = it.double("pitch", 1.0).toFloat(), rate = it.double("rate", 1.0).toFloat(),
                recurring = it.bool("recurring"), unlockedByDefault = it.bool("unlocked", true),
            )
        }

    fun vocab(text: String, campus: Campus): Map<String, VocabSpec> =
        Json.parseObject(text).objs("vocab").associate {
            val ko = subst(it.str("ko"), campus, emptyMap())
            it.str("id") to VocabSpec(
                id = it.str("id"), ko = ko, meaning = it.loc("meaning"),
                forms = it.strs("forms").map { f -> subst(f, campus, emptyMap()) }.ifEmpty { listOf(ko) },
                audio = it.strOrNull("audio"), note = it.loc("note"), target = it.bool("target"),
            )
        }

    fun index(text: String): List<EpisodeIndexEntry> =
        Json.parseObject(text).objs("episodes").map {
            EpisodeIndexEntry(
                id = it.str("id"), file = it.strOrNull("file"), title = it.loc("title"),
                summary = it.loc("summary"), status = it.strOrNull("status") ?: "planned",
                setting = it.loc("setting"),
            )
        }

    fun ui(text: String): Map<String, L> =
        Json.parseObject(text).mapValues { (_, v) ->
            @Suppress("UNCHECKED_CAST")
            (v as Map<String, Any?>).mapValues { it.value.toString() }
        }

    fun config(text: String): AppConfig {
        val j = Json.parseObject(text)
        val r = j.objOrNull("review") ?: emptyMap()
        val review = ReviewConfig(
            intervalsHours = r.arr("intervalsHours").map { (it as Number).toDouble() }
                .ifEmpty { ReviewConfig().intervalsHours },
            afterLastHours = r.double("afterLastHours", 336.0),
            difficultHours = r.double("difficultHours", 1.0),
        )
        return AppConfig(
            review = review,
            daily = j.objs("daily").map { DailyBlock(it.int("minutes"), it.loc("label"), it.strOrNull("action") ?: "home") },
            diaryPrompts = j.objs("diaryPrompts").map {
                DiaryPrompt(it.str("id"), it.strOrNull("kind") ?: "weekly", it.loc("text"), it.strOrNull("koStarter") ?: "")
            },
            sideQuestMinutes = j.int("sideQuestMinutes", 5),
        )
    }

    /** Parses an episode file. Returns the main episode followed by its recall variants. */
    fun episodes(text: String, campus: Campus): List<Episode> {
        val j = Json.parseObject(text)
        val main = episode(j, campus, emptyMap(), parent = null, variantLabel = emptyMap())
        val recall = j.objOrNull("recall") ?: return listOf(main)
        val variants = recall.objs("variants").map { RecallVariant(it.str("id"), it.loc("label"), it.strMap("vars")) }
        val recallEpisodes = variants.map { v ->
            val body = LinkedHashMap(recall)
            body["id"] = "${main.id}__recall_${v.id}"
            body["title"] = recall["title"] ?: j["title"]
            body["objective"] = recall["objective"] ?: j["objective"]
            body["targets"] = recall["targets"] ?: j["targets"]
            body["review"] = j["review"]
            episode(body, campus, v.vars, parent = main.id, variantLabel = v.label)
        }
        return listOf(main) + recallEpisodes
    }

    private fun episode(j: JObj, campus: Campus, vars: Map<String, String>, parent: String?, variantLabel: L): Episode {
        fun s(t: String) = subst(t, campus, vars)
        fun sl(l: L) = l.mapValues { s(it.value) }
        val nodes = j.objs("nodes").associate { n ->
            val id = n.str("id")
            val node: Node = when (val type = n.str("type")) {
                "narration" -> Node.Narration(id, sl(n.loc("text")), n.str("next"))
                "line" -> Node.Line(
                    id, n.str("speaker"), s(n.str("ko")), sl(n.loc("gloss")), n.strOrNull("audio"),
                    n.strs("words"), n.strOrNull("pose") ?: "idle", n.str("next"),
                )
                "move" -> Node.Move(id, n.strMap("moves").mapValues { s(it.value) }, n.str("next"))
                "turn" -> {
                    val h = n.obj("hints")
                    Node.Turn(
                        id = id, partner = n.str("partner"), task = sl(n.loc("task")),
                        targets = n.strs("targets"),
                        intents = n.objs("intents").map { intent(it, ::s, ::sl) },
                        fallback = n.str("fallback"),
                        hints = Hints(sl(h.loc("cue")), s(h.str("start")), s(h.str("model")), h.strOrNull("modelAudio")),
                    )
                }
                "end" -> Node.End(id, n.str("takeaway"), sl(n.loc("practicePoint")), n.strs("unlocks"))
                else -> error("Unknown node type '$type' in $id")
            }
            id to node
        }
        return Episode(
            id = j.str("id"), title = j.loc("title"), objective = sl(j.loc("objective")),
            targets = j.strs("targets"), start = j.str("start"),
            initialPositions = j.strMap("positions"), nodes = nodes,
            mirrorPhrases = j.objs("mirror").map {
                MirrorPhrase(it.str("id"), s(it.str("ko")), sl(it.loc("gloss")), it.strOrNull("audio"),
                    sl(it.loc("listenFor")), it.strOrNull("vocab"))
            },
            reviewStatus = j.objOrNull("review")?.strOrNull("status") ?: "draft",
            parent = parent, variantLabel = variantLabel,
        )
    }

    private fun intent(j: JObj, s: (String) -> String, sl: (L) -> L): IntentSpec = IntentSpec(
        id = j.str("id"), meaning = sl(j.loc("meaning")),
        any = j.arr("any").map { g -> (g as List<*>).map { s(it.toString()) } },
        none = j.strs("none").map(s),
        number = j.strOrNull("number"), numberNot = j.strOrNull("numberNot"), noNumber = j.bool("noNumber"),
        examples = j.strs("examples").map(s),
        next = j.str("next"),
        nextWhen = j.objs("nextWhen").map { Branch(it.strs("flags"), it.str("next")) },
        sets = j.strs("sets"), success = j.bool("success"), clarification = j.bool("clarification"),
        priority = j.int("priority"), manual = j.bool("manual", true),
    )

    // Android's regex engine (ICU) requires a literal "}" to be escaped, unlike desktop Java.
    private val placeholder = Regex("\\{(b|var):([a-zA-Z0-9_]+)\\}|\\{campus\\}")

    fun subst(text: String, campus: Campus, vars: Map<String, String>): String {
        // Variables may themselves contain building placeholders, so resolve up to three levels.
        var out = text
        repeat(3) {
            val next = substOnce(out, campus, vars)
            if (next == out) return out
            out = next
        }
        return out
    }

    private fun substOnce(text: String, campus: Campus, vars: Map<String, String>): String =
        placeholder.replace(text) { m ->
            when {
                m.value == "{campus}" -> campus.nameKo
                m.groupValues[1] == "b" -> campus.building(m.groupValues[2])?.ko ?: m.value
                else -> vars[m.groupValues[2]] ?: m.value
            }
        }

    private fun color(hex: String?, def: Long): Long {
        if (hex == null) return def
        val h = hex.removePrefix("#")
        return when (h.length) {
            6 -> 0xFF000000 or h.toLong(16)
            8 -> h.toLong(16)
            else -> def
        }
    }
}

/** Checks an episode for broken links and missing translations. Returns human-readable problems. */
object ContentValidator {
    fun validate(
        ep: Episode,
        characters: Map<String, CharacterSpec>,
        vocab: Map<String, VocabSpec>,
        campus: Campus,
    ): List<String> {
        val p = mutableListOf<String>()
        fun link(from: String, to: String) { if (to !in ep.nodes) p += "${ep.id}/$from -> missing node '$to'" }
        fun langs(where: String, l: L) {
            SUPPORTED_LANGS.filter { l[it].isNullOrBlank() }.forEach { p += "${ep.id}/$where missing '$it'" }
        }
        fun place(where: String, loc: String) {
            if (loc != "off" && campus.location(loc) == null) p += "${ep.id}/$where unknown location '$loc'"
        }
        if (ep.start !in ep.nodes) p += "${ep.id}: start node '${ep.start}' missing"
        ep.targets.forEach { if (it !in vocab) p += "${ep.id}: unknown target vocab '$it'" }
        ep.initialPositions.forEach { (who, loc) -> place("positions.$who", loc) }
        langs("title", ep.title); langs("objective", ep.objective)
        if (Regex("\\{(b|var):").containsMatchIn(ep.nodes.values.joinToString { it.toString() }))
            p += "${ep.id}: unresolved placeholder"
        for (n in ep.nodes.values) when (n) {
            is Node.Narration -> { link(n.id, n.next); langs(n.id, n.text) }
            is Node.Line -> {
                link(n.id, n.next); langs("${n.id}.gloss", n.gloss)
                if (n.speaker !in characters) p += "${ep.id}/${n.id}: unknown speaker '${n.speaker}'"
                n.words.forEach { if (it !in vocab) p += "${ep.id}/${n.id}: unknown word '$it'" }
            }
            is Node.Move -> { link(n.id, n.next); n.moves.forEach { (who, loc) -> place(n.id, loc)
                if (who != "learner" && who !in characters) p += "${ep.id}/${n.id}: unknown character '$who'" } }
            is Node.Turn -> {
                link(n.id, n.fallback); langs("${n.id}.task", n.task); langs("${n.id}.hints.cue", n.hints.cue)
                if (n.partner !in characters) p += "${ep.id}/${n.id}: unknown partner '${n.partner}'"
                n.targets.forEach { if (it !in vocab) p += "${ep.id}/${n.id}: unknown target '$it'" }
                if (n.intents.none { it.success }) p += "${ep.id}/${n.id}: no successful intent"
                n.intents.forEach { i ->
                    link("${n.id}.${i.id}", i.next); i.nextWhen.forEach { link("${n.id}.${i.id}", it.next) }
                    langs("${n.id}.${i.id}.meaning", i.meaning)
                }
            }
            is Node.End -> { langs("${n.id}.practicePoint", n.practicePoint)
                if (n.takeaway !in vocab) p += "${ep.id}/${n.id}: unknown takeaway '${n.takeaway}'" }
        }
        ep.mirrorPhrases.forEach { langs("mirror.${it.id}.gloss", it.gloss); langs("mirror.${it.id}.listenFor", it.listenFor) }
        // Every node must be reachable from the start.
        val seen = mutableSetOf<String>()
        val stack = ArrayDeque(listOf(ep.start))
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!seen.add(id)) continue
            when (val n = ep.nodes[id]) {
                is Node.Narration -> stack += n.next
                is Node.Line -> stack += n.next
                is Node.Move -> stack += n.next
                is Node.Turn -> { stack += n.fallback; n.intents.forEach { i -> stack += i.next; i.nextWhen.forEach { stack += it.next } } }
                else -> {}
            }
        }
        (ep.nodes.keys - seen).forEach { p += "${ep.id}: unreachable node '$it'" }
        if (ep.nodes.values.none { it is Node.End }) p += "${ep.id}: no end node"
        return p
    }
}
