package com.moscool.agent.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moscool.agent.model.TaskState

@Composable
fun HomeScreen(
    statusMessage: String,
    taskState: TaskState,
    accessibilityEnabled: Boolean,
    aiConfigured: Boolean,
    onExecute: (String) -> Unit,
    onStop: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    val isWorking = taskState != TaskState.IDLE && taskState != TaskState.COMPLETED &&
            taskState != TaskState.FAILED && taskState != TaskState.CANCELLED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Moscool AI Agent",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Your AI-powered phone assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusRow(
                    label = "Accessibility",
                    enabled = accessibilityEnabled,
                    enabledText = "Active",
                    disabledText = "Disabled"
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = "AI Provider",
                    enabled = aiConfigured,
                    enabledText = "Configured",
                    disabledText = "Not configured"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Command input
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What would you like me to do?") },
            placeholder = { Text("e.g., Create a Facebook post about AC maintenance") },
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Microphone button (placeholder for voice)
            IconButton(
                onClick = { /* TODO: Voice input */ },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isWorking) {
                FilledTonalButton(
                    onClick = onStop,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STOP AGENT")
                }
            } else {
                FilledTonalButton(
                    onClick = {
                        if (command.isNotBlank()) {
                            onExecute(command)
                            command = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    enabled = command.isNotBlank() && aiConfigured
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status message
        if (statusMessage.isNotBlank()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        QuickActionChip("Open Facebook") { onExecute("Open Facebook") }
        QuickActionChip("Open Telegram") { onExecute("Open Telegram") }
        QuickActionChip("Create a Facebook post") { command = "Create a Facebook post about AC maintenance" }
    }
}

@Composable
private fun StatusRow(
    label: String,
    enabled: Boolean,
    enabledText: String,
    disabledText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (enabled) enabledText else disabledText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QuickActionChip(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}
