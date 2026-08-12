package com.liana.dayplanner.data

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

object VisionCheckStore {
    private const val PREFS = "arc_vision_checks"
    private const val KEY_CHECKS = "checks"
    private const val KEY_LAST_REVIEW = "lastReviewEpochDay"

    fun saveCheck(context: Context, date: LocalDate, answer: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = try {
            JSONObject(sp.getString(KEY_CHECKS, "{}") ?: "{}")
        } catch (_: Exception) { JSONObject() }
        obj.put(date.toString(), answer)
        // prune older than 90 days
        val cutoff = date.minusDays(90)
        obj.keys().asSequence().toList().forEach { k ->
            runCatching { if (LocalDate.parse(k).isBefore(cutoff)) obj.remove(k) }
        }
        sp.edit().putString(KEY_CHECKS, obj.toString()).apply()
    }

    fun getChecks(context: Context): Map<String, String> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = try {
            JSONObject(sp.getString(KEY_CHECKS, "{}") ?: "{}")
        } catch (_: Exception) { JSONObject() }
        return buildMap { obj.keys().forEach { k -> put(k, obj.getString(k)) } }
    }

    fun getStreak(context: Context): Int {
        val checks = getChecks(context)
        var cursor = LocalDate.now()
        if (checks[cursor.toString()] == null) cursor = cursor.minusDays(1)
        var streak = 0
        while (true) {
            val ans = checks[cursor.toString()] ?: break
            if (ans == "no") break
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun getTodayCheck(context: Context): String? =
        getChecks(context)[LocalDate.now().toString()]

    fun saveLastReview(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_REVIEW, LocalDate.now().toEpochDay()).apply()
    }

    fun daysSinceLastReview(context: Context): Long? {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!sp.contains(KEY_LAST_REVIEW)) return null
        return LocalDate.now().toEpochDay() - sp.getLong(KEY_LAST_REVIEW, 0)
    }
}
