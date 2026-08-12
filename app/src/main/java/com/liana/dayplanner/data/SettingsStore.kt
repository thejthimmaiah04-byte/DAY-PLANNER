package com.liana.dayplanner.data

import android.content.Context

/** All user-configurable settings. Times are minutes-from-midnight. */
data class AppSettings(
    val workStart: Int = 9 * 60,
    val workEnd: Int = 17 * 60,
    val dayStart: Int = 8 * 60,
    val dayEnd: Int = 22 * 60,
    val morningHour: Int = 6,
    val morningMinute: Int = 0,
    val eveningHour: Int = 21,
    val eveningMinute: Int = 0,
    val lightTheme: Boolean = false,
    val onboarded: Boolean = false
)

object SettingsStore {
    private const val PREFS = "arc_settings"

    // Process-wide cache so DayPlanner/receivers can read without threading a param everywhere.
    @Volatile private var cached: AppSettings? = null

    fun current(context: Context): AppSettings = cached ?: load(context)

    fun load(context: Context): AppSettings {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppSettings(
            workStart = sp.getInt("workStart", 9 * 60),
            workEnd = sp.getInt("workEnd", 17 * 60),
            dayStart = sp.getInt("dayStart", 8 * 60),
            dayEnd = sp.getInt("dayEnd", 22 * 60),
            morningHour = sp.getInt("morningHour", 6),
            morningMinute = sp.getInt("morningMinute", 0),
            eveningHour = sp.getInt("eveningHour", 21),
            eveningMinute = sp.getInt("eveningMinute", 0),
            lightTheme = sp.getBoolean("lightTheme", false),
            onboarded = sp.getBoolean("onboarded", false)
        ).also { cached = it }
    }

    fun save(context: Context, s: AppSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("workStart", s.workStart)
            .putInt("workEnd", s.workEnd)
            .putInt("dayStart", s.dayStart)
            .putInt("dayEnd", s.dayEnd)
            .putInt("morningHour", s.morningHour)
            .putInt("morningMinute", s.morningMinute)
            .putInt("eveningHour", s.eveningHour)
            .putInt("eveningMinute", s.eveningMinute)
            .putBoolean("lightTheme", s.lightTheme)
            .putBoolean("onboarded", s.onboarded)
            .apply()
        cached = s
    }
}
