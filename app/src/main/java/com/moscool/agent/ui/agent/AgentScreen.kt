package com.moscool.agent.ui.agent

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moscool.agent.agent.AgentEngine
import com.moscool.agent.model.TaskState

@Composable
fun AgentScreen(
    timelineEvents: List<AgentEngine.TimelineEvent>,
    taskState: TaskState,
    onStop: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new events arrive
    LaunchedEffect(timelineEvents.size) {
        if (timelineEvents.isNotEmpty()) {
            listState.animateScrollToItem(timelineEvents.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Agent",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Task state
        Text(
            text = "Status: ${taskState.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = when (taskState) {
                TaskState.COMPLETED -> MaterialTheme.colorScheme.primary
                TaskState.FAILED -> MaterialTheme.colorScheme.error
                TaskState.CANCELLED -> MaterialTheme.colorScheme.error
                TaskState.WAITING_FOR_CONFIRMATION -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stop button
        if (taskState != TaskState.IDLE && taskState != TaskState.COMPLETED &&
            taskState != TaskState.FAILED && taskState != TaskState.CANCELLED
        ) {
            FilledTonalButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("STOP AGENT", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Timeline
        if (timelineEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events yet. Enter a command on the Home screen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timelineEvents) { event ->
                    TimelineEventCard(event)
                }
            }
        }
    }
}

@Composable
private fun TimelineEventCard(event: AgentEngine.TimelineEvent) {
    val (icon, iconColor) = when (event.type) {
        AgentEngine.EventType.SUCCESS -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        AgentEngine.EventType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
        AgentEngine.EventType.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
        AgentEngine.EventType.ERROR -> Icons.Default.Error to MaterialTheme.colorScheme.error
        AgentEngine.EventType.PENDING -> Icons.Default.Info to Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
