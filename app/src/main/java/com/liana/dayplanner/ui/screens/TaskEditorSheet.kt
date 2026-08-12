package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.Priority
import com.liana.dayplanner.data.RepeatKind
import com.liana.dayplanner.data.STUDY_SUBJECTS
import com.liana.dayplanner.data.SubTask
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.data.Vision
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Terracotta
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    existing: Task?,
    defaultDate: LocalDate,
    defaultCategory: Category,
    vision: Vision,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDiscardWarning by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: defaultCategory) }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var dueDate by remember {
        mutableStateOf(existing?.dueEpochDay?.let { LocalDate.ofEpochDay(it) } ?: defaultDate)
    }
    var hasDueDate by remember { mutableStateOf(existing == null || existing.dueEpochDay != null) }
    var estimate by remember { mutableStateOf(existing?.estimatedMinutes ?: 30) }
    var fixedTime by remember { mutableStateOf(existing?.fixedTime) }
    var repeat by remember { mutableStateOf(existing?.repeat) }
    var milestoneId by remember { mutableStateOf(existing?.milestoneId) }
    var subject by remember { mutableStateOf(existing?.subject) }
    var spaced by remember { mutableStateOf(existing?.spaced ?: false) }
    var inProgress by remember { mutableStateOf(existing?.inProgress ?: false) }
    var pendingReason by remember { mutableStateOf(existing?.pendingReason ?: "") }
    var outcome by remember { mutableStateOf(existing?.outcome ?: "") }
    var includeInMeeting by remember { mutableStateOf(existing?.includeInMeeting ?: false) }
    val steps = remember {
        mutableStateListOf<SubTask>().apply { addAll(existing?.subs ?: emptyList()) }
    }
    var reminderTime by remember {
        mutableStateOf(
            existing?.reminderAt?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
            }
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showFixedPicker by remember { mutableStateOf(false) }
    var milestoneMenuOpen by remember { mutableStateOf(false) }

    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = Surface1,
        selectedContainerColor = Champagne,
        selectedLabelColor = OnChampagne
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Champagne,
        unfocusedBorderColor = Hairline,
        cursorColor = Champagne
    )

    // Has the user entered anything worth warning about before discarding?
    val dirty: Boolean = if (existing == null) {
        title.isNotBlank() || notes.isNotBlank() ||
            steps.any { it.text.isNotBlank() } || fixedTime != null ||
            inProgress || pendingReason.isNotBlank() || outcome.isNotBlank() || includeInMeeting
    } else {
        title.trim() != existing.title ||
            notes.trim() != existing.notes ||
            category != existing.category ||
            priority != existing.priority ||
            estimate != existing.estimatedMinutes ||
            fixedTime != existing.fixedTime ||
            (if (hasDueDate) dueDate.toEpochDay() else null) != existing.dueEpochDay ||
            repeat != existing.repeat ||
            subject != existing.subject ||
            spaced != existing.spaced ||
            milestoneId != existing.milestoneId ||
            inProgress != existing.inProgress ||
            pendingReason.trim() != existing.pendingReason ||
            outcome.trim() != existing.outcome ||
            includeInMeeting != existing.includeInMeeting ||
            Task.encodeSubs(steps.filter { it.text.isNotBlank() }) != existing.subsJson
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Swiping down or pressing back with unsaved edits asks first.
            if (dirty) {
                showDiscardWarning = true
                scope.launch { sheetState.show() }   // keep the sheet up
            } else onDismiss()
        },
        sheetState = sheetState,
        containerColor = Ink
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (existing == null) "New task" else "Edit task",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                if (existing != null) {
                    IconButton(onClick = { onDelete(existing) }) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = Terracotta)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What needs to be done?", color = IvoryDim) },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            val isResearch = category == Category.RESEARCH
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = if (isResearch) {
                    { Text("Research notes", color = IvoryDim) }
                } else null,
                placeholder = {
                    Text(
                        if (isResearch) "Findings, links, sources, next questions…"
                        else "Notes (optional)",
                        color = IvoryDim
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
                minLines = if (isResearch) 6 else 2
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Category")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) },
                        colors = chipColors
                    )
                }
            }

            if (category == Category.VISION && vision.milestones.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Milestone")
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(onClick = { milestoneMenuOpen = true }) {
                        Text(
                            vision.milestones.firstOrNull { it.id == milestoneId }?.title
                                ?: "No milestone",
                            color = if (milestoneId != null) Champagne else IvoryDim
                        )
                    }
                    DropdownMenu(
                        expanded = milestoneMenuOpen,
                        onDismissRequest = { milestoneMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No milestone") },
                            onClick = { milestoneId = null; milestoneMenuOpen = false }
                        )
                        vision.milestones.forEach { ms ->
                            DropdownMenuItem(
                                text = { Text(ms.title) },
                                onClick = { milestoneId = ms.id; milestoneMenuOpen = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Priority")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) },
                        colors = chipColors
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Estimated time")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60, 90, 120).forEach { m ->
                    FilterChip(
                        selected = estimate == m,
                        onClick = { estimate = m },
                        label = { Text(if (m >= 60) "${m / 60}h${if (m % 60 != 0) " ${m % 60}m" else ""}" else "${m}m") },
                        colors = chipColors
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Schedule")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(
                        if (hasDueDate) dueDate.format(dateFmt) else "No date",
                        color = if (hasDueDate) Champagne else IvoryDim
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (hasDueDate) {
                    TextButton(onClick = {
                        hasDueDate = false; reminderTime = null; repeat = null; fixedTime = null
                    }) {
                        Text("Clear", color = IvoryDim)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Fixed start time")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showFixedPicker = true }) {
                    Text(
                        fixedTime ?: "None",
                        color = if (fixedTime != null) Champagne else IvoryDim
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (fixedTime != null) {
                    TextButton(onClick = { fixedTime = null }) { Text("Clear", color = IvoryDim) }
                } else {
                    Text("appointment — planner works around it",
                        style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                }
            }

            if (hasDueDate) {
                Spacer(Modifier.height(12.dp))
                SectionLabel("Repeat")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = repeat == null,
                        onClick = { repeat = null },
                        label = { Text("Never") },
                        colors = chipColors
                    )
                    RepeatKind.entries.forEach { r ->
                        FilterChip(
                            selected = repeat == r.name,
                            onClick = { repeat = r.name },
                            label = { Text(r.label) },
                            colors = chipColors
                        )
                    }
                }
            }

            if (category == Category.SKILL || category == Category.VISION) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Subject")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = subject == null,
                        onClick = { subject = null },
                        label = { Text("None") },
                        colors = chipColors
                    )
                    STUDY_SUBJECTS.forEach { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s) },
                            colors = chipColors
                        )
                    }
                }

                if (hasDueDate) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Spaced revision", style = MaterialTheme.typography.titleMedium)
                            Text("Reschedules 1 → 3 → 7 → 15 → 30 days after each review",
                                style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                        }
                        Switch(
                            checked = spaced,
                            onCheckedChange = { spaced = it; if (it) repeat = null },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnChampagne, checkedTrackColor = Champagne)
                        )
                    }
                }
            }

            if (category == Category.WORK || category == Category.RESEARCH) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Meeting details")
                Spacer(Modifier.height(8.dp))
                // Research tasks opt in per-task; Work tasks are always included.
                if (category == Category.RESEARCH) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Include in Meeting mode",
                                style = MaterialTheme.typography.titleMedium)
                            Text("Only Research tasks you switch on appear in meetings",
                                style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                        }
                        Switch(
                            checked = includeInMeeting,
                            onCheckedChange = { includeInMeeting = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnChampagne, checkedTrackColor = Champagne)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Currently working on this",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Shows under \"In progress\" in Meeting mode",
                            style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                    }
                    Switch(
                        checked = inProgress,
                        onCheckedChange = { inProgress = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnChampagne, checkedTrackColor = Champagne)
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pendingReason,
                    onValueChange = { pendingReason = it },
                    placeholder = { Text("Why is it pending / what's blocking it?", color = IvoryDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    minLines = 2
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    placeholder = { Text("Outcome (filled in when you complete it)", color = IvoryDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    minLines = 2
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Subtasks")
            Spacer(Modifier.height(8.dp))
            steps.forEachIndexed { i, sub ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Checkbox(
                        checked = sub.done,
                        onCheckedChange = { steps[i] = sub.copy(done = it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Champagne, checkmarkColor = OnChampagne)
                    )
                    OutlinedTextField(
                        value = sub.text,
                        onValueChange = { steps[i] = sub.copy(text = it) },
                        placeholder = { Text("Subtask", color = IvoryDim) },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                    IconButton(onClick = { steps.removeAt(i) }) {
                        Icon(Icons.Rounded.Close, "Remove subtask", tint = IvoryDim)
                    }
                }
            }
            OutlinedButton(onClick = { steps.add(SubTask("", false)) }) {
                Icon(Icons.Rounded.Add, null, tint = IvoryDim,
                    modifier = Modifier.padding(end = 6.dp))
                Text("Add subtask", color = IvoryDim)
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        reminderTime?.let { "At ${it.format(timeFmt)} on due date" } ?: "Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvoryDim
                    )
                }
                Switch(
                    checked = reminderTime != null,
                    onCheckedChange = { on ->
                        if (on) {
                            if (!hasDueDate) { hasDueDate = true }
                            showTimePicker = true
                        } else reminderTime = null
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnChampagne,
                        checkedTrackColor = Champagne
                    )
                )
            }
            if (reminderTime != null) {
                TextButton(onClick = { showTimePicker = true }) {
                    Text("Change time", color = Champagne)
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val tracksMeeting = category == Category.WORK || category == Category.RESEARCH
                    val due = if (hasDueDate) dueDate.toEpochDay() else null
                    val reminderMillis = if (hasDueDate && reminderTime != null) {
                        dueDate.atTime(reminderTime).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    } else null
                    onSave(
                        (existing ?: Task(title = title)).copy(
                            title = title.trim(),
                            notes = notes.trim(),
                            category = category,
                            priority = priority,
                            dueEpochDay = due,
                            reminderAt = reminderMillis,
                            estimatedMinutes = estimate,
                            fixedTime = fixedTime,
                            repeat = if (due != null) repeat else null,
                            milestoneId = if (category == Category.VISION) milestoneId else null,
                            subject = if (category == Category.SKILL || category == Category.VISION) subject else null,
                            spaced = spaced && hasDueDate,
                            revisionStage = if (spaced) (existing?.revisionStage ?: 0) else 0,
                            subsJson = Task.encodeSubs(steps.filter { it.text.isNotBlank() }),
                            inProgress = if (tracksMeeting) inProgress else false,
                            pendingReason = if (tracksMeeting) pendingReason.trim() else "",
                            outcome = if (tracksMeeting) outcome.trim() else "",
                            includeInMeeting = category == Category.RESEARCH && includeInMeeting
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (existing == null) "Add task" else "Save changes",
                    style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDiscardWarning) {
        AlertDialog(
            onDismissRequest = { showDiscardWarning = false },
            containerColor = Surface1,
            title = { Text("Unsaved task") },
            text = {
                Text(
                    if (existing == null)
                        "This task hasn't been saved yet. Discard it?"
                    else "Your changes haven't been saved. Discard them?",
                    color = IvoryDim
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardWarning = false
                    scope.launch { sheetState.hide(); onDismiss() }
                }) { Text("Discard", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardWarning = false }) {
                    Text("Keep editing", color = Champagne)
                }
            }
        )
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        hasDueDate = true
                    }
                    showDatePicker = false
                }) { Text("OK", color = Champagne) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = IvoryDim) }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val initial = reminderTime ?: LocalTime.of(9, 0)
        val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Surface1,
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK", color = Champagne) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = IvoryDim) }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showFixedPicker) {
        val initial = fixedTime?.split(":")?.let {
            LocalTime.of(it[0].toInt(), it[1].toInt()) } ?: LocalTime.of(10, 0)
        val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showFixedPicker = false },
            containerColor = Surface1,
            confirmButton = {
                TextButton(onClick = {
                    fixedTime = "%02d:%02d".format(timeState.hour, timeState.minute)
                    showFixedPicker = false
                }) { Text("OK", color = Champagne) }
            },
            dismissButton = {
                TextButton(onClick = { showFixedPicker = false }) { Text("Cancel", color = IvoryDim) }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
