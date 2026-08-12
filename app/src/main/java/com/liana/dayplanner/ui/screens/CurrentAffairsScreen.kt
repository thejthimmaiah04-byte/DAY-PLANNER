package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.ui.EmptyState
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Sage
import com.liana.dayplanner.ui.theme.Surface1

/**
 * Current-affairs capture: the headlines the last brief pulled from Indian
 * sources. Tapping "Revise" turns a headline into a spaced-revision task
 * (subject Current Affairs), which resurfaces on the 1-3-7-15-30 day curve.
 */
@Composable
fun CurrentAffairsScreen(
    date: String,
    headlines: List<String>,
    onSaveForRevision: (String) -> Unit,
    onClose: () -> Unit
) {
    val saved = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Current Affairs", style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Done", color = Champagne) }
        }
        Text(
            if (headlines.isEmpty()) "" else "From your brief${if (date.isNotBlank()) " · $date" else ""}",
            style = MaterialTheme.typography.bodyMedium, color = IvoryDim,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (headlines.isEmpty()) {
            EmptyState(
                "No headlines yet",
                "Run the Morning brief with Headlines on, then come back to save items for revision."
            )
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            items(headlines) { h ->
                val isSaved = saved.contains(h)
                Surface(color = Surface1, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(h, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        if (isSaved) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Check, null, tint = Sage,
                                    modifier = Modifier.height(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Added to revision", color = Sage,
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(onClick = { onSaveForRevision(h); saved.add(h) }) {
                                Text("Revise this", color = Champagne)
                            }
                        }
                    }
                }
            }
        }
    }
}
