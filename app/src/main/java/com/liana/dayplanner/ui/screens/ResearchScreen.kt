package com.liana.dayplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liana.dayplanner.data.ResearchItem
import com.liana.dayplanner.data.ResearchProject
import com.liana.dayplanner.ui.EmptyState
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.Periwinkle
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Surface2
import com.liana.dayplanner.ui.theme.Terracotta

@Composable
fun ResearchScreen(
    projects: List<ResearchProject>,
    onUpdateProject: (ResearchProject) -> Unit,
    onAddProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Research",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showNewDialog = true }) {
                    Icon(Icons.Outlined.Add, "New project", tint = Champagne)
                }
            }
        }

        if (projects.isEmpty()) {
            item {
                EmptyState(
                    title = "No research projects",
                    subtitle = "Tap + to create your first project."
                )
            }
        } else {
            items(projects, key = { it.id }) { proj ->
                ProjectCard(
                    project = proj,
                    expanded = expandedId == proj.id,
                    onToggleExpand = { expandedId = if (expandedId == proj.id) null else proj.id },
                    onUpdate = onUpdateProject,
                    onDelete = { onDeleteProject(proj.id) }
                )
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }

    if (showNewDialog) {
        NewProjectDialog(
            onConfirm = { title -> onAddProject(title); showNewDialog = false },
            onDismiss = { showNewDialog = false }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ResearchProject,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (ResearchProject) -> Unit,
    onDelete: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(color = Surface1, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(10.dp).background(Periwinkle, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        project.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val taskCount = project.tasks.size
                    val doneCount = project.tasks.count { it.done }
                    val targetCount = project.targets.size
                    if (taskCount > 0 || targetCount > 0) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            buildString {
                                if (taskCount > 0) append("$doneCount/$taskCount tasks")
                                if (taskCount > 0 && targetCount > 0) append(" · ")
                                if (targetCount > 0) append("$targetCount targets")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvoryDim
                        )
                    }
                }
                IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, "Rename", tint = IvoryFaint, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = IvoryFaint, modifier = Modifier.size(16.dp))
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = IvoryFaint,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp)) {
                    Divider(color = Hairline, thickness = 0.5.dp)
                    Spacer(Modifier.height(12.dp))

                    ItemSection(
                        label = "Tasks",
                        items = project.tasks,
                        accentColor = Champagne,
                        onToggleItem = { itemId ->
                            onUpdate(project.copy(tasks = project.tasks.map {
                                if (it.id == itemId) it.copy(done = !it.done) else it
                            }))
                        },
                        onDeleteItem = { itemId ->
                            onUpdate(project.copy(tasks = project.tasks.filter { it.id != itemId }))
                        },
                        onAddItem = { text ->
                            onUpdate(project.copy(tasks = project.tasks + ResearchItem(text = text)))
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    ItemSection(
                        label = "Targets",
                        items = project.targets,
                        accentColor = Periwinkle,
                        onToggleItem = { itemId ->
                            onUpdate(project.copy(targets = project.targets.map {
                                if (it.id == itemId) it.copy(done = !it.done) else it
                            }))
                        },
                        onDeleteItem = { itemId ->
                            onUpdate(project.copy(targets = project.targets.filter { it.id != itemId }))
                        },
                        onAddItem = { text ->
                            onUpdate(project.copy(targets = project.targets + ResearchItem(text = text)))
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    NotesSection(
                        notes = project.notes,
                        onSave = { onUpdate(project.copy(notes = it)) }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        var input by remember { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Champagne,
                        cursorColor = Champagne
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (input.isNotBlank()) { onUpdate(project.copy(title = input.trim())) }
                    showRenameDialog = false
                }) { Text("Save", color = Champagne) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete project?") },
            text = { Text("\"${project.title}\" and all its items will be removed.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = Terracotta)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ItemSection(
    label: String,
    items: List<ResearchItem>,
    accentColor: Color,
    onToggleItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onAddItem: (String) -> Unit
) {
    var addInput by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(false) }

    SectionLabel(label)
    Spacer(Modifier.height(6.dp))

    if (items.isEmpty() && !showInput) {
        Text(
            "No ${label.lowercase()} yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = IvoryFaint,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (item.done) accentColor else Color.Transparent,
                        CircleShape
                    )
                    .then(
                        if (!item.done) Modifier.background(Color.Transparent, CircleShape)
                            .padding(1.dp).background(Hairline, CircleShape)
                        else Modifier
                    )
                    .clickable { onToggleItem(item.id) },
                contentAlignment = Alignment.Center
            ) {
                if (item.done) {
                    Box(Modifier.size(8.dp).background(Surface1, CircleShape))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.done) IvoryFaint else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onDeleteItem(item.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Outlined.Delete, "Remove", tint = IvoryFaint, modifier = Modifier.size(14.dp))
            }
        }
    }

    if (showInput) {
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            value = addInput,
            onValueChange = { addInput = it },
            placeholder = { Text("Add ${label.lowercase().dropLast(1)}…", color = IvoryFaint, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Hairline,
                cursorColor = accentColor
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (addInput.isNotBlank()) { onAddItem(addInput.trim()); addInput = "" }
                showInput = false
            })
        )
        androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    TextButton(
        onClick = { showInput = !showInput; addInput = "" },
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Outlined.Add, null, tint = accentColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add ${label.dropLast(1).lowercase()}", color = accentColor,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NotesSection(notes: String, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(notes) { mutableStateOf(notes) }

    SectionLabel("Notes")
    Spacer(Modifier.height(6.dp))

    if (editing) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Write your notes…", color = IvoryFaint, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IvoryDim,
                unfocusedBorderColor = Hairline,
                cursorColor = Champagne
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            minLines = 3
        )
        Spacer(Modifier.height(4.dp))
        Row {
            TextButton(onClick = { onSave(draft.trim()); editing = false },
                contentPadding = PaddingValues(0.dp)) {
                Text("Save", color = Champagne, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = { draft = notes; editing = false },
                contentPadding = PaddingValues(0.dp)) {
                Text("Cancel", color = IvoryFaint, style = MaterialTheme.typography.labelMedium)
            }
        }
    } else {
        Surface(
            color = Surface2,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().clickable { editing = true }
        ) {
            Text(
                text = notes.ifBlank { "Tap to add notes…" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (notes.isBlank()) IvoryFaint else IvoryDim,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun NewProjectDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New research project") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Project name", color = IvoryFaint) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Champagne,
                    cursorColor = Champagne
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (title.isNotBlank()) onConfirm(title.trim())
                })
            )
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onConfirm(title.trim()) }) {
                Text("Create", color = Champagne)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
