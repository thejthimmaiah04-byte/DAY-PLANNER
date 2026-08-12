package com.liana.dayplanner.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liana.dayplanner.data.AppSettings
import com.liana.dayplanner.ui.SectionLabel
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Fraunces
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1

@Composable
fun OnboardingScreen(
    initial: AppSettings,
    onFinish: (AppSettings, String) -> Unit
) {
    var workStart by remember { mutableStateOf(initial.workStart) }
    var workEnd by remember { mutableStateOf(initial.workEnd) }
    var visionText by remember { mutableStateOf("") }

    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    Column(
        Modifier.fillMaxSize().background(Ink).padding(28.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome to ARC", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "A calm, offline planner that shapes each day around your work and your " +
            "two-year vision. Two quick things to tune it to you.",
            style = MaterialTheme.typography.bodyLarge, color = IvoryDim
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("When do you work?")
        Spacer(Modifier.height(10.dp))
        StepperRow("Work starts", workStart, { workStart = it.coerceAtMost(workEnd - 30) }, ::fmt)
        Spacer(Modifier.height(8.dp))
        StepperRow("Work ends", workEnd, { workEnd = it.coerceAtLeast(workStart + 30) }, ::fmt)
        Spacer(Modifier.height(6.dp))
        Text("Work tasks stay inside these hours; mornings and evenings are reserved " +
            "for your growth and skills.",
            style = MaterialTheme.typography.bodyMedium, color = IvoryDim)

        Spacer(Modifier.height(28.dp))
        SectionLabel("Who do you want to become?")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = visionText,
            onValueChange = { visionText = it },
            placeholder = { Text("In two years I am…", color = IvoryDim) },
            textStyle = TextStyle(fontFamily = Fraunces, fontSize = 18.sp, lineHeight = 27.sp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Champagne,
                unfocusedBorderColor = Hairline,
                cursorColor = Champagne
            ),
            minLines = 3
        )
        Spacer(Modifier.height(6.dp))
        Text("You can refine this any time on the Vision screen, and break it into milestones.",
            style = MaterialTheme.typography.bodyMedium, color = IvoryDim)

        Spacer(Modifier.height(36.dp))
        Button(
            onClick = {
                onFinish(
                    initial.copy(workStart = workStart, workEnd = workEnd, onboarded = true),
                    visionText.trim()
                )
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Champagne, contentColor = OnChampagne)
        ) { Text("Start planning", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun StepperRow(label: String, minutes: Int, onChange: (Int) -> Unit, fmt: (Int) -> String) {
    Surface(color = Surface1, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onChange((minutes - 15).coerceAtLeast(0)) }) {
                Icon(Icons.Rounded.Remove, "Earlier", tint = IvoryDim)
            }
            Text(fmt(minutes), style = MaterialTheme.typography.titleMedium, color = Champagne)
            IconButton(onClick = { onChange((minutes + 15).coerceAtMost(24 * 60)) }) {
                Icon(Icons.Rounded.Add, "Later", tint = IvoryDim)
            }
        }
    }
}
