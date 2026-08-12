package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Hairline
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.Surface1
import com.liana.dayplanner.ui.theme.Terracotta

/**
 * Appears right after a task is completed, offering to capture its outcome —
 * the note that later reads back in Meeting mode. Fully skippable.
 */
@Composable
fun CompletionNoteDialog(
    taskTitle: String,
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = Surface1,
        title = { Text("Completed ✓") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Champagne,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "What was the outcome?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryDim
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Optional — e.g. \"Shipped, awaiting review\"", color = IvoryFaint) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Champagne,
                        unfocusedBorderColor = Hairline,
                        cursorColor = Champagne
                    ),
                    minLines = 2
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onUndo) {
                    Text("Actually, mark it not done", color = Terracotta,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(note) },
                enabled = note.isNotBlank()
            ) { Text("Save outcome", color = if (note.isNotBlank()) Champagne else IvoryFaint) }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Skip", color = IvoryDim) }
        }
    )
}
