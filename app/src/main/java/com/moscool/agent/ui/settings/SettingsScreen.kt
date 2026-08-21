package com.moscool.agent.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.moscool.agent.model.AIProviderConfig
import com.moscool.agent.model.AIProviderType
import com.moscool.agent.model.AutomationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: AIProviderConfig,
    automationMode: AutomationMode,
    accessibilityEnabled: Boolean,
    debugLogging: Boolean,
    visionEnabled: Boolean,
    versionName: String,
    onSaveConfig: (AIProviderConfig) -> Unit,
    onSaveMode: (AutomationMode) -> Unit,
    onToggleDebugLogging: (Boolean) -> Unit,
    onToggleVision: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    var editedConfig by remember { mutableStateOf(config) }
    var showApiKey by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // AI Provider Section
        SectionHeader("AI Provider")

        Spacer(modifier = Modifier.height(8.dp))

        // Provider type dropdown
        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = it }
        ) {
            OutlinedTextField(
                value = editedConfig.providerType.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Provider") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false }
            ) {
                AIProviderType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            editedConfig = editedConfig.copy(
                                providerType = type,
                                baseUrl = ""
                            )
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Base URL
        OutlinedTextField(
            value = editedConfig.baseUrl.ifBlank { editedConfig.defaultBaseUrl() },
            onValueChange = { editedConfig = editedConfig.copy(baseUrl = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL") },
            placeholder = { Text(editedConfig.defaultBaseUrl()) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // API Key
        OutlinedTextField(
            value = editedConfig.apiKey,
            onValueChange = { editedConfig = editedConfig.copy(apiKey = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility"
                    )
                }
            },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Model
        OutlinedTextField(
            value = editedConfig.model,
            onValueChange = { editedConfig = editedConfig.copy(model = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model") },
            placeholder = { Text("e.g., gpt-4o, gemini-pro, llama-3.1-70b") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSaveConfig(editedConfig) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = editedConfig.apiKey.isNotBlank() && editedConfig.model.isNotBlank()
        ) {
            Text("Save AI Configuration")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Automation Mode
        SectionHeader("Automation Mode")

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = modeExpanded,
            onExpandedChange = { modeExpanded = it }
        ) {
            OutlinedTextField(
                value = when (automationMode) {
                    AutomationMode.SAFE -> "Safe Mode"
                    AutomationMode.ASSISTED -> "Assisted Mode"
                    AutomationMode.AUTONOMOUS -> "Autonomous Mode"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Mode") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = modeExpanded,
                onDismissRequest = { modeExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Safe Mode\nDefault. Asks before all sensitive actions.") },
                    onClick = { onSaveMode(AutomationMode.SAFE); modeExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Assisted Mode\nRoutine auto, pauses for sensitive actions.") },
                    onClick = { onSaveMode(AutomationMode.ASSISTED); modeExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Autonomous Mode\nExplicit warning shown. Configurable permissions.") },
                    onClick = { onSaveMode(AutomationMode.AUTONOMOUS); modeExpanded = false }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Accessibility
        SectionHeader("Accessibility Service")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (accessibilityEnabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Accessibility,
                    contentDescription = null,
                    tint = if (accessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (accessibilityEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (accessibilityEnabled) "Service is running" else "Required for automation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggles
        SectionHeader("Features")

        Spacer(modifier = Modifier.height(8.dp))

        ToggleRow("Debug Logging", debugLogging, onToggleDebugLogging)
        ToggleRow("Vision (Screen Analysis)", visionEnabled, onToggleVision)

        Spacer(modifier = Modifier.height(24.dp))

        // About
        SectionHeader("About")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Moscool AI Agent v${versionName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Clear history
        TextButton(onClick = { showClearDialog = true }) {
            Text("Clear Task History", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all task history?") },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showClearDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
