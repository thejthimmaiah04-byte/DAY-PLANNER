package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.RepeatKind
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.data.VisionCheckStore
import com.liana.dayplanner.ui.BarcodeProgress
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.Periwinkle
import com.liana.dayplanner.ui.theme.Sage
import com.liana.dayplanner.ui.theme.Slate
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Terracotta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private data class HabitStat(
    val title: String, val repeatLabel: String, val streak: Int, val recent: List<Task>)

private fun habitStats(tasks: List<Task>): List<HabitStat> {
    val today = LocalDate.now().toEpochDay()
    return tasks.filter { it.repeat != null }
        .groupBy { it.seriesId ?: it.id }
        .values.map { list ->
            val sorted = list.sortedBy { it.dueEpochDay ?: Long.MAX_VALUE }
            val past = sorted.filter { it.dueEpochDay != null && it.dueEpochDay <= today }
            var streak = 0
            for (i in past.indices.reversed()) {
                val t = past[i]
                if (t.dueEpochDay == today && !t.isDone && streak == 0) continue
                if (t.isDone) streak++ else break
            }
            HabitStat(
                title = sorted.last().title,
                repeatLabel = runCatching {
                    RepeatKind.valueOf(sorted.first().repeat!!).label }.getOrDefault(""),
                streak = streak,
                recent = past.takeLast(14)
            )
        }.sortedByDescending { it.streak }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = Surface1, shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Text(value, style = MaterialTheme.typography.displaySmall, color = Champagne)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
        }
    }
}

