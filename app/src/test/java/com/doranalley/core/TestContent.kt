package com.doranalley.core

import java.io.File

/** Loads the real content files from src/main/assets so tests exercise what ships. */
object TestContent {
    private val root: File = listOf("src/main/assets", "app/src/main/assets")
        .map(::File).firstOrNull { it.isDirectory } ?: error("Run tests from the project or app directory")

    fun text(path: String) = File(root, path).readText()

    val campus by lazy { ContentParser.campus(text("config/campus.json")) }
    val characters by lazy { ContentParser.characters(text("config/characters.json")) }
    val vocab by lazy { ContentParser.vocab(text("config/vocab.json"), campus) }
    val config by lazy { ContentParser.config(text("config/app.json")) }
    val ui by lazy { ContentParser.ui(text("i18n/ui.json")) }
    val index by lazy { ContentParser.index(text("episodes/index.json")) }
    val episodes by lazy {
        index.filter { it.status == "playable" }.flatMap { ContentParser.episodes(text(it.file!!), campus) }
    }
    val ep01 by lazy { episodes.first { it.id == "ep01_classroom_moved" } }

    fun turn(ep: Episode, id: String) = ep.node(id) as Node.Turn
}
