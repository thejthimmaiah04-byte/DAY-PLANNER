package com.liana.dayplanner.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ResearchProjectStore {
    private const val PREFS = "arc_research"
    private const val KEY = "projects"

    fun load(context: Context): List<ResearchProject> = try {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(json)
        (0 until arr.length()).map { parseProject(arr.getJSONObject(it)) }
    } catch (_: Exception) { emptyList() }

    fun save(context: Context, projects: List<ResearchProject>) {
        val arr = JSONArray()
        projects.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("title", p.title)
                put("tasks", encodeItems(p.tasks))
                put("targets", encodeItems(p.targets))
                put("notes", p.notes)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    private fun parseProject(o: JSONObject) = ResearchProject(
        id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
        title = o.getString("title"),
        tasks = parseItems(o.optJSONArray("tasks")),
        targets = parseItems(o.optJSONArray("targets")),
        notes = o.optString("notes", "")
    )

    private fun parseItems(arr: JSONArray?): List<ResearchItem> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ResearchItem(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                text = o.getString("text"),
                done = o.optBoolean("done")
            )
        }
    }

    private fun encodeItems(items: List<ResearchItem>): JSONArray {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done)) }
        return arr
    }
}
