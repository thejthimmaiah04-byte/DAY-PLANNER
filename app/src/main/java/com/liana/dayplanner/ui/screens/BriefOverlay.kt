package com.liana.dayplanner.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.brief.BriefController
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.ChampagneDeep
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Surface2

@Composable
fun BriefOverlay(controller: BriefController) {
    if (!controller.visible) return
    val scope = rememberCoroutineScope()
    var showVoice by remember { mutableStateOf(controller.settingsOnly) }

    LaunchedEffect(Unit) { controller.loadVoices(scope) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "pulse"
    )

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = Surface1,
        selectedContainerColor = Champagne,
        selectedLabelColor = OnChampagne
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.98f))
            .padding(28.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!controller.settingsOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = controller.includeWeather,
                        onClick = { controller.setWeatherPref(!controller.includeWeather) },
                        label = { Text("Weather") }, colors = chipColors
                    )
                    FilterChip(
                        selected = controller.includeNews,
                        onClick = { controller.setNewsPref(!controller.includeNews) },
                        label = { Text("Headlines") }, colors = chipColors
                    )
                    FilterChip(
                        selected = showVoice,
                        onClick = { showVoice = !showVoice },
                        label = { Text("Voice") }, colors = chipColors
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Calm" to 0.85f, "Natural" to 0.95f, "Brisk" to 1.05f).forEach { (label, r) ->
                        FilterChip(
                            selected = controller.rate == r,
                            onClick = { controller.setRatePref(r) },
                            label = { Text(label) }, colors = chipColors
                        )
                    }
                }
            }

            if (showVoice || controller.settingsOnly) {
                VoicePanel(controller, scope, chipColors, Modifier.weight(1f))
            } else {
                Spacer(Modifier.height(44.dp))
                Box(
                    Modifier
                        .size(90.dp)
                        .border(1.dp, ChampagneDeep, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .scale(pulse)
                            .background(Champagne, CircleShape)
                    )
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    controller.section.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Champagne
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    controller.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IvoryDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            if (controller.settingsOnly) {
                Button(
                    onClick = { controller.end() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Champagne, contentColor = OnChampagne)
                ) { Text("Done") }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { controller.skipSection() },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Skip section", color = IvoryDim) }
                    Button(
                        onClick = { controller.end() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Champagne, contentColor = OnChampagne)
                    ) { Text("End brief") }
                }
            }
        }
    }
}

@Composable
private fun VoicePanel(
    controller: BriefController,
    scope: kotlinx.coroutines.CoroutineScope,
    chipColors: androidx.compose.material3.SelectableChipColors,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Text("Accent", style = MaterialTheme.typography.labelMedium, color = IvoryFaint)
        Spacer(Modifier.height(8.dp))

        if (!controller.voicesReady) {
            Text("Loading installed voices…",
                style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
        } else if (controller.availableAccents.isEmpty()) {
            Text("No offline English voice is installed. Open Android Settings → " +
                "System → Languages & input → Text-to-speech → Google → Install voice data, " +
                "and add English (India) or your preferred accent.",
                style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
        } else {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                controller.availableAccents.forEach { (country, label) ->
                    FilterChip(
                        selected = controller.accent == country,
                        onClick = { controller.setAccent(scope, country) },
                        label = { Text(label) }, colors = chipColors
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Voice", style = MaterialTheme.typography.labelMedium, color = IvoryFaint)
            Spacer(Modifier.height(8.dp))

            val forAccent = controller.voices.filter { it.country == controller.accent }
            LazyColumn(
                Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(forAccent, key = { it.id }) { v ->
                    val selected = controller.voiceId == v.id
                    Surface(
                        color = if (selected) Surface2 else Surface1,
                        shape = RoundedCornerShape(14.dp),
                        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Champagne) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setVoice(scope, v.id, preview = true) }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(v.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) Champagne else MaterialTheme.colorScheme.onSurface)
                                Text(v.accentLabel,
                                    style = MaterialTheme.typography.bodyMedium, color = IvoryDim)
                            }
                            Icon(Icons.Rounded.PlayArrow, "Preview", tint = Champagne)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { controller.previewCurrent(scope) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Champagne,
                    modifier = Modifier.padding(end = 6.dp))
                Text("Preview selected voice", color = Champagne)
            }
        }
    }
}
