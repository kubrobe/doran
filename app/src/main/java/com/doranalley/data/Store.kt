package com.doranalley.data

import android.content.Context
import com.doranalley.core.Codec
import com.doranalley.core.DiaryEntry
import com.doranalley.core.JObj
import com.doranalley.core.Json
import com.doranalley.core.Meta
import com.doranalley.core.Progress
import com.doranalley.core.ReviewItem
import com.doranalley.core.Settings
import com.doranalley.core.TurnOutcome
import com.doranalley.core.objs
import java.io.File

/**
 * Local persistence: small JSON files in the app's private storage.
 * Nothing leaves the device. Each write goes to a temp file first, then replaces the old one.
 */
class Store(context: Context) {
    private val dir: File = context.filesDir
    val diaryDir: File = File(dir, "diary").apply { mkdirs() }
    val scratchDir: File = File(context.cacheDir, "takes").apply { mkdirs() }

    private fun readObj(name: String): JObj? {
        val f = File(dir, name)
        if (!f.exists()) return null
        return runCatching { Json.parseObject(f.readText(Charsets.UTF_8)) }.getOrNull()
    }

    @Synchronized
    private fun write(name: String, value: Any?) {
        val f = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(Json.write(value, pretty = false), Charsets.UTF_8)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }

    private fun readList(name: String): List<JObj> = readObj(name)?.objs("items") ?: emptyList()
    private fun writeList(name: String, items: List<Map<String, Any?>>) = write(name, mapOf("items" to items))

    fun loadSettings(): Settings = readObj("settings.json")?.let { runCatching { Codec.settings(it) }.getOrNull() } ?: Settings()
    fun saveSettings(s: Settings) = write("settings.json", Codec.settings(s))

    fun loadMeta(): Meta = readObj("meta.json")?.let { runCatching { Codec.meta(it) }.getOrNull() } ?: Meta()
    fun saveMeta(m: Meta) = write("meta.json", Codec.meta(m))

    fun loadReview(): Map<String, ReviewItem> =
        readList("review.json").mapNotNull { runCatching { Codec.item(it) }.getOrNull() }.associateBy { it.id }
    fun saveReview(items: Map<String, ReviewItem>) = writeList("review.json", items.values.map(Codec::item))

    fun loadOutcomes(): List<TurnOutcome> = readList("outcomes.json").mapNotNull { runCatching { Codec.outcome(it) }.getOrNull() }
    fun saveOutcomes(o: List<TurnOutcome>) = writeList("outcomes.json", o.map(Codec::outcome))

    fun loadDiary(): List<DiaryEntry> = readList("diary.json").mapNotNull { runCatching { Codec.diary(it) }.getOrNull() }
        .filter { File(diaryDir, it.file).exists() }
    fun saveDiary(d: List<DiaryEntry>) = writeList("diary.json", d.map(Codec::diary))

    private fun progressName(episodeId: String) = "progress_${episodeId.replace(Regex("[^A-Za-z0-9_]"), "_")}.json"
    fun loadProgress(episodeId: String): Progress? =
        readObj(progressName(episodeId))?.let { runCatching { Codec.progress(it) }.getOrNull() }
    fun saveProgress(p: Progress) = write(progressName(p.episodeId), Codec.progress(p))
    fun clearProgress(episodeId: String) { File(dir, progressName(episodeId)).delete() }

    fun diaryFile(name: String) = File(diaryDir, name)

    /** Keeps diary recordings unless asked; clears everything else. */
    fun resetProgress(keepDiary: Boolean) {
        dir.listFiles()?.filter { it.isFile && it.name != "settings.json" && (!keepDiary || it.name != "diary.json") }?.forEach { it.delete() }
        if (!keepDiary) diaryDir.listFiles()?.forEach { it.delete() }
        scratchDir.listFiles()?.forEach { it.delete() }
    }
}
