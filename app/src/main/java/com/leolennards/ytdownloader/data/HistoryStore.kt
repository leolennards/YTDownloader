package com.leolennards.ytdownloader.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// saves the history as json in SharedPreferences. enough for this app, no database needed
object HistoryStore {
    private const val PREFS = "history"
    private const val KEY = "entries"

    fun load(context: Context): List<HistoryEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                HistoryEntry(
                    title = o.getString("title"),
                    format = MediaFormat.valueOf(o.getString("format")),
                    qualityLabel = if (o.isNull("quality")) null else o.getString("quality"),
                    sizeBytes = o.getLong("size"),
                    savedAtMs = o.getLong("savedAt"),
                    uri = o.getString("uri"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("title", e.title)
                    .put("format", e.format.name)
                    .put("quality", e.qualityLabel ?: JSONObject.NULL)
                    .put("size", e.sizeBytes)
                    .put("savedAt", e.savedAtMs)
                    .put("uri", e.uri),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
