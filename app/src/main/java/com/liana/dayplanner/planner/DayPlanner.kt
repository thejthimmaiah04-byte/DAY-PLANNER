package com.liana.dayplanner.planner

import com.liana.dayplanner.data.AppSettings
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.Task
import java.time.LocalDate
import java.time.LocalDateTime

data class PlannedBlock(
    val task: Task,
    val startMin: Int,          // minutes from midnight
    val endMin: Int,
    val fixed: Boolean = false,
    val part: String? = null    // "1/2" when a long task is split
)

data class DayPlan(
    val blocks: List<PlannedBlock>,
    val overflow: List<Task>,
    val completed: List<Task>,
    val triage: List<Task>      // overdue, waiting for a decision
)

/**
 * Work tasks live inside 9:00–17:00; every other category gets the growth
 * windows 8:00–9:00 and 17:00–22:00. Fixed-time appointments anchor first and
 * the rest flows around them; tasks over an hour are split into <=55-minute
 * focus parts with 10-minute breaks.
 */
object DayPlanner {
    private class Window(var cur: Int, val end: Int)

    fun plan(
        allTasks: List<Task>,
        date: LocalDate,
        now: LocalDateTime = LocalDateTime.now(),
        settings: AppSettings = AppSettings()
    ): DayPlan {
        val DAY_START = settings.dayStart
        val WORK_START = settings.workStart.coerceIn(settings.dayStart, settings.dayEnd)
        val WORK_END = settings.workEnd.coerceIn(WORK_START, settings.dayEnd)
        val DAY_END = settings.dayEnd
        val epochDay = date.toEpochDay()
        val isToday = date == now.toLocalDate()

        val dayTasks = allTasks.filter { it.dueEpochDay == epochDay }
        val completed = dayTasks.filter { it.isDone }
        val pending = dayTasks.filter { !it.isDone }
        val triage = if (isToday)
            allTasks.filter { it.dueEpochDay != null && it.dueEpochDay < epochDay && !it.isDone }
                .sortedByDescending { it.priority.weight }
        else emptyList()

        var nowCursor = 0
        if (isToday) {
            val nowMin = now.hour * 60 + now.minute
            nowCursor = ((nowMin + 14) / 15) * 15
        }

        val blocks = mutableListOf<PlannedBlock>()
        val overflow = mutableListOf<Task>()
        val busy = mutableListOf<IntRange>()

        for (t in pending.filter { it.fixedTime != null }) {
            val parts = t.fixedTime!!.split(":")
            val start = parts[0].toInt() * 60 + parts[1].toInt()
            val end = start + t.estimatedMinutes.coerceAtLeast(15)
            blocks.add(PlannedBlock(t, start, end, fixed = true))
            busy.add((start - 5)..(end + 5))
        }

        fun carve(spans: List<IntRange>): MutableList<Window> {
            var wins = spans.map { maxOf(it.first, nowCursor) to it.last }
                .filter { it.first < it.second }.toMutableList()
            for (b in busy.sortedBy { it.first }) {
                val next = mutableListOf<Pair<Int, Int>>()
                for ((s, e) in wins) {
                    if (b.last <= s || b.first >= e) { next.add(s to e); continue }
                    if (b.first > s) next.add(s to b.first)
                    if (b.last < e) next.add(b.last to e)
                }
                wins = next
            }
            return wins.map { Window(it.first, it.second) }.toMutableList()
        }

        fun fill(list: List<Task>, windows: MutableList<Window>) {
            val ranked = list.sortedWith(
                compareBy<Task> { it.manualOrder ?: Int.MAX_VALUE }
                    .thenByDescending { it.priority.weight }
                    .thenBy { it.estimatedMinutes }
            )
            for (t in ranked) {
                val total = t.estimatedMinutes.coerceAtLeast(15)
                val parts = if (total > 60) (total + 54) / 55 else 1
                val per = (total + parts - 1) / parts
                val placed = mutableListOf<Pair<Int, Int>>()
                for (p in 0 until parts) {
                    val win = windows.firstOrNull { it.cur + per <= it.end } ?: break
                    placed.add(win.cur to win.cur + per)
                    win.cur += per + if (parts > 1) 10 else 5
                }
                if (placed.size < parts) { overflow.add(t); continue }
                placed.forEachIndexed { i, (s, e) ->
                    blocks.add(PlannedBlock(t, s, e,
                        part = if (parts > 1) "${i + 1}/$parts" else null))
                }
            }
        }

        val flex = pending.filter { it.fixedTime == null }
        fill(flex.filter { it.category == Category.WORK }, carve(listOf(WORK_START..WORK_END)))
        fill(flex.filter { it.category != Category.WORK },
             carve(listOf(DAY_START..WORK_START, WORK_END..DAY_END)))

        blocks.sortBy { it.startMin }
        overflow.sortByDescending { it.priority.weight }
        return DayPlan(blocks, overflow, completed, triage)
    }
}
