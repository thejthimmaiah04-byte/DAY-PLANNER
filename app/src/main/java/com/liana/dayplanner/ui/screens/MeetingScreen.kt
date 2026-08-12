package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.ResearchItem
import com.liana.dayplanner.data.ResearchProject
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.Periwinkle
import com.liana.dayplanner.ui.theme.Sage
import com.liana.dayplanner.ui.theme.Slate
import com.liana.dayplanner.ui.theme.Surface1
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MeetingScreen(
    tasks: List<Task>,
    researchProjects: List<ResearchProject>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onAddWorkTask: (String) -> Unit,
    onToggleResearchTask: (projId: String, itemId: String) -> Unit,
    onAddResearchTask: (projId: String, text: String) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 8.dp, end = 16.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close", tint = IvoryDim)
                }
                Text(
                    "Meeting",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d", Locale.ENGLISH)),
                    style = MaterialTheme.typography.labelMedium,
                    color = IvoryDim
                )
            }

            // Work / Research tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Ink,
                contentColor = Champagne,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Champagne
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(
                        "Work",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedTab == 0) Champagne else IvoryFaint,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(
                        "Research",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedTab == 1) Champagne else IvoryFaint,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                WorkTab(tasks = tasks, onToggle = onToggle, onEdit = onEdit, onAddTask = onAddWorkTask)
            } else {
                ResearchTab(
                    projects = researchProjects,
                    onToggleTask = onToggleResearchTask,
                    onAddTask = onAddResearchTask
                )
            }
        }
    }
}

