package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.ui.EmptyState
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.TaskRow
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    contentPadding: PaddingValues
) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    val q = query.trim().lowercase()
    val filtered = tasks.filter { t ->
        (filter == null || t.category.name == filter) &&
        (q.isEmpty() || t.title.lowercase().contains(q) || t.notes.lowercase().contains(q))
    }
    val pending = filtered.filter { !it.isDone }
        .sortedWith(compareByDescending<Task> { it.priority.weight }
            .thenBy { it.dueEpochDay ?: Long.MAX_VALUE })
    val done = filtered.filter { it.isDone }.sortedByDescending { it.completedAt ?: 0 }
    val todayEpoch = LocalDate.now().toEpochDay()

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Text(
            "All tasks",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 24.dp, bottom = 14.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search tasks…", color = IvoryDim) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Champagne,
                unfocusedBorderColor = Hairline,
                cursorColor = Champagne
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Surface1,
                    selectedContainerColor = Champagne,
                    selectedLabelColor = OnChampagne
                )
            )
            Category.entries.forEach { cat ->
                FilterChip(
                    selected = filter == cat.name,
                    onClick = { filter = if (filter == cat.name) null else cat.name },
                    label = { Text(cat.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Surface1,
                        selectedContainerColor = Champagne,
                        selectedLabelColor = OnChampagne
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            if (pending.isEmpty() && done.isEmpty()) {
                item {
                    if (q.isNotEmpty()) EmptyState("No matches", "Try a different search.")
                    else EmptyState("Nothing here yet", "Capture work, goals and skills to build your path.")
                }
            }
            if (pending.isNotEmpty()) {
                item { SectionLabel("Pending") }
                items(pending, key = { it.id }) { task ->
                    val dueLabel = task.dueEpochDay?.let {
                        if (it == todayEpoch) "Today"
                        else LocalDate.ofEpochDay(it)
                            .format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
                    }
                    TaskRow(task, onToggle = { onToggle(task) },
                        onClick = { onEdit(task) }, timeLabel = dueLabel)
                }
            }
            if (done.isNotEmpty()) {
                item { SectionLabel("Done", Modifier.padding(top = 12.dp)) }
                items(done, key = { it.id }) { task ->
                    TaskRow(task, onToggle = { onToggle(task) }, onClick = { onEdit(task) })
                }
            }
            item { Spacer(Modifier.height(1.dp)) }
        }
    }
}
