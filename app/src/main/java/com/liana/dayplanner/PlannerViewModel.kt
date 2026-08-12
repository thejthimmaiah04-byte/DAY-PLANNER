package com.liana.dayplanner

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liana.dayplanner.data.AppDatabase
import com.liana.dayplanner.data.AppSettings
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.Milestone
import com.liana.dayplanner.data.Priority
import com.liana.dayplanner.data.ResearchItem
import com.liana.dayplanner.data.ResearchProject
import com.liana.dayplanner.data.ResearchProjectStore
import com.liana.dayplanner.data.SettingsStore
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.data.Vision
import com.liana.dayplanner.data.VisionCheckStore
import com.liana.dayplanner.data.VisionStore
import com.liana.dayplanner.data.WeeklyReflection
import com.liana.dayplanner.data.WeeklyReflectionStore
import com.liana.dayplanner.data.nextDueEpochDay
import com.liana.dayplanner.data.spacedNextGap
import com.liana.dayplanner.planner.PlannedBlock
import java.io.File
import java.time.LocalDate
import com.liana.dayplanner.reminder.ReminderScheduler
import com.liana.dayplanner.widget.ArcWidget
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class UndoOp(val label: String, val undo: suspend () -> Unit)

/** A task that was just completed — carries what's needed to note its outcome or undo. */
class CompletedTask(val id: Long, val previous: Task, val spawnedId: Long?)

class PlannerViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).taskDao()

    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var vision by mutableStateOf(VisionStore.load(app))
        private set

    var settings by mutableStateOf(SettingsStore.load(app))
        private set

    var researchProjects by mutableStateOf(ResearchProjectStore.load(app))
        private set

    val undoEvents = MutableSharedFlow<UndoOp>(extraBufferCapacity = 4)

    /** Fires when a task is completed, so the UI can offer to note its outcome. */
    val completedForNote = MutableSharedFlow<CompletedTask>(extraBufferCapacity = 4)

    private fun refreshWidget() = ArcWidget.requestUpdate(getApplication())

    fun saveVision(v: Vision) {
        vision = v
        VisionStore.save(getApplication(), v)
        if (v.statement.isNotBlank()) VisionCheckStore.saveLastReview(getApplication())
    }

    fun saveSettings(s: AppSettings) {
        settings = s
        SettingsStore.save(getApplication(), s)
        refreshWidget()
    }

    /* ---- Research projects ---- */

    private fun persistResearch(projects: List<ResearchProject>) {
        researchProjects = projects
        ResearchProjectStore.save(getApplication(), projects)
    }

    fun addResearchProject(title: String) {
        persistResearch(researchProjects + ResearchProject(title = title))
    }

    fun updateResearchProject(project: ResearchProject) {
        persistResearch(researchProjects.map { if (it.id == project.id) project else it })
    }

    fun deleteResearchProject(id: String) {
        persistResearch(researchProjects.filter { it.id != id })
    }

    /** Toggle a task item inside a research project (used in Meeting + Research screens). */
    fun toggleResearchTask(projId: String, itemId: String) {
        val updated = researchProjects.map { p ->
            if (p.id != projId) p else p.copy(tasks = p.tasks.map { t ->
                if (t.id == itemId) t.copy(done = !t.done) else t
            })
        }
        persistResearch(updated)
    }

    /** Add a task item to a research project (used in Meeting screen). */
    fun addResearchTask(projId: String, text: String) {
        val updated = researchProjects.map { p ->
            if (p.id != projId) p else p.copy(tasks = p.tasks + ResearchItem(text = text))
        }
        persistResearch(updated)
    }

    /** Add a Work task due today from the Meeting screen. */
    fun addMeetingWorkTask(title: String) {
        viewModelScope.launch {
            dao.upsert(Task(
                title = title,
                category = Category.WORK,
                priority = Priority.MEDIUM,
                dueEpochDay = LocalDate.now().toEpochDay(),
                estimatedMinutes = 30
            ))
            refreshWidget()
        }
    }

    fun save(task: Task) {
        viewModelScope.launch {
            val withSeries =
                if (task.repeat != null && task.seriesId == null && task.id != 0L)
                    task.copy(seriesId = task.id) else task
            val id = dao.upsert(withSeries)
            var saved = if (withSeries.id == 0L) withSeries.copy(id = id) else withSeries
            if (saved.repeat != null && saved.seriesId == null) {
                saved = saved.copy(seriesId = saved.id)
                dao.update(saved)
            }
            val context = getApplication<Application>()
            if (saved.reminderAt != null && !saved.isDone) {
                ReminderScheduler.schedule(context, saved)
            } else {
                ReminderScheduler.cancel(context, saved.id)
            }
            refreshWidget()
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isDone = !task.isDone,
                completedAt = if (!task.isDone) System.currentTimeMillis() else null
            )
            dao.update(updated)
            val context = getApplication<Application>()
            var spawnedId: Long? = null
            if (updated.isDone) {
                ReminderScheduler.cancel(context, updated.id)
                // Spaced-revision task -> reschedule on the 1-3-7-15-30 curve
                if (updated.spaced && updated.dueEpochDay != null && !updated.nextSpawned) {
                    val gap = spacedNextGap(updated.revisionStage)
                    val nextDue = LocalDate.ofEpochDay(updated.dueEpochDay).plusDays(gap.toLong()).toEpochDay()
                    spawnedId = dao.upsert(updated.copy(
                        id = 0, dueEpochDay = nextDue, isDone = false,
                        completedAt = null, nextSpawned = false,
                        revisionStage = updated.revisionStage + 1,
                        seriesId = updated.seriesId ?: updated.id,
                        createdAt = System.currentTimeMillis()
                    ))
                    dao.update(updated.copy(nextSpawned = true))
                }
                // Repeating task completed -> queue the next occurrence once
                else if (updated.repeat != null && updated.dueEpochDay != null && !updated.nextSpawned) {
                    val nextDue = nextDueEpochDay(updated.dueEpochDay, updated.repeat)
                    if (nextDue != null) {
                        spawnedId = dao.upsert(updated.copy(
                            id = 0, dueEpochDay = nextDue, isDone = false,
                            completedAt = null, nextSpawned = false,
                            seriesId = updated.seriesId ?: updated.id,
                            createdAt = System.currentTimeMillis()
                        ))
                        dao.update(updated.copy(nextSpawned = true))
                    }
                }
            } else if (updated.reminderAt != null) {
                ReminderScheduler.schedule(context, updated)
            }
            refreshWidget()
            val sp = spawnedId
            if (updated.isDone) {
                // Completing clears the in-progress flag and offers an outcome note.
                if (updated.inProgress) dao.update(updated.copy(inProgress = false))
                completedForNote.emit(CompletedTask(updated.id, task, sp))
            } else {
                undoEvents.emit(UndoOp("Marked \"${task.title}\" not done") {
                    dao.update(task)
                    refreshWidget()
                })
            }
        }
    }

    /** Save the outcome note captured after completing a task. */
    fun saveOutcome(taskId: Long, note: String) {
        viewModelScope.launch {
            val t = dao.getById(taskId) ?: return@launch
            dao.update(t.copy(outcome = note.trim()))
            refreshWidget()
        }
    }

    /** Undo a completion (from the outcome prompt): restore the task, drop any spawned repeat. */
    fun undoComplete(c: CompletedTask) {
        viewModelScope.launch {
            dao.update(c.previous)
            c.spawnedId?.let { dao.deleteById(it) }
            refreshWidget()
        }
    }

    /** Mark a task as actively being worked on (or not) for Meeting mode. */
    fun setInProgress(task: Task, value: Boolean) {
        viewModelScope.launch {
            dao.update(task.copy(inProgress = value))
            refreshWidget()
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
            ReminderScheduler.cancel(getApplication(), task.id)
            refreshWidget()
            undoEvents.emit(UndoOp("Deleted \"${task.title}\"") {
                dao.upsert(task)
                refreshWidget()
            })
        }
    }

    /** Turn a captured headline into a spaced-revision Current Affairs task. */
    fun addRevisionTask(headline: String) {
        viewModelScope.launch {
            dao.upsert(Task(
                title = headline,
                category = Category.SKILL,
                priority = Priority.LOW,
                dueEpochDay = LocalDate.now().toEpochDay(),
                estimatedMinutes = 15,
                subject = "Current Affairs",
                spaced = true,
                revisionStage = 0
            ))
            refreshWidget()
        }
    }

    /** Triage actions for overdue tasks: move to today / tomorrow, or drop */
    fun triage(task: Task, action: String) {
        viewModelScope.launch {
            when (action) {
                "today", "tomorrow" -> {
                    val newDue = LocalDate.now()
                        .plusDays(if (action == "tomorrow") 1 else 0).toEpochDay()
                    dao.update(task.copy(dueEpochDay = newDue))
                    refreshWidget()
                    undoEvents.emit(UndoOp("Moved to $action") {
                        dao.update(task); refreshWidget()
                    })
                }
                "drop" -> delete(task)
            }
        }
    }

    /* ---------------- Plan reordering ---------------- */

    fun reorderPlan(blocks: List<PlannedBlock>, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val reordered = blocks.toMutableList()
        val item = reordered.removeAt(fromIndex)
        reordered.add(toIndex.coerceIn(0, reordered.size), item)
        viewModelScope.launch {
            val seen = LinkedHashSet<Long>()
            var order = 0
            reordered.forEach { b ->
                if (seen.add(b.task.id)) dao.update(b.task.copy(manualOrder = order++))
            }
            refreshWidget()
        }
    }

    /* ---------------- Vision & reflection ---------------- */

    fun saveVisionCheck(answer: String) {
        VisionCheckStore.saveCheck(getApplication(), LocalDate.now(), answer)
    }

    fun saveReflection(r: WeeklyReflection) {
        WeeklyReflectionStore.save(getApplication(), r)
    }

    /* ---------------- Backup ---------------- */

    private suspend fun buildBackupJson(): JSONObject {
        val root = JSONObject()
        root.put("app", "arc")
        root.put("exported", System.currentTimeMillis())
        val arr = JSONArray()
        dao.getAll().forEach { t ->
            arr.put(JSONObject().apply {
                put("title", t.title); put("notes", t.notes)
                put("category", t.category.name); put("priority", t.priority.name)
                put("dueEpochDay", t.dueEpochDay ?: JSONObject.NULL)
                put("reminderAt", t.reminderAt ?: JSONObject.NULL)
                put("estimatedMinutes", t.estimatedMinutes)
                put("fixedTime", t.fixedTime ?: JSONObject.NULL)
                put("repeat", t.repeat ?: JSONObject.NULL)
                put("seriesId", t.seriesId ?: JSONObject.NULL)
                put("nextSpawned", t.nextSpawned)
                put("milestoneId", t.milestoneId ?: JSONObject.NULL)
                put("subject", t.subject ?: JSONObject.NULL)
                put("spaced", t.spaced)
                put("revisionStage", t.revisionStage)
                put("subsJson", t.subsJson ?: JSONObject.NULL)
                put("inProgress", t.inProgress)
                put("outcome", t.outcome)
                put("pendingReason", t.pendingReason)
                put("includeInMeeting", t.includeInMeeting)
                put("isDone", t.isDone)
                put("completedAt", t.completedAt ?: JSONObject.NULL)
                put("createdAt", t.createdAt)
            })
        }
        root.put("tasks", arr)
        val v = vision
        root.put("vision", JSONObject().apply {
            put("statement", v.statement); put("target", v.target)
            val ms = JSONArray()
            v.milestones.forEach { m ->
                ms.put(JSONObject().put("id", m.id).put("title", m.title)
                    .put("target", m.target).put("done", m.done))
            }
            put("milestones", ms)
        })
        return root
    }

    fun exportBackup(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(buildBackupJson().toString(2).toByteArray())
                }
                onDone(true)
            } catch (_: Exception) { onDone(false) }
        }
    }

    /** Silent local snapshot once per day; keeps the most recent 14 files. */
    fun autoBackupIfNeeded() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val today = LocalDate.now().toString()
                val dir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
                val file = File(dir, "arc-auto-$today.json")
                if (file.exists()) return@launch
                file.writeText(buildBackupJson().toString())
                dir.listFiles { f -> f.name.startsWith("arc-auto-") }
                    ?.sortedByDescending { it.name }
                    ?.drop(14)
                    ?.forEach { it.delete() }
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    fun importBackup(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val text = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: throw Exception()
                val root = JSONObject(text)
                if (root.optString("app") != "arc") throw Exception()
                val arr = root.getJSONArray("tasks")
                val imported = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Task(
                        title = o.getString("title"),
                        notes = o.optString("notes"),
                        category = runCatching { Category.valueOf(o.getString("category")) }
                            .getOrDefault(Category.WORK),
                        priority = runCatching { Priority.valueOf(o.getString("priority")) }
                            .getOrDefault(Priority.MEDIUM),
                        dueEpochDay = if (o.isNull("dueEpochDay")) null else o.getLong("dueEpochDay"),
                        reminderAt = if (o.isNull("reminderAt")) null else o.getLong("reminderAt"),
                        estimatedMinutes = o.optInt("estimatedMinutes", 30),
                        fixedTime = if (o.isNull("fixedTime")) null else o.getString("fixedTime"),
                        repeat = if (o.isNull("repeat")) null else o.getString("repeat"),
                        seriesId = if (o.isNull("seriesId")) null else o.getLong("seriesId"),
                        nextSpawned = o.optBoolean("nextSpawned"),
                        milestoneId = if (o.isNull("milestoneId")) null else o.getString("milestoneId"),
                        subject = if (o.isNull("subject")) null else o.optString("subject"),
                        spaced = o.optBoolean("spaced"),
                        revisionStage = o.optInt("revisionStage", 0),
                        subsJson = if (o.isNull("subsJson")) null else o.getString("subsJson"),
                        inProgress = o.optBoolean("inProgress"),
                        outcome = o.optString("outcome"),
                        pendingReason = o.optString("pendingReason"),
                        includeInMeeting = o.optBoolean("includeInMeeting"),
                        isDone = o.optBoolean("isDone"),
                        completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                }
                dao.deleteAll()
                dao.insertAll(imported)
                root.optJSONObject("vision")?.let { vo ->
                    val ms = vo.optJSONArray("milestones") ?: JSONArray()
                    saveVision(Vision(
                        statement = vo.optString("statement"),
                        target = vo.optString("target"),
                        milestones = (0 until ms.length()).map {
                            val m = ms.getJSONObject(it)
                            Milestone(m.getString("id"), m.getString("title"),
                                m.optString("target"), m.optBoolean("done"))
                        }
                    ))
                }
                refreshWidget()
                onDone(true)
            } catch (_: Exception) { onDone(false) }
        }
    }
}
