package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.TaskRow
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.color
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    contentPadding: PaddingValues
) {
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = LocalDate.now()
    val tasksByDay = remember(tasks) { tasks.filter { it.dueEpochDay != null }.groupBy { it.dueEpochDay!! } }
    val dayTasks = tasksByDay[selectedDate.toEpochDay()].orEmpty()
        .sortedWith(compareBy<Task> { it.isDone }.thenByDescending { it.priority.weight })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", tint = IvoryDim)
                }
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", tint = IvoryDim)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(d, style = MaterialTheme.typography.labelMedium, color = IvoryFaint)
                    }
                }
            }
        }

        item {
            val firstDow = month.atDay(1).dayOfWeek.value // Monday = 1
            val daysInMonth = month.lengthOfMonth()
            val cells = firstDow - 1 + daysInMonth
            val rows = (cells + 6) / 7
            Column {
                repeat(rows) { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val dayNum = row * 7 + col - (firstDow - 1) + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..daysInMonth) {
                                    val date = month.atDay(dayNum)
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val dueHere = tasksByDay[date.toEpochDay()].orEmpty()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                when {
                                                    isSelected -> Champagne
                                                    isToday -> Surface1
                                                    else -> androidx.compose.ui.graphics.Color.Transparent
                                                },
                                                CircleShape
                                            )
                                            .clickable { onSelectDate(date) },
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = when {
                                                isSelected -> OnChampagne
                                                isToday -> Champagne
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.height(6.dp)
                                        ) {
                                            dueHere.take(3).forEach { t ->
                                                Box(
                                                    Modifier
                                                        .size(4.dp)
                                                        .background(
                                                            if (isSelected) OnChampagne else t.priority.color,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionLabel(
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)),
                Modifier.padding(top = 16.dp, bottom = 2.dp)
            )
        }

        if (dayTasks.isEmpty()) {
            item {
                Text(
                    "Nothing scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryDim,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(dayTasks, key = { it.id }) { task ->
                TaskRow(task, onToggle = { onToggle(task) }, onClick = { onEdit(task) })
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }
}
