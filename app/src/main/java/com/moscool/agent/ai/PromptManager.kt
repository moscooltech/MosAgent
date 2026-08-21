package com.moscool.agent.ai

import com.moscool.agent.model.AIProviderConfig
import com.moscool.agent.model.ChatMessage
import com.moscool.agent.model.UIElementInfo

/**
 * Builds and manages prompts sent to the AI.
 */
object PromptManager {

    private const val SYSTEM_PROMPT = """You are Moscool AI Agent, an intelligent Android automation assistant. You help users perform tasks on their Android device by controlling apps through the accessibility service.

## Core Capabilities
- Understand natural language commands
- Generate social media content (Facebook posts, etc.)
- Open apps and navigate their UI
- Find and tap UI elements by their text, description, or properties
- Type text into fields
- Scroll and swipe
- Read the current screen
- Verify actions after execution

## Rules
1. NEVER execute destructive actions without asking the user first.
2. NEVER publish social media posts without explicit user approval.
3. NEVER bypass security mechanisms (CAPTCHA, passwords, 2FA, biometrics).
4. NEVER execute arbitrary shell commands.
5. Always explain what you're doing and why.
6. If you can't find a UI element, say so rather than guessing.
7. Prefer accessibility-based element matching over coordinates.
8. After each action, observe the result before proceeding.

## Action Format
When you need to perform an action, use the appropriate tool. Each action has a clear schema.

## Social Media Posts
When asked to create a social media post:
1. Generate engaging content with HOOK, BODY, and CALL TO ACTION
2. Present it to the user for review and editing
3. Only proceed to post after explicit approval
4. The post should be appropriate for the platform

## Facebook Post Structure
- Start with an attention-grabbing hook
- Provide valuable content in the body
- End with a clear call to action
- Use relevant hashtags if appropriate
- Use emojis tastefully
- Keep it appropriate for the target audience"""

    fun getSystemPrompt(): String = SYSTEM_PROMPT

    fun buildInitialMessages(
        userCommand: String,
        screenInfo: String? = null
    ): List<ChatMessage> {
        val messages = mutableListOf(
            ChatMessage(role = "system", content = SYSTEM_PROMPT),
            ChatMessage(role = "user", content = userCommand)
        )
        if (!screenInfo.isNullOrBlank()) {
            messages.add(
                ChatMessage(
                    role = "system",
                    content = "Current screen information:\n$screenInfo"
                )
            )
        }
        return messages
    }

    fun buildSocialPostPrompt(
        command: String,
        topic: String,
        business: String? = null,
        audience: String? = null,
        tone: String? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Create a social media post with the following requirements:")
        sb.appendLine()
        sb.appendLine("Command: $command")
        if (topic.isNotBlank()) sb.appendLine("Topic: $topic")
        if (!business.isNullOrBlank()) sb.appendLine("Business: $business")
        if (!audience.isNullOrBlank()) sb.appendLine("Target audience: $audience")
        if (!tone.isNullOrBlank()) sb.appendLine("Tone: $tone")
        sb.appendLine()
        sb.appendLine("Generate the post with:")
        sb.appendLine("1. An attention-grabbing HOOK (first line)")
        sb.appendLine("2. Engaging BODY content")
        sb.appendLine("3. A clear CALL TO ACTION")
        sb.appendLine("4. Optional relevant HASHTAGS")
        sb.appendLine()
        sb.appendLine("Output the post in this format:")
        sb.appendLine("HOOK: <the hook>")
        sb.appendLine("BODY: <the body>")
        sb.appendLine("CTA: <call to action>")
        sb.appendLine("HASHTAGS: <comma-separated hashtags, or none>")
        return sb.toString()
    }

    fun buildScreenAnalysisPrompt(
        userGoal: String,
        screenElements: List<UIElementInfo>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("The user wants to: $userGoal")
        sb.appendLine()
        sb.appendLine("Current screen elements:")
        screenElements.forEachIndexed { index, element ->
            sb.append("$index: ")
            element.text?.let { sb.append("text=\"$it\" ") }
            element.contentDescription?.let { sb.append("desc=\"$it\" ") }
            element.resourceId?.let { sb.append("id=$it ") }
            element.className?.let { sb.append("class=$it ") }
            if (element.isClickable) sb.append("[clickable] ")
            if (element.isEditable) sb.append("[editable] ")
            if (element.isScrollable) sb.append("[scrollable] ")
            sb.appendLine()
        }
        sb.appendLine()
        sb.appendLine("Which element should be interacted with to progress toward the goal? Respond with the index number and the action to take.")
        return sb.toString()
    }

    fun buildActionPlanPrompt(
        command: String,
        currentScreen: String? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Create a step-by-step plan to accomplish this task:")
        sb.appendLine("\"$command\"")
        sb.appendLine()
        sb.appendLine("For each step, specify:")
        sb.appendLine("- Action type (OPEN_APP, TAP, TYPE, SCROLL, PRESS_BACK, WAIT, etc.)")
        sb.appendLine("- Target (text, description, or element to interact with)")
        sb.appendLine("- Expected result")
        sb.appendLine("- Whether this step needs user confirmation (true/false)")
        if (!currentScreen.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("Current screen: $currentScreen")
        }
        return sb.toString()
    }
}
