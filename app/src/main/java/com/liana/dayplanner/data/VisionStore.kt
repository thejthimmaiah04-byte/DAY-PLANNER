package com.liana.dayplanner.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Milestone(
    val id: String,
    val title: String,
    val target: String = "",   // "YYYY-MM"
    val done: Boolean = false
)

data class Vision(
    val statement: String = "",
    val target: String = "",   // "YYYY-MM"
    val milestones: List<Milestone> = emptyList()
)

object VisionStore {
    private const val PREFS = "arc_vision"

    fun load(context: Context): Vision {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val milestones = try {
            val arr = JSONArray(sp.getString("milestones", "[]") ?: "[]")
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Milestone(o.getString("id"), o.getString("title"),
                    o.optString("target"), o.optBoolean("done"))
            }
        } catch (_: Exception) { emptyList() }
        return Vision(
            statement = sp.getString("statement", "") ?: "",
            target = sp.getString("target", "") ?: "",
            milestones = milestones
        )
    }

    fun save(context: Context, vision: Vision) {
        val arr = JSONArray()
        vision.milestones.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title)
                .put("target", it.target).put("done", it.done))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("statement", vision.statement)
            .putString("target", vision.target)
            .putString("milestones", arr.toString())
            .apply()
    }
}
