package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.liana.dayplanner.data.Priority
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.planner.DayPlanner
import com.liana.dayplanner.planner.PlannedBlock
import com.liana.dayplanner.ui.BarcodeProgress
import com.liana.dayplanner.ui.EmptyState
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.TaskRow
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.ChampagneDeep
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Terracotta
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

private fun minLabel(min: Int): String =
    LocalTime.of(min / 60, min % 60).format(timeFmt)

@Composable
fun TodayScreen(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onTriage: (Task, String) -> Unit,
    onStartBrief: () -> Unit,
    onVoiceSettings: () -> Unit,
    onFocus: (Task) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCurrentAffairs: () -> Unit,
    onOpenMeeting: () -> Unit,
    onReorderPlan: (fromIndex: Int, toIndex: Int, blocks: List<PlannedBlock>) -> Unit,
    settings: com.liana.dayplanner.data.AppSettings,
    contentPadding: PaddingValues
) {
    val today = LocalDate.now()
    val plan = DayPlanner.plan(tasks, today, settings = settings)
    val plannedCount = plan.blocks.distinctBy { it.task.id }.size
    val total = plannedCount + plan.overflow.size + plan.completed.size
    val progress = if (total == 0) 0f else plan.completed.size.toFloat() / total

    val urgentPendingCount = plan.blocks.count { !it.task.isDone && it.task.priority == Priority.URGENT } +
        plan.overflow.count { it.priority == Priority.URGENT }

    val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
    val workEndMin = settings.workEnd
    val showOvercommitWarning = urgentPendingCount > 0 &&
        nowMin >= (workEndMin - 60) && nowMin < workEndMin

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = today.format(
                            DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Champagne,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Settings, "Settings",
                            tint = IvoryDim, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                HeroClock()
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "${plan.completed.size} of $total done",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryDim
                )
                Spacer(Modifier.height(8.dp))
                BarcodeProgress(fraction = progress, modifier = Modifier.fillMaxWidth(), height = 34.dp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onStartBrief,
                        border = BorderStroke(1.dp, ChampagneDeep)
                    ) {
                        Icon(Icons.Rounded.Mic, null, tint = Champagne,
                            modifier = Modifier.padding(end = 8.dp))
                        Text("Morning brief", color = Champagne,
                            style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onVoiceSettings) {
                        Icon(Icons.Rounded.Tune, null, tint = IvoryDim,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp))
                        Text("Voice", color = IvoryDim,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenCurrentAffairs,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Rounded.Newspaper, null, tint = IvoryDim,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp))
                        Text("Current affairs", color = IvoryDim,
                            style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = onOpenMeeting,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Rounded.Groups, null, tint = Champagne,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp))
                        Text("Meeting mode", color = Champagne,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DayClock(blocks = plan.blocks, urgentPendingCount = urgentPendingCount)
            }
        }

        if (showOvercommitWarning) {
            item {
                Surface(
                    color = Terracotta.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$urgentPendingCount urgent ${if (urgentPendingCount == 1) "task" else "tasks"} still open — your work window closes at ${minLabel(workEndMin)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Terracotta,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (plan.triage.isNotEmpty()) {
            item {
                Surface(
                    color = Surface1,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ChampagneDeep),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionLabel("Slipped from earlier — decide their fate")
                        Spacer(Modifier.height(4.dp))
                        plan.triage.forEach { t ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(onClick = { onTriage(t, "today") }) {
                                    Text("Today", color = Champagne,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { onTriage(t, "tomorrow") }) {
                                    Text("Tmrw", color = IvoryDim,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { onTriage(t, "drop") }) {
                                    Text("Drop", color = IvoryDim,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (total == 0 && plan.triage.isEmpty()) {
            item {
                EmptyState(
                    title = "A clear day",
                    subtitle = "Add tasks and ARC will plan your day."
                )
            }
        }

        if (plan.blocks.isNotEmpty()) {
            item { SectionLabel("Your plan", Modifier.padding(top = 12.dp, bottom = 2.dp)) }
            item {
                DraggablePlanList(
                    blocks = plan.blocks,
                    onReorder = { from, to -> onReorderPlan(from, to, plan.blocks) },
                    onToggle = onToggle,
                    onEdit = onEdit,
                    onFocus = onFocus
                )
            }
        }

        if (plan.overflow.isNotEmpty()) {
            item { SectionLabel("Didn't fit today", Modifier.padding(top = 12.dp, bottom = 2.dp)) }
            items(plan.overflow, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggle(task) },
                    onClick = { onEdit(task) }
                )
            }
        }

        if (plan.completed.isNotEmpty()) {
            item { SectionLabel("Completed", Modifier.padding(top = 12.dp, bottom = 2.dp)) }
            items(plan.completed, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggle(task) },
                    onClick = { onEdit(task) }
                )
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun HeroClock() {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = LocalTime.now()
        }
    }
    val hm = String.format(Locale.ENGLISH, "%02d:%02d", now.hour, now.minute)
    val sec = String.format(Locale.ENGLISH, "%02d", now.second)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(
                fontFamily = com.liana.dayplanner.ui.theme.Fraunces,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 72.sp,
                letterSpacing = (-2).sp,
                color = androidx.compose.ui.graphics.Color.White
            )) { append(hm) }
            withStyle(SpanStyle(
                fontFamily = com.liana.dayplanner.ui.theme.Inter,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 22.sp,
                baselineShift = BaselineShift(0.4f),
                color = IvoryFaint
            )) { append(sec) }
        }
    )
}

@Composable
private fun DraggablePlanList(
    blocks: List<PlannedBlock>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onFocus: (Task) -> Unit
) {
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(88f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEachIndexed { index, block ->
            val isDragging = dragIndex == index
            val yOffset = if (isDragging) dragOffsetY.roundToInt() else 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { IntOffset(0, yOffset) }
                    .onGloballyPositioned { coords ->
                        if (index == 0 && coords.size.height > 0)
                            itemHeightPx = coords.size.height.toFloat() + 10f // include gap
                    }
            ) {
                TaskRow(
                    task = block.task,
                    onToggle = { onToggle(block.task) },
                    onClick = { onEdit(block.task) },
                    timeLabel = minLabel(block.startMin),
                    partLabel = block.part,
                    onFocus = { onFocus(block.task) },
                    dragHandleModifier = if (!block.task.isDone) {
                        Modifier.pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragIndex = index; dragOffsetY = 0f },
                                onDrag = { _, delta -> dragOffsetY += delta.y },
                                onDragEnd = {
                                    val steps = (dragOffsetY / itemHeightPx).roundToInt()
                                    val target = (dragIndex + steps).coerceIn(0, blocks.lastIndex)
                                    onReorder(dragIndex, target)
                                    dragIndex = -1; dragOffsetY = 0f
                                },
                                onDragCancel = { dragIndex = -1; dragOffsetY = 0f }
                            )
                        }
                    } else null
                )
            }
        }
    }
}
