package com.liana.dayplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class Priority(val label: String, val weight: Int) {
    URGENT("Urgent", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1)
}

enum class Category(val label: String) {
    WORK("Work"),
    VISION("2-Year Vision"),
    SKILL("Skill Building"),
    PERSONAL("Personal"),
    RESEARCH("Research")   // note-heavy; the editor foregrounds its notes field
}

enum class RepeatKind(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly")
}

data class SubTask(val text: String, val done: Boolean)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val category: Category = Category.WORK,
    val priority: Priority = Priority.MEDIUM,
    /** Due date as epoch day (LocalDate.toEpochDay), null = backlog / someday */
    val dueEpochDay: Long? = null,
    /** Reminder instant in epoch millis, null = no reminder */
    val reminderAt: Long? = null,
    val estimatedMinutes: Int = 30,
    /** "HH:mm" — fixed appointment time; planner schedules around it */
    val fixedTime: String? = null,
    /** RepeatKind name, null = one-off */
    val repeat: String? = null,
    /** Groups occurrences of a repeating task for streaks */
    val seriesId: Long? = null,
    /** Set once the next occurrence has been created */
    val nextSpawned: Boolean = false,
    /** Milestone id from VisionStore, links task to the 2-year vision */
    val milestoneId: String? = null,
    /** Study subject (GS1–4, Optional, Essay, CSAT…), null = none */
    val subject: String? = null,
    /** Spaced-revision task: reschedules on a 1-3-7-15-30 day curve on completion */
    val spaced: Boolean = false,
    /** Which step of the spaced curve this occurrence is */
    val revisionStage: Int = 0,
    /** JSON [{"t":"subtask","d":false},…] */
    val subsJson: String? = null,
    /** User's manual priority override within its flexible planning window
     *  (lower = earlier). Set by dragging to reorder on the Today screen. */
    val manualOrder: Int? = null,
    /** Actively being worked on — surfaces under "In progress" in Meeting mode. */
    val inProgress: Boolean = false,
    /** What came of the task, captured when it is completed (the "outcome"). */
    val outcome: String = "",
    /** Why the task is still pending or what is blocking it. */
    val pendingReason: String = "",
    /** Research tasks only: opt this one into Meeting mode (Work is always in). */
    val includeInMeeting: Boolean = false,
    /** Free-form feedback captured from the Meeting screen for this task. */
    val meetingNote: String = "",
    val isDone: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subs: List<SubTask>
        get() = try {
            val arr = JSONArray(subsJson ?: return emptyList())
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                SubTask(o.getString("t"), o.optBoolean("d"))
            }
        } catch (_: Exception) { emptyList() }

    companion object {
        fun encodeSubs(subs: List<SubTask>): String? {
            if (subs.isEmpty()) return null
            val arr = JSONArray()
            subs.forEach { arr.put(JSONObject().put("t", it.text).put("d", it.done)) }
            return arr.toString()
        }
    }
}

fun nextDueEpochDay(due: Long, repeat: String?): Long? {
    val d = LocalDate.ofEpochDay(due)
    return when (repeat) {
        "DAILY" -> d.plusDays(1)
        "WEEKLY" -> d.plusWeeks(1)
        "MONTHLY" -> d.plusMonths(1)
        "YEARLY" -> d.plusYears(1)
        else -> null
    }?.toEpochDay()
}

/** Study subjects tuned for UPSC preparation. */
val STUDY_SUBJECTS = listOf(
    "GS1", "GS2", "GS3", "GS4", "Essay", "Optional", "CSAT", "Current Affairs"
)

/** Spaced-repetition interval (days) added on each completion. */
fun spacedNextGap(stage: Int): Int = when (stage) {
    0 -> 1; 1 -> 3; 2 -> 7; 3 -> 15; else -> 30
}
