package com.liana.dayplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.RepeatKind
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.ChampagneDeep
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.color

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = IvoryFaint,
        modifier = modifier
    )
}

@Composable
fun CheckDot(checked: Boolean, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clickable(onClick = onClick)
            .then(
                if (checked) Modifier.background(Champagne, CircleShape)
                else Modifier.border(1.5.dp, tint, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Done",
                tint = OnChampagne,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    timeLabel: String? = null,
    partLabel: String? = null,
    onFocus: (() -> Unit)? = null,
    dragHandleModifier: Modifier? = null
) {
    var drag = 0f
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .pointerInput(task.id) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag > 140f) onToggle() },
                    onHorizontalDrag = { _, amount -> drag += amount }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckDot(checked = task.isDone, tint = task.priority.color, onClick = onToggle)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isDone) IvoryFaint else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(task.priority.color, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    val repeatLabel = task.repeat?.let {
                        runCatching { RepeatKind.valueOf(it).label }.getOrNull()
                    }
                    val subs = task.subs
                    Text(
                        text = buildString {
                            append("${task.priority.label} · ${task.category.label} · ${task.estimatedMinutes}m")
                            if (repeatLabel != null) append(" · ↻ $repeatLabel")
                            if (subs.isNotEmpty()) append(" · ${subs.count { it.done }}/${subs.size} subtasks")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvoryDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.category == Category.RESEARCH && task.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvoryDim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (timeLabel != null) {
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Champagne
                    )
                    if (partLabel != null) {
                        Text(
                            text = "part $partLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = IvoryFaint
                        )
                    }
                }
            }
            if (onFocus != null && !task.isDone) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .border(1.dp, ChampagneDeep, CircleShape)
                        .clickable(onClick = onFocus),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Start focus timer",
                        tint = Champagne,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (dragHandleModifier != null && !task.isDone) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = IvoryFaint,
                    modifier = dragHandleModifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Barcode-style progress bar: a row of thin vertical bars, the leading
 * [fraction] lit and the rest dim. Reads as a crisp, premium progress meter.
 */
@Composable
fun BarcodeProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    bars: Int = 34,
    height: Dp = 52.dp,
    filled: Color = Champagne,
    empty: Color = Hairline
) {
    val litCount = (fraction.coerceIn(0f, 1f) * bars).roundToInt()
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 0 until bars) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (i < litCount) filled else empty,
                        RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(56.dp)
                .border(1.dp, Hairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(Champagne, CircleShape)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = IvoryDim,
            textAlign = TextAlign.Center
        )
    }
}
