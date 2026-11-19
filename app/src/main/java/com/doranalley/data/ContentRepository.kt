package com.doranalley.data

import android.content.Context
import com.doranalley.core.AppConfig
import com.doranalley.core.Campus
import com.doranalley.core.CharacterSpec
import com.doranalley.core.ContentParser
import com.doranalley.core.Episode
import com.doranalley.core.EpisodeIndexEntry
import com.doranalley.core.L
import com.doranalley.core.VocabSpec
import java.io.IOException

/** Everything the app shows, loaded from assets/. Adding an episode = adding a JSON file + an index entry. */
class Content(
    val campus: Campus,
    val characters: Map<String, CharacterSpec>,
    val vocab: Map<String, VocabSpec>,
    val config: AppConfig,
    val ui: Map<String, L>,
    val index: List<EpisodeIndexEntry>,
    val episodes: Map<String, Episode>,
) {
    fun recallVariants(parentId: String): List<Episode> = episodes.values.filter { it.parent == parentId }
    fun mainEpisodes(): List<Episode> = episodes.values.filter { it.parent == null }
    fun targetVocab(): List<VocabSpec> = vocab.values.filter { it.target }
}

object ContentRepository {

    fun load(context: Context): Content {
        fun read(path: String) = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val campus = ContentParser.campus(read("config/campus.json"))
        val index = ContentParser.index(read("episodes/index.json"))
        val episodes = index.filter { it.status == "playable" && it.file != null }
            .flatMap { ContentParser.episodes(read(it.file!!), campus) }
            .associateBy { it.id }
        return Content(
            campus = campus,
            characters = ContentParser.characters(read("config/characters.json")),
            vocab = ContentParser.vocab(read("config/vocab.json"), campus),
            config = ContentParser.config(read("config/app.json")),
            ui = ContentParser.ui(read("i18n/ui.json")),
            index = index,
            episodes = episodes,
        )
    }

    private val existsCache = HashMap<String, Boolean>()

    /** Bundled, reviewed recordings live under assets/audio/. Missing files fall back to a marked synthetic preview. */
    fun hasAudio(context: Context, path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return synchronized(existsCache) {
            existsCache.getOrPut(path) {
                try { context.assets.open("audio/$path").close(); true } catch (e: IOException) { false }
            }
        }
    }
}
