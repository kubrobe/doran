package com.doranalley.data

import android.content.Context
import android.util.Log
import com.doranalley.core.Course
import com.doranalley.core.Progress
import java.io.File

/** Saves progress as one JSON file, written atomically so a crash never corrupts it. */
class Store(private val context: Context) {
    private val file get() = File(context.filesDir, FILE)

    fun load(): Progress = try {
        if (file.exists()) Progress.fromJson(file.readText()) else Progress()
    } catch (t: Throwable) {
        Log.e("Store", "progress unreadable, starting fresh", t)
        file.renameTo(File(context.filesDir, "$FILE.broken"))
        Progress()
    }

    fun save(p: Progress) {
        try {
            val tmp = File(context.filesDir, "$FILE.tmp")
            tmp.writeText(p.toJson())
            if (!tmp.renameTo(file)) {
                file.delete(); tmp.renameTo(file)
            }
        } catch (t: Throwable) {
            Log.e("Store", "could not save progress", t)
        }
    }

    fun loadCourse(): Course = Course.parse(context.assets.open("course/course.json").bufferedReader().use { it.readText() })

    companion object {
        const val FILE = "progress.json"
    }
}
