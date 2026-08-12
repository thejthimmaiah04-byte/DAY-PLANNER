package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liana.dayplanner.data.Milestone
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.data.Vision
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.ChampagneDeep
import com.liana.dayplanner.ui.theme.Fraunces
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Terracotta
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private fun milestoneProgress(ms: Milestone, tasks: List<Task>): Float {
    val linked = tasks.filter { it.milestoneId == ms.id }
    if (linked.isEmpty()) return if (ms.done) 1f else 0f
    return linked.count { it.isDone }.toFloat() / linked.size
}

@Composable
fun VisionScreen(
    vision: Vision,
    tasks: List<Task>,
    onSave: (Vision) -> Unit,
    onVisionCheck: (String) -> Unit,
    onMarkReviewed: () -> Unit,
    initialTodayCheck: String?,
    daysSinceReview: Long?,
    contentPadding: PaddingValues
) {
    var statement by rememberSaveable(vision.statement) { mutableStateOf(vision.statement) }
    var newMilestone by rememberSaveable { mutableStateOf("") }
    var todayCheck by rememberSaveable { mutableStateOf(initialTodayCheck) }
    var reviewedNow by rememberSaveable { mutableStateOf(false) }

    val needsReview = !reviewedNow && vision.statement.isNotBlank() &&
        (daysSinceReview == null || daysSinceReview >= 30)

    val monthsLeft = vision.target.takeIf { it.isNotBlank() }?.let {
        runCatching {
            val ym = YearMonth.parse(it)
            val now = YearMonth.now()
            (ym.year - now.year) * 12 + (ym.monthValue - now.monthValue)
        }.getOrNull()
    }
    val overall = if (vision.milestones.isEmpty()) 0f
        else vision.milestones.map { milestoneProgress(it, tasks) }.average().toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "2-Year Vision",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
        }

        // 30-day review nudge
        if (needsReview) {
            item {
                Surface(
                    color = Champagne.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ChampagneDeep.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (daysSinceReview == null)
                                    "Your vision hasn't been reviewed yet."
                                else
                                    "Your vision was last reviewed $daysSinceReview days ago.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Read it below — still true?",
                                style = MaterialTheme.typography.bodyMedium, color = IvoryDim
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { reviewedNow = true; onMarkReviewed() }) {
                            Text("Still true", color = Champagne,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = statement,
                onValueChange = {
                    statement = it
                    onSave(vision.copy(statement = it))
                },
                placeholder = {
                    Text("Who do you want to be in two years? Write it as if it is already true.",
                        color = IvoryDim)
                },
                textStyle = TextStyle(
                    fontFamily = Fraunces, fontSize = 18.sp, lineHeight = 27.sp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Champagne,
                    unfocusedBorderColor = Hairline,
                    cursorColor = Champagne
                ),
                minLines = 3
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val yearNow = LocalDate.now().year
                val options = (0..3).map { YearMonth.of(yearNow + it, 7) }
                OutlinedButton(onClick = {
                    val cur = runCatching { YearMonth.parse(vision.target) }.getOrNull()
                    val idx = options.indexOfFirst { it == cur }
                    val next = options[(idx + 1).mod(options.size)]
                    onSave(vision.copy(target = next.toString()))
                }) {
                    Text(
                        vision.target.takeIf { it.isNotBlank() }?.let {
                            runCatching {
                                YearMonth.parse(it).format(
                                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                            }.getOrDefault(it)
                        } ?: "Set target",
                        color = Champagne
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        monthsLeft == null -> "Tap to set your horizon"
                        monthsLeft > 0 -> "$monthsLeft months remaining"
                        monthsLeft == 0 -> "This is the month."
                        else -> "Target passed — set a new horizon"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryDim
                )
            }
        }

        // Daily vision alignment check
        if (vision.statement.isNotBlank()) {
            item {
                Surface(
                    color = Surface1,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Did today serve your vision?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("yes" to "Yes", "partial" to "Partially", "no" to "No")
                                .forEach { (value, label) ->
                                    val selected = todayCheck == value
                                    Surface(
                                        color = if (selected) Champagne.copy(alpha = 0.14f)
                                                else Surface1,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) Champagne else Hairline
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                todayCheck = value
                                                onVisionCheck(value)
                                            }
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) Champagne else IvoryDim,
                                            modifier = Modifier
                                                .padding(vertical = 10.dp)
                                                .fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }

        item {
            SectionLabel("Overall progress", Modifier.padding(top = 10.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { overall },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = Champagne,
                    trackColor = Hairline
                )
                Spacer(Modifier.width(10.dp))
                Text("${(overall * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
            }
        }

        item { SectionLabel("Milestones", Modifier.padding(top = 12.dp)) }

        if (vision.milestones.isEmpty()) {
            item {
                Text(
                    "Break the vision into 4–8 concrete milestones. Then link tasks to them " +
                    "from the task editor (category: 2-Year Vision) — progress fills in by itself.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryDim
                )
            }
        }

        items(vision.milestones, key = { it.id }) { ms ->
            val linked = tasks.filter { it.milestoneId == ms.id }
            val progress = milestoneProgress(ms, tasks)
            Surface(color = Surface1, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (linked.isEmpty()) {
                            Checkbox(
                                checked = ms.done,
                                onCheckedChange = { checked ->
                                    onSave(vision.copy(milestones = vision.milestones.map {
                                        if (it.id == ms.id) it.copy(done = checked) else it
                                    }))
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Champagne, checkmarkColor = OnChampagne)
                            )
                        }
                        Text(ms.title, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        if (linked.isNotEmpty()) {
                            Text("${linked.count { it.isDone }}/${linked.size} tasks",
                                style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                        }
                        IconButton(onClick = {
                            onSave(vision.copy(
                                milestones = vision.milestones.filter { it.id != ms.id }))
                        }) {
                            Icon(Icons.Rounded.Close, "Delete milestone",
                                tint = Terracotta, modifier = Modifier.width(18.dp))
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(Hairline, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(5.dp)
                                .background(Champagne, RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newMilestone,
                    onValueChange = { newMilestone = it },
                    placeholder = { Text("New milestone…", color = IvoryDim) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Champagne,
                        unfocusedBorderColor = Hairline,
                        cursorColor = Champagne
                    ),
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = {
                    if (newMilestone.isBlank()) return@OutlinedButton
                    onSave(vision.copy(milestones = vision.milestones +
                        Milestone("ms" + System.currentTimeMillis(), newMilestone.trim())))
                    newMilestone = ""
                }) { Text("Add", color = Champagne) }
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }
}
