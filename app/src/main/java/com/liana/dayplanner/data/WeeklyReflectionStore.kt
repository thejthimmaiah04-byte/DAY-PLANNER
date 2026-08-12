package com.liana.dayplanner.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class WeeklyReflection(
    val weekOf: String,   // ISO date of the Monday of that week
    val bestDay: String,
    val obstacle: String,
    val adjustment: String,
    val savedAt: Long = System.currentTimeMillis()
)

object WeeklyReflectionStore {
    private const val PREFS = "arc_reflections"
    private const val KEY = "reflections"

    fun load(context: Context): List<WeeklyReflection> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            val arr = JSONArray(sp.getString(KEY, "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                WeeklyReflection(
                    weekOf = o.getString("weekOf"),
                    bestDay = o.optString("bestDay"),
                    obstacle = o.optString("obstacle"),
                    adjustment = o.optString("adjustment"),
                    savedAt = o.optLong("savedAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, r: WeeklyReflection) {
        val existing = load(context).toMutableList()
        existing.removeAll { it.weekOf == r.weekOf }
        existing.add(0, r)
        val arr = JSONArray()
        existing.take(12).forEach { ref ->
            arr.put(JSONObject().apply {
                put("weekOf", ref.weekOf)
                put("bestDay", ref.bestDay)
                put("obstacle", ref.obstacle)
                put("adjustment", ref.adjustment)
                put("savedAt", ref.savedAt)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString()).apply()
    }

    fun hasReflectionThisWeek(context: Context): Boolean =
        load(context).any { it.weekOf == currentWeekOf() }

    fun currentWeekOf(): String {
        val d = LocalDate.now()
        return d.minusDays(d.dayOfWeek.value.toLong() - 1).toString()
    }
}
