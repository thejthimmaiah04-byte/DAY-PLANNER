package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.data.WeeklyReflection
import com.liana.dayplanner.data.WeeklyReflectionStore
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.OnChampagne

@Composable
fun WeeklyReflectionScreen(
    existing: WeeklyReflection?,
    onSave: (WeeklyReflection) -> Unit,
    onClose: () -> Unit
) {
    var bestDay by remember { mutableStateOf(existing?.bestDay ?: "") }
    var obstacle by remember { mutableStateOf(existing?.obstacle ?: "") }
    var adjustment by remember { mutableStateOf(existing?.adjustment ?: "") }

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly Reflection", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "30 seconds of honest reflection changes next week.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvoryDim
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, "Close", tint = IvoryDim)
                }
            }
            Spacer(Modifier.height(36.dp))

            ReflectionQuestion(
                number = "01",
                question = "What was your best day this week, and why?",
                value = bestDay,
                onValueChange = { bestDay = it }
            )
            Spacer(Modifier.height(28.dp))
            ReflectionQuestion(
                number = "02",
                question = "What got in the way?",
                value = obstacle,
                onValueChange = { obstacle = it }
            )
            Spacer(Modifier.height(28.dp))
            ReflectionQuestion(
                number = "03",
                question = "One adjustment for next week.",
                value = adjustment,
                onValueChange = { adjustment = it }
            )
            Spacer(Modifier.height(44.dp))

            Button(
                onClick = {
                    onSave(WeeklyReflection(
                        weekOf = WeeklyReflectionStore.currentWeekOf(),
                        bestDay = bestDay.trim(),
                        obstacle = obstacle.trim(),
                        adjustment = adjustment.trim()
                    ))
                },
                enabled = bestDay.isNotBlank() || obstacle.isNotBlank() || adjustment.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Champagne,
                    contentColor = OnChampagne,
                    disabledContainerColor = Hairline
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Save reflection",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ReflectionQuestion(
    number: String,
    question: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(number, style = MaterialTheme.typography.labelMedium, color = Champagne)
        Spacer(Modifier.height(6.dp))
        Text(question, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Your answer…", color = IvoryFaint) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Champagne,
                unfocusedBorderColor = Hairline,
                cursorColor = Champagne
            ),
            minLines = 3
        )
    }
}