@Composable
private fun WorkTab(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onAddTask: (String) -> Unit
) {
    val cutoffMillis = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val relevant = tasks.filter {
        it.category == Category.WORK ||
                (it.category == Category.RESEARCH && it.includeInMeeting)
    }
    val completed = relevant.filter { it.isDone }
        .filter { it.completedAt == null || it.completedAt >= cutoffMillis }
        .sortedByDescending { it.completedAt ?: 0L }
    val inProgress = relevant.filter { !it.isDone && it.inProgress }
        .sortedByDescending { it.priority.weight }
    val pending = relevant.filter { !it.isDone && !it.inProgress }
        .sortedByDescending { it.priority.weight }

    var addInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tally row
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Tally("${completed.size}", "done", Sage, Modifier.weight(1f))
                    Tally("${inProgress.size}", "in progress", Champagne, Modifier.weight(1f))
                    Tally("${pending.size}", "pending", Slate, Modifier.weight(1f))
                }
            }

            if (relevant.isEmpty()) {
                item {
                    Text(
                        "Work tasks appear automatically. Research tasks appear when you enable \"Include in Meeting mode\" in the task editor.",
                        style = MaterialTheme.typography.bodyMedium, color = IvoryDim,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            workSection("In progress", Champagne, inProgress, onToggle, onEdit, null)
            workSection("Pending", Slate, pending, onToggle, onEdit,
                "No pending tasks.")
            workSection("Completed", Sage, completed, onToggle, onEdit,
                "Nothing done in the last 7 days.", struck = true)

            item { Spacer(Modifier.height(8.dp)) }
        }

        // Add task input at bottom
        Divider(color = Hairline, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = addInput,
                onValueChange = { addInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a Work task…", color = IvoryFaint) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Champagne,
                    unfocusedBorderColor = Hairline,
                    cursorColor = Champagne
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (addInput.isNotBlank()) { onAddTask(addInput.trim()); addInput = "" }
                })
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Champagne, CircleShape)
                    .clickable {
                        if (addInput.isNotBlank()) { onAddTask(addInput.trim()); addInput = "" }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, "Add", tint = Ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workSection(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    emptyText: String?,
    struck: Boolean = false
) {
    if (tasks.isEmpty() && emptyText == null) return

    item {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
            Box(Modifier.size(7.dp).background(accent, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = IvoryFaint)
            Spacer(Modifier.width(8.dp))
            if (tasks.isNotEmpty()) Text("${tasks.size}", style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }

    if (tasks.isEmpty() && emptyText != null) {
        item {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium,
                color = IvoryFaint, modifier = Modifier.padding(vertical = 4.dp))
        }
    } else {
        items(tasks, key = { it.id }) { t ->
            MeetingTaskRow(task = t, struck = struck, onToggle = { onToggle(t) }, onEdit = { onEdit(t) })
        }
    }
}

@Composable
private fun MeetingTaskRow(task: Task, struck: Boolean, onToggle: () -> Unit, onEdit: () -> Unit) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (task.isDone) Sage else androidx.compose.ui.graphics.Color.Transparent,
                        CircleShape
                    )
                    .then(
                        if (!task.isDone)
                            Modifier.background(androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                        else Modifier
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (!task.isDone) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                            .then(Modifier.background(androidx.compose.ui.graphics.Color.Transparent, CircleShape))
                    )
                    // Draw border ring
                    Surface(
                        modifier = Modifier.size(22.dp),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Hairline)
                    ) {}
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (struck) IvoryDim else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (struck) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.notes.isNotBlank() || task.pendingReason.isNotBlank() || task.outcome.isNotBlank()) {
                    val note = when {
                        task.isDone && task.outcome.isNotBlank() -> task.outcome
                        task.pendingReason.isNotBlank() -> task.pendingReason
                        task.notes.isNotBlank() -> task.notes
                        else -> ""
                    }
                    if (note.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(note, style = MaterialTheme.typography.bodyMedium, color = IvoryFaint,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            fontStyle = FontStyle.Italic)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Edit, "Edit", tint = IvoryFaint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ResearchTab(
    projects: List<ResearchProject>,
    onToggleTask: (String, String) -> Unit,
    onAddTask: (String, String) -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    if (projects.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 40.dp)) {
            Text(
                "No research projects yet. Go to the Research tab to create one.",
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryDim
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(projects, key = { it.id }) { proj ->
            MeetingResearchProject(
                project = proj,
                expanded = expandedId == proj.id,
                onToggleExpand = { expandedId = if (expandedId == proj.id) null else proj.id },
                onToggleTask = { itemId -> onToggleTask(proj.id, itemId) },
                onAddTask = { text -> onAddTask(proj.id, text) }
            )
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun MeetingResearchProject(
    project: ResearchProject,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String) -> Unit
) {
    var addInput by remember { mutableStateOf("") }
    var showAddInput by remember { mutableStateOf(false) }

    Surface(color = Surface1, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(Periwinkle, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    project.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val doneCount = project.tasks.count { it.done }
                if (project.tasks.isNotEmpty()) {
                    Text(
                        "$doneCount/${project.tasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IvoryFaint
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, tint = IvoryFaint, modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Divider(color = Hairline, thickness = 0.5.dp)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (project.tasks.isEmpty() && !showAddInput) {
                        Text("No tasks yet.", style = MaterialTheme.typography.bodyMedium, color = IvoryFaint)
                        Spacer(Modifier.height(4.dp))
                    }
                    project.tasks.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(20.dp).clickable { onToggleTask(item.id) },
                                color = if (item.done) Champagne else androidx.compose.ui.graphics.Color.Transparent,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp, if (item.done) Champagne else Hairline
                                )
                            ) {}
                            Spacer(Modifier.width(10.dp))
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (item.done) IvoryFaint else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (showAddInput) {
                        OutlinedTextField(
                            value = addInput,
                            onValueChange = { addInput = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            placeholder = { Text("Add task…", color = IvoryFaint) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Champagne,
                                unfocusedBorderColor = Hairline,
                                cursorColor = Champagne
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (addInput.isNotBlank()) { onAddTask(addInput.trim()); addInput = "" }
                                showAddInput = false
                            })
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { showAddInput = !showAddInput; addInput = "" },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null, tint = Champagne, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add task", color = Champagne, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tally(value: String, label: String, accent: androidx.compose.ui.graphics.Color,
                 modifier: Modifier = Modifier) {
    Surface(color = Surface1, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = IvoryDim)
        }
    }
}
