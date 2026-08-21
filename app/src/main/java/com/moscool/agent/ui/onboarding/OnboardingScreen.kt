package com.moscool.agent.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    accessibilityEnabled: Boolean,
    onComplete: () -> Unit,
    onConfigureAI: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        },
        label = "onboarding_step"
    ) { step ->
        when (step) {
            0 -> OnboardingStep(
                icon = Icons.Default.SmartToy,
                title = "Welcome to Moscool AI Agent",
                description = "Your AI-powered phone assistant that can understand natural language commands and perform tasks on your device.\n\nLet's set up a few things to get started.",
                primaryButton = "Next",
                onPrimaryClick = { currentStep = 1 }
            )

            1 -> OnboardingStep(
                icon = Icons.Default.SmartToy,
                title = "AI Provider Setup",
                description = "Configure your AI provider to enable intelligent task execution.\n\nYou can use OpenAI, Gemini, OpenRouter, Groq, or any OpenAI-compatible endpoint.",
                primaryButton = "Configure AI Provider",
                onPrimaryClick = onConfigureAI,
                secondaryButton = "Skip for now",
                onSecondaryClick = { currentStep = 2 },
                secondaryNote = if (accessibilityEnabled) "Accessibility is enabled ✓" else null
            )

            2 -> OnboardingStep(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                description = "The Accessibility Service allows Moscool AI Agent to:\n\n• Read UI elements from other apps\n• Tap buttons and links\n• Enter text into fields\n• Navigate app screens\n• Verify actions\n\nThis is required for the agent to work.",
                primaryButton = if (accessibilityEnabled) "Enabled ✓" else "Enable Accessibility Service",
                onPrimaryClick = {
                    if (!accessibilityEnabled) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                secondaryButton = "Next",
                onSecondaryClick = { currentStep = 3 }
            )

            3 -> OnboardingStep(
                icon = Icons.Default.CheckCircle,
                title = "You're All Set!",
                description = "You can now give natural-language commands and let the agent work for you.\n\nTry:\n• \"Open Facebook\"\n• \"Create a post about AC maintenance\"\n• \"Search YouTube for Python tutorials\"",
                primaryButton = "Get Started",
                onPrimaryClick = onComplete,
                isFinal = true
            )
        }
    }
}

@Composable
private fun OnboardingStep(
    icon: ImageVector,
    title: String,
    description: String,
    primaryButton: String,
    onPrimaryClick: () -> Unit,
    secondaryButton: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryNote: String? = null,
    isFinal: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !primaryButton.contains("✓")
        ) {
            Text(primaryButton)
        }

        if (secondaryButton != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSecondaryClick ?: {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(secondaryButton)
            }
        }

        if (secondaryNote != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = secondaryNote,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
