package com.moscool.agent.agent

import com.moscool.agent.ai.AIProvider
import com.moscool.agent.ai.PromptManager
import com.moscool.agent.ai.ToolRegistry
import com.moscool.agent.automation.AccessibilityController
import com.moscool.agent.automation.AppLauncher
import com.moscool.agent.data.repository.LogRepository
import com.moscool.agent.model.ActionType
import com.moscool.agent.model.AgentAction
import com.moscool.agent.model.AutomationMode
import com.moscool.agent.model.ChatMessage
import com.moscool.agent.model.SocialPost
import com.moscool.agent.model.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The main agent engine. Implements the observe → reason → act → verify loop.
 */
class AgentEngine(
    private val aiProvider: AIProvider,
    private val accessibilityController: AccessibilityController,
    private val appLauncher: AppLauncher,
    private val logRepository: LogRepository
) {
    private val taskManager = TaskManager()
    private val memory = AgentMemory()
    private val planner = AgentPlanner(aiProvider)
    private val executor = ActionExecutor(accessibilityController, appLauncher)
    private val verifier = ActionVerifier(accessibilityController)

    private var currentJob: Job? = null
    private var isStopped = false

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _timelineEvents = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<TimelineEvent>> = _timelineEvents.asStateFlow()

    private val _generatedPost = MutableStateFlow<SocialPost?>(null)
    val generatedPost: StateFlow<SocialPost?> = _generatedPost.asStateFlow()

    private val _needsConfirmation = MutableStateFlow(false)
    val needsConfirmation: StateFlow<Boolean> = _needsConfirmation.asStateFlow()

    private val _confirmationMessage = MutableStateFlow("")
    val confirmationMessage: StateFlow<String> = _confirmationMessage.asStateFlow()

    val currentTask: StateFlow<Task?> = taskManager.currentTask
    val taskState: StateFlow<com.moscool.agent.model.TaskState> = taskManager.taskState

    data class TimelineEvent(
        val message: String,
        val type: EventType = EventType.INFO,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class EventType { SUCCESS, INFO, WARNING, ERROR, PENDING }

    /**
     * Main entry point: execute a user command.
     */
    suspend fun executeCommand(command: String, mode: AutomationMode = AutomationMode.SAFE) {
        if (aiProvider.isConfigured().not()) {
            addTimelineEvent("AI provider is not configured. Please set up an AI provider in Settings.", EventType.ERROR)
            _statusMessage.value = "AI provider not configured"
            return
        }

        isStopped = false
        _timelineEvents.value = emptyList()
        _generatedPost.value = null
        memory.clear()

        val task = taskManager.createTask(command)
        memory.setCurrentTask(task)
        memory.addUserMessage(command)

        addTimelineEvent("Understanding request...", EventType.INFO)
        _statusMessage.value = "Understanding..."

        try {
            // Phase 1: Understand and plan
            taskManager.updateState(com.moscool.agent.model.TaskState.UNDERSTANDING)
            val plan = generatePlan(command, mode)
            if (plan.isEmpty()) {
                taskManager.failTask("Could not generate a plan for this command.")
                addTimelineEvent("Failed to create a plan.", EventType.ERROR)
                return
            }

            addTimelineEvent("Plan created: ${plan.size} steps", EventType.SUCCESS)
            taskManager.setPlan(plan)

            // Phase 2: Execute each step
            for ((index, action) in plan.withIndex()) {
                if (isStopped) {
                    taskManager.cancelTask()
                    addTimelineEvent("Agent stopped by user.", EventType.WARNING)
                    return
                }

                if (action.action == ActionType.ASK_USER) {
                    // Handle user prompt
                    _confirmationMessage.value = action.text ?: "Please confirm"
                    _needsConfirmation.value = true
                    taskManager.waitForConfirmation()
                    addTimelineEvent("Waiting for user confirmation: ${action.text}", EventType.PENDING)
                    // The confirmation is handled externally via confirmAction/denyAction
                    return
                }

                if (action.action == ActionType.GENERATE_TEXT) {
                    // Handle text generation
                    val postContent = generateSocialContent(action)
                    if (postContent != null) {
                        _generatedPost.value = postContent
                        taskManager.waitForConfirmation()
                        _needsConfirmation.value = true
                        _confirmationMessage.value = "Generated post ready for review"
                        addTimelineEvent("Generated social media post", EventType.SUCCESS)
                        return
                    }
                    continue
                }

                if (action.action == ActionType.COMPLETE) {
                    taskManager.completeTask(action.text ?: "Task completed")
                    addTimelineEvent("Task completed successfully!", EventType.SUCCESS)
                    _statusMessage.value = "Completed"
                    return
                }

                if (action.action == ActionType.STOP) {
                    taskManager.cancelTask()
                    addTimelineEvent("Agent stopped.", EventType.WARNING)
                    return
                }

                // Check if action needs confirmation in safe mode
                if (mode == AutomationMode.SAFE && needsConfirmationForAction(action)) {
                    taskManager.waitForConfirmation()
                    _needsConfirmation.value = true
                    _confirmationMessage.value = action.reason ?: "Confirm this action"
                    addTimelineEvent("Waiting for confirmation: ${action.reason}", EventType.PENDING)
                    return
                }

                // Execute the action
                addTimelineEvent("Executing: ${action.reason ?: action.action.name}", EventType.INFO)
                taskManager.updateState(com.moscool.agent.model.TaskState.EXECUTING)

                val result = executor.execute(action)
                taskManager.recordStepResult(action, result.success, result.message)
                memory.recordAction("${action.action.name}: ${action.reason}")

                if (result.success) {
                    addTimelineEvent("✓ ${result.message ?: action.action.name}", EventType.SUCCESS)
                } else {
                    addTimelineEvent("✗ ${result.message ?: "Action failed"}", EventType.ERROR)
                    // Don't fail immediately - let the AI decide if this is recoverable
                    if (index == plan.lastIndex) {
                        taskManager.failTask(result.message ?: "Action failed")
                        return
                    }
                }

                // Verify
                taskManager.updateState(com.moscool.agent.model.TaskState.VERIFYING)
                val verification = verifier.verify(action, result)
                if (!verification.verified) {
                    addTimelineEvent("Verification: ${verification.message}", EventType.WARNING)
                }
            }

            // If we get here, all steps completed
            taskManager.completeTask("All steps completed")
            addTimelineEvent("Task completed successfully!", EventType.SUCCESS)
            _statusMessage.value = "Completed"

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            taskManager.failTask("Error: ${e.message}")
            addTimelineEvent("Error: ${e.message}", EventType.ERROR)
            _statusMessage.value = "Failed"
            log("ERROR", "AgentEngine", "Command failed: ${e.message}")
        }
    }

    /**
     * Generate a plan from a user command.
     */
    private suspend fun generatePlan(command: String, mode: AutomationMode): List<AgentAction> {
        // First, use the AI to understand the command
        val screenSnapshot = try {
            accessibilityController.captureScreenSnapshot()
        } catch (_: Exception) {
            null
        }

        val screenInfo = screenSnapshot?.let { snap ->
            "Current app: ${snap.packageName}, Elements: ${snap.elements.size}"
        }

        val messages = PromptManager.buildInitialMessages(command, screenInfo)
        val response = aiProvider.chat(
            messages = messages,
            tools = ToolRegistry.getAIToolDefinitions(),
            temperature = 0.3
        )

        memory.addAssistantMessage(response.content)

        // If the AI returned tool calls, convert them to actions
        if (response.toolCalls.isNotEmpty()) {
            return response.toolCalls.map { tc ->
                val tool = ToolRegistry.getToolByName(tc.name)
                val args = tc.arguments
                AgentAction(
                    action = tool?.actionType ?: ActionType.WAIT,
                    target = com.moscool.agent.model.ActionTarget(
                        text = args["text"]?.toString()?.removeSurrounding("\""),
                        contentDescription = args["content_description"]?.toString()?.removeSurrounding("\""),
                        resourceId = args["resource_id"]?.toString()?.removeSurrounding("\""),
                        partialText = args["partial_text"]?.toString()?.removeSurrounding("\""),
                        packageName = args["package_name"]?.toString()?.removeSurrounding("\"")
                    ),
                    text = args["text"]?.toString()?.removeSurrounding("\""),
                    parameters = args.mapValues { it.value?.toString()?.removeSurrounding("\"") ?: "" },
                    reason = "AI planned: ${tool?.description ?: tc.name}"
                )
            }
        }

        // If the AI returned text, parse it
        return planner.parseActionsFromResponse(response.content)
    }

    /**
     * Generate social media content.
     */
    private suspend fun generateSocialContent(action: AgentAction): SocialPost? {
        val prompt = PromptManager.buildSocialPostPrompt(
            command = action.reason ?: "Generate social media post",
            topic = action.parameters["topic"] ?: action.text ?: "",
            business = action.parameters["business"],
            audience = action.parameters["audience"],
            tone = action.parameters["tone"]
        )

        val messages = listOf(
            ChatMessage(role = "system", content = PromptManager.getSystemPrompt()),
            ChatMessage(role = "user", content = prompt)
        )

        val response = aiProvider.chat(messages, temperature = 0.8)
        return parseSocialPost(response.content)
    }

    private fun parseSocialPost(text: String): SocialPost {
        var hook = ""
        var body = ""
        var cta = ""
        val hashtags = mutableListOf<String>()

        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("HOOK:", ignoreCase = true) -> {
                    hook = trimmed.removePrefix("HOOK:").trim()
                }
                trimmed.startsWith("BODY:", ignoreCase = true) -> {
                    body = trimmed.removePrefix("BODY:").trim()
                }
                trimmed.startsWith("CTA:", ignoreCase = true) -> {
                    cta = trimmed.removePrefix("CTA:").trim()
                }
                trimmed.startsWith("HASHTAGS:", ignoreCase = true) -> {
                    val tags = trimmed.removePrefix("HASHTAGS:").trim()
                    if (!tags.equals("none", ignoreCase = true)) {
                        hashtags.addAll(tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    }
                }
            }
        }

        // If parsing failed, use the whole text as the post
        if (hook.isEmpty() && body.isEmpty()) {
            return SocialPost.fromFullText(text)
        }

        val fullText = buildString {
            if (hook.isNotEmpty()) appendLine(hook)
            appendLine()
            if (body.isNotEmpty()) appendLine(body)
            appendLine()
            if (cta.isNotEmpty()) appendLine(cta)
            if (hashtags.isNotEmpty()) {
                appendLine()
                appendLine(hashtags.joinToString(" ") { "#$it" })
            }
        }.trim()

        return SocialPost(
            hook = hook,
            body = body,
            callToAction = cta,
            hashtags = hashtags,
            fullText = fullText
        )
    }

    /**
     * Confirm a waiting action.
     */
    fun confirmAction() {
        _needsConfirmation.value = false
    }

    /**
     * Deny a waiting action.
     */
    fun denyAction() {
        _needsConfirmation.value = false
        taskManager.cancelTask()
        addTimelineEvent("Action denied by user.", EventType.WARNING)
    }

    /**
     * Update the generated post after user edits.
     */
    fun updateGeneratedPost(post: SocialPost) {
        _generatedPost.value = post
    }

    /**
     * Stop the current agent task.
     */
    fun stopAgent() {
        isStopped = true
        currentJob?.cancel()
        taskManager.cancelTask()
        addTimelineEvent("Agent stopped by user.", EventType.WARNING)
        _statusMessage.value = "Stopped"
    }

    private fun needsConfirmationForAction(action: AgentAction): Boolean {
        return when (action.action) {
            ActionType.OPEN_APP -> false
            ActionType.TAP -> false
            ActionType.TYPE -> false
            ActionType.CLEAR_TEXT -> false
            ActionType.SCROLL -> false
            ActionType.SWIPE -> false
            ActionType.PRESS_BACK -> false
            ActionType.PRESS_HOME -> false
            ActionType.WAIT -> false
            ActionType.READ_SCREEN -> false
            ActionType.FIND_ELEMENT -> false
            ActionType.GENERATE_TEXT -> false
            ActionType.ASK_USER -> false
            ActionType.VERIFY -> false
            ActionType.COMPLETE -> false
            ActionType.STOP -> false
            ActionType.TAKE_SCREENSHOT -> true
            ActionType.CLOSE_APP -> true
            ActionType.LONG_PRESS -> true
        }
    }

    private fun addTimelineEvent(message: String, type: EventType) {
        _timelineEvents.value = _timelineEvents.value + TimelineEvent(message, type)
        _statusMessage.value = message
    }

    private suspend fun log(level: String, tag: String, message: String) {
        logRepository.log(level, tag, message)
    }
}
