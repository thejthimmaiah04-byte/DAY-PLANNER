package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.AppSettings
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.ChampagneDeep
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.Surface1

private fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)

@Composable
private fun TimeStepper(
    label: String,
    minutes: Int,
    step: Int = 15,
    min: Int = 0,
    max: Int = 24 * 60,
    onChange: (Int) -> Unit
) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onChange((minutes - step).coerceAtLeast(min)) }) {
                Icon(Icons.Rounded.Remove, "Earlier", tint = IvoryDim)
            }
            Text(fmt(minutes), style = MaterialTheme.typography.titleMedium, color = Champagne)
            IconButton(onClick = { onChange((minutes + step).coerceAtMost(max)) }) {
                Icon(Icons.Rounded.Add, "Later", tint = IvoryDim)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Done", color = Champagne) }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionLabel("Work hours") }
            item {
                TimeStepper("Work starts", settings.workStart) {
                    onSave(settings.copy(workStart = it.coerceAtMost(settings.workEnd - 30)))
                }
            }
            item {
                TimeStepper("Work ends", settings.workEnd) {
                    onSave(settings.copy(workEnd = it.coerceAtLeast(settings.workStart + 30)))
                }
            }
            item {
                Text("Work tasks are scheduled inside these hours. Everything else " +
                    "(vision, skills, personal) fills the rest of your day.",
                    style = MaterialTheme.typography.bodyMedium, color = IvoryDim,
                    modifier = Modifier.padding(vertical = 2.dp))
            }

            item { SectionLabel("Day window", Modifier.padding(top = 10.dp)) }
            item {
                TimeStepper("Day starts", settings.dayStart) {
                    onSave(settings.copy(dayStart = it.coerceAtMost(settings.workStart)))
                }
            }
            item {
                TimeStepper("Day ends", settings.dayEnd) {
                    onSave(settings.copy(dayEnd = it.coerceAtLeast(settings.workEnd)))
                }
            }

            item { SectionLabel("Daily notifications", Modifier.padding(top = 10.dp)) }
            item {
                TimeStepper("Morning brief", settings.morningHour * 60 + settings.morningMinute,
                    onChange = { onSave(settings.copy(morningHour = it / 60, morningMinute = it % 60)) })
            }
            item {
                TimeStepper("Evening wind-down", settings.eveningHour * 60 + settings.eveningMinute,
                    onChange = { onSave(settings.copy(eveningHour = it / 60, eveningMinute = it % 60)) })
            }
            item {
                Text("Times take effect from tomorrow. Grant Alarms & reminders in " +
                    "Android Settings → Apps → ARC so they fire exactly.",
                    style = MaterialTheme.typography.bodyMedium, color = IvoryDim,
                    modifier = Modifier.padding(vertical = 2.dp))
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}
