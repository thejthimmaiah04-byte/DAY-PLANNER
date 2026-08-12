package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Sage
import kotlinx.coroutines.delay

/**
 * A simple in-app focus timer. Counts down a focus block (default 25 min or the
 * task estimate capped at 55), then a 5-minute break. Runs while the app is open.
 */
@Composable
fun FocusOverlay(task: Task?, onComplete: (Task) -> Unit, onDismiss: () -> Unit) {
    if (task == null) return

    val focusSeconds = remember(task.id) {
        (task.estimatedMinutes.coerceIn(15, 55)) * 60
    }
    val breakSeconds = 5 * 60
    var onBreak by remember(task.id) { mutableStateOf(false) }
    var remaining by remember(task.id) { mutableStateOf(focusSeconds) }
    var running by remember(task.id) { mutableStateOf(true) }

    LaunchedEffect(task.id, running, onBreak) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (running && remaining <= 0) {
            if (!onBreak) { onBreak = true; remaining = breakSeconds }
            else { onBreak = false; remaining = focusSeconds; running = false }
        }
    }

    val total = if (onBreak) breakSeconds else focusSeconds
    val progress = 1f - remaining.toFloat() / total
    val ring = if (onBreak) Sage else Champagne
    val mm = remaining / 60
    val ss = remaining % 60

    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.985f)).padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (onBreak) "BREAK" else "FOCUS",
                style = MaterialTheme.typography.labelMedium, color = ring
            )
            Spacer(Modifier.height(10.dp))
            Text(
                task.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val d = Size(size.width - stroke, size.height - stroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
                    drawArc(Hairline, -90f, 360f, false,
                        topLeft = topLeft, size = d, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(ring, -90f, 360f * progress, false,
                        topLeft = topLeft, size = d, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(
                    "%02d:%02d".format(mm, ss),
                    style = MaterialTheme.typography.displayLarge
                )
            }

            Spacer(Modifier.height(40.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text(if (running) "Pause" else "Resume", color = IvoryDim) }
                Button(
                    onClick = { onComplete(task) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Champagne, contentColor = OnChampagne)
                ) { Text("Done") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Close timer", color = IvoryDim) }
        }
    }
}