@Composable
fun StatsScreen(
    tasks: List<Task>,
    onExport: () -> Unit,
    onImport: () -> Unit,
    contentPadding: PaddingValues
) {
    val today = LocalDate.now()
    val last7 = (0..6).map { today.minusDays((6 - it).toLong()) }

    val week = tasks.filter { t ->
        val due = t.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
        due != null && !due.isBefore(last7.first()) && !due.isAfter(today)
    }
    val dueWeight = week.sumOf { it.priority.weight }
    val doneWeight = week.filter { it.isDone }.sumOf { it.priority.weight }
    val efficiency = if (dueWeight == 0) 0 else (doneWeight * 100f / dueWeight).roundToInt()

    // Previous 7-day window, for the "vs. last period" delta
    val prevStart = today.minusDays(13)
    val prevEnd = today.minusDays(7)
    val prevWeek = tasks.filter { t ->
        val due = t.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
        due != null && !due.isBefore(prevStart) && !due.isAfter(prevEnd)
    }
    val prevDue = prevWeek.sumOf { it.priority.weight }
    val prevEff = if (prevDue == 0) 0 else (prevWeek.filter { it.isDone }.sumOf { it.priority.weight } * 100f / prevDue).roundToInt()
    val delta = efficiency - prevEff

    val completedAll = tasks.filter { it.isDone }
    val completionDays = completedAll.mapNotNull { it.completedAt?.toLocalDate() }.toSet()
    var streak = 0
    var cursor = today
    if (cursor !in completionDays) cursor = cursor.minusDays(1)
    while (cursor in completionDays) {
        streak++
        cursor = cursor.minusDays(1)
    }

    val perDayCounts = last7.map { d -> completedAll.count { it.completedAt?.toLocalDate() == d } }
    val maxCount = (perDayCounts.maxOrNull() ?: 0).coerceAtLeast(1)

    val focusMinutes = completedAll
        .filter { it.completedAt?.toLocalDate()?.let { d -> !d.isBefore(last7.first()) } == true }
        .sumOf { it.estimatedMinutes }

    val habits = habitStats(tasks)
    val countsByDay = completedAll.mapNotNull { it.completedAt?.toLocalDate() }
        .groupingBy { it }.eachCount()

    val context = LocalContext.current
    val visionChecks = remember { VisionCheckStore.getChecks(context) }
    val visionStreak = remember { VisionCheckStore.getStreak(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Progress",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
        }

        item {
            Surface(color = Surface1, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Weekly efficiency", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            dueWeight == 0 -> "Add dated tasks to start tracking."
                            delta > 0 -> "You are ahead of last week — keep the momentum."
                            delta < 0 -> "Slightly behind last week. A strong day resets it."
                            else -> "Holding steady with last week."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = IvoryDim
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$efficiency%", style = MaterialTheme.typography.displayLarge,
                            color = Champagne)
                        Spacer(Modifier.width(12.dp))
                        if (prevDue > 0) {
                            val up = delta >= 0
                            Surface(
                                color = if (up) Sage.copy(alpha = 0.18f) else Terracotta.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    "${if (up) "↗" else "↘"} ${kotlin.math.abs(delta)}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (up) Sage else Terracotta,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("vs. last week",
                                style = MaterialTheme.typography.bodyMedium, color = IvoryFaint,
                                modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    BarcodeProgress(fraction = efficiency / 100f, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("$streak", "task streak", Modifier.weight(1f))
                StatCard("$visionStreak", "vision streak", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    if (focusMinutes >= 60) "%.1fh".format(focusMinutes / 60f) else "${focusMinutes}m",
                    "focus this week", Modifier.weight(1f)
                )
                StatCard("${week.count { it.isDone }}", "done this week", Modifier.weight(1f))
            }
        }
        item {
            StatCard("${completedAll.size}", "completed all-time", Modifier.fillMaxWidth())
        }

        if (visionChecks.isNotEmpty()) {
            item { SectionLabel("Vision alignment · last 14 days", Modifier.padding(top = 12.dp)) }
            item {
                Surface(color = Surface1, shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        val last14 = (0..13).map { today.minusDays((13 - it).toLong()) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            last14.forEach { day ->
                                val ans = visionChecks[day.toString()]
                                val color = when (ans) {
                                    "yes" -> Champagne
                                    "partial" -> Sage
                                    "no" -> Terracotta.copy(alpha = 0.7f)
                                    else -> Hairline
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .background(color, RoundedCornerShape(4.dp))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        day.format(DateTimeFormatter.ofPattern("d")),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IvoryFaint
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(
                                "Yes" to Champagne,
                                "Partial" to Sage,
                                "No" to Terracotta.copy(alpha = 0.7f)
                            ).forEach { (label, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(color, CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall,
                                        color = IvoryFaint)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (habits.isNotEmpty()) {
            item { SectionLabel("Habit streaks", Modifier.padding(top = 12.dp)) }
            item {
                Surface(color = Surface1, shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        habits.forEach { h ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(h.title, style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f))
                                    Text("↻ ${h.repeatLabel}",
                                        style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                                    Spacer(Modifier.width(10.dp))
                                    Text("🔥 ${h.streak}",
                                        style = MaterialTheme.typography.labelLarge, color = Champagne)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    h.recent.forEach { t ->
                                        Box(Modifier.size(8.dp).background(
                                            if (t.isDone) Champagne else Hairline, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("Activity · last 12 weeks", Modifier.padding(top = 12.dp)) }
        item {
            Surface(color = Surface1, shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()) {
                val start = today.minusDays(83)
                    .minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
                Row(
                    Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var d = start
                    while (!d.isAfter(today)) {
                        val weekStart = d
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)) {
                            for (i in 0..6) {
                                val day = weekStart.plusDays(i.toLong())
                                if (day.isAfter(today)) break
                                val n = countsByDay[day] ?: 0
                                val color = when {
                                    n == 0 -> Hairline
                                    n == 1 -> Champagne.copy(alpha = 0.35f)
                                    n == 2 -> Champagne.copy(alpha = 0.65f)
                                    else -> Champagne
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .background(color, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                        d = d.plusWeeks(1)
                    }
                }
            }
        }

        item { SectionLabel("Completions · last 7 days", Modifier.padding(top = 12.dp)) }
        item {
            Surface(color = Surface1, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7.forEachIndexed { i, d ->
                        val count = perDayCounts[i]
                        val frac = count.toFloat() / maxCount
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (count > 0) {
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IvoryDim
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((94 * frac).dp.coerceAtLeast(3.dp))
                                    .background(
                                        if (count > 0) Champagne else Hairline,
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).take(2),
                                style = MaterialTheme.typography.labelMedium,
                                color = IvoryFaint
                            )
                        }
                    }
                }
            }
        }

        item { SectionLabel("Balance by category", Modifier.padding(top = 12.dp)) }
        item {
            Surface(color = Surface1, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val catColors = mapOf(
                        Category.WORK to Champagne,
                        Category.VISION to Terracotta,
                        Category.SKILL to Sage,
                        Category.PERSONAL to Slate,
                        Category.RESEARCH to Periwinkle
                    )
                    Category.entries.forEach { cat ->
                        val inCat = tasks.filter { it.category == cat }
                        val doneInCat = inCat.count { it.isDone }
                        val frac = if (inCat.isEmpty()) 0f else doneInCat.toFloat() / inCat.size
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .background(catColors[cat]!!, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cat.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$doneInCat / ${inCat.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IvoryDim
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(Hairline, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(frac)
                                        .height(5.dp)
                                        .background(catColors[cat]!!, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("Your data", Modifier.padding(top = 12.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Text("⬇ Export backup", color = IvoryDim,
                        style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Text("⬆ Import backup", color = IvoryDim,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }
}
