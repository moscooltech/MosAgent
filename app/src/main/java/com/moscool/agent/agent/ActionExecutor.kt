package com.moscool.agent.agent

import com.moscool.agent.automation.AccessibilityController
import com.moscool.agent.automation.AppLauncher
import com.moscool.agent.model.ActionResult
import com.moscool.agent.model.ActionTarget
import com.moscool.agent.model.ActionType
import com.moscool.agent.model.AgentAction
import kotlinx.coroutines.delay

/**
 * Executes agent actions by delegating to the appropriate automation component.
 */
class ActionExecutor(
    private val accessibilityController: AccessibilityController,
    private val appLauncher: AppLauncher
) {

    suspend fun execute(action: AgentAction): ActionResult {
        return try {
            when (action.action) {
                ActionType.OPEN_APP -> executeOpenApp(action)
                ActionType.CLOSE_APP -> executeCloseApp(action)
                ActionType.TAP -> executeTap(action)
                ActionType.LONG_PRESS -> executeLongPress(action)
                ActionType.TYPE -> executeType(action)
                ActionType.CLEAR_TEXT -> executeClearText(action)
                ActionType.SCROLL -> executeScroll(action)
                ActionType.SWIPE -> executeSwipe(action)
                ActionType.PRESS_BACK -> executePressBack()
                ActionType.PRESS_HOME -> executePressHome()
                ActionType.WAIT -> executeWait(action)
                ActionType.READ_SCREEN -> executeReadScreen()
                ActionType.TAKE_SCREENSHOT -> executeScreenshot()
                ActionType.FIND_ELEMENT -> executeFindElement(action)
                ActionType.GENERATE_TEXT -> ActionResult(success = true, message = "Text generation handled by AI")
                ActionType.ASK_USER -> ActionResult(success = true, message = "User prompt handled by engine")
                ActionType.VERIFY -> executeVerify(action)
                ActionType.COMPLETE -> ActionResult(success = true, message = "Task completed")
                ActionType.STOP -> ActionResult(success = true, message = "Agent stopped")
            }
        } catch (e: Exception) {
            ActionResult(success = false, message = "Action failed: ${e.message}")
        }
    }

    private suspend fun executeOpenApp(action: AgentAction): ActionResult {
        val packageName = action.target?.packageName
            ?: action.target?.text?.let { resolvePackageName(it) }
            ?: return ActionResult(success = false, message = "No app specified")

        val launched = appLauncher.launch(packageName)
        return if (launched) {
            delay(1500) // Wait for app to launch
            ActionResult(success = true, message = "Opened $packageName")
        } else {
            ActionResult(success = false, message = "Could not open $packageName. Is it installed?")
        }
    }

    private suspend fun executeCloseApp(action: AgentAction): ActionResult {
        accessibilityController.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
        delay(500)
        return ActionResult(success = true, message = "App overview opened")
    }

    private suspend fun executeTap(action: AgentAction): ActionResult {
        val target = action.target
            ?: return ActionResult(success = false, message = "No tap target specified")

        val element = findBestElement(target)
            ?: return ActionResult(success = false, message = "Could not find element: ${target}")

        val tapped = accessibilityController.performActionOnNode(
            element,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
        )
        return if (tapped) {
            delay(500)
            ActionResult(success = true, message = "Tapped: ${target.text ?: target.contentDescription ?: target.resourceId}")
        } else {
            ActionResult(success = false, message = "Failed to tap element")
        }
    }

    private suspend fun executeLongPress(action: AgentAction): ActionResult {
        val target = action.target
            ?: return ActionResult(success = false, message = "No long-press target specified")

        val element = findBestElement(target)
            ?: return ActionResult(success = false, message = "Could not find element: ${target}")

        val longPressed = accessibilityController.performActionOnNode(
            element,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK
        )
        return if (longPressed) {
            delay(500)
            ActionResult(success = true, message = "Long-pressed element")
        } else {
            ActionResult(success = false, message = "Failed to long-press element")
        }
    }

    private suspend fun executeType(action: AgentAction): ActionResult {
        val text = action.text
            ?: return ActionResult(success = false, message = "No text specified to type")

        // First try to find an editable field and set text directly
        val editableNode = accessibilityController.findEditableNode()
        if (editableNode != null) {
            val bundle = android.os.Bundle()
            bundle.putCharSequence(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            val success = accessibilityController.performActionOnNode(
                editableNode,
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
                bundle
            )
            if (success) {
                delay(300)
                return ActionResult(success = true, message = "Typed: ${text.take(50)}...")
            }
        }

        // Fallback: use clipboard paste
        val pasteSuccess = accessibilityController.pasteText(text)
        delay(300)
        return if (pasteSuccess) {
            ActionResult(success = true, message = "Pasted text")
        } else {
            ActionResult(success = false, message = "Could not type text - no editable field found")
        }
    }

    private suspend fun executeClearText(action: AgentAction): ActionResult {
        val editableNode = accessibilityController.findEditableNode()
            ?: return ActionResult(success = false, message = "No editable field found")

        val bundle = android.os.Bundle()
        bundle.putCharSequence(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            ""
        )
        val success = accessibilityController.performActionOnNode(
            editableNode,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
            bundle
        )
        delay(200)
        return if (success) {
            ActionResult(success = true, message = "Cleared text")
        } else {
            ActionResult(success = false, message = "Failed to clear text")
        }
    }

    private suspend fun executeScroll(action: AgentAction): ActionResult {
        val direction = action.parameters["direction"] ?: "down"
        val scrollableNode = accessibilityController.findScrollableNode()
            ?: return ActionResult(success = false, message = "No scrollable element found")

        val scrollAction = when (direction) {
            "down" -> android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "up" -> android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        val success = accessibilityController.performActionOnNode(scrollableNode, scrollAction)
        delay(500)
        return if (success) {
            ActionResult(success = true, message = "Scrolled $direction")
        } else {
            ActionResult(success = false, message = "Failed to scroll")
        }
    }

    private suspend fun executeSwipe(action: AgentAction): ActionResult {
        val direction = action.parameters["direction"] ?: "up"
        val result = accessibilityController.performSwipe(direction)
        delay(500)
        return result
    }

    private suspend fun executePressBack(): ActionResult {
        accessibilityController.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        delay(500)
        return ActionResult(success = true, message = "Pressed back")
    }

    private suspend fun executePressHome(): ActionResult {
        accessibilityController.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
        delay(500)
        return ActionResult(success = true, message = "Pressed home")
    }

    private suspend fun executeWait(action: AgentAction): ActionResult {
        val duration = action.parameters["duration_ms"]?.toLongOrNull() ?: 2000
        delay(duration)
        return ActionResult(success = true, message = "Waited ${duration}ms")
    }

    private suspend fun executeReadScreen(): ActionResult {
        val snapshot = accessibilityController.captureScreenSnapshot()
        val elementCount = snapshot.elements.size
        val screenInfo = buildString {
            appendLine("Screen: ${snapshot.packageName}")
            appendLine("Activity: ${snapshot.activityName}")
            appendLine("Elements found: $elementCount")
            snapshot.elements.take(20).forEachIndexed { idx, el ->
                append("$idx: ")
                el.text?.let { append("text=\"$it\" ") }
                el.contentDescription?.let { append("desc=\"$it\" ") }
                el.resourceId?.let { append("id=$it ") }
                if (el.isClickable) append("[clickable] ")
                if (el.isEditable) append("[editable] ")
                if (el.isScrollable) append("[scrollable] ")
                appendLine()
            }
        }
        return ActionResult(success = true, message = screenInfo, uiSnapshot = snapshot)
    }

    private suspend fun executeScreenshot(): ActionResult {
        return ActionResult(success = false, message = "Screenshot requires MediaProjection permission (not yet implemented)")
    }

    private suspend fun executeFindElement(action: AgentAction): ActionResult {
        val target = action.target
            ?: return ActionResult(success = false, message = "No search criteria specified")

        val element = findBestElement(target)
        return if (element != null) {
            ActionResult(
                success = true,
                message = "Found: text=${element.text}, desc=${element.contentDescription}, id=${element.resourceId}"
            )
        } else {
            ActionResult(success = false, message = "Element not found")
        }
    }

    private suspend fun executeVerify(action: AgentAction): ActionResult {
        val expected = action.parameters["expected"] ?: "element_exists"
        val targetValue = action.parameters["target"] ?: ""

        return when (expected) {
            "text_exists" -> {
                val snapshot = accessibilityController.captureScreenSnapshot()
                val found = snapshot.elements.any { el ->
                    el.text?.contains(targetValue, ignoreCase = true) == true
                }
                if (found) {
                    ActionResult(success = true, message = "Verified: text '$targetValue' exists on screen")
                } else {
                    ActionResult(success = false, message = "Verification failed: text '$targetValue' not found on screen")
                }
            }
            "element_visible" -> {
                val snapshot = accessibilityController.captureScreenSnapshot()
                val found = snapshot.elements.any { el ->
                    el.text?.contains(targetValue, ignoreCase = true) == true ||
                    el.contentDescription?.contains(targetValue, ignoreCase = true) == true
                }
                if (found) {
                    ActionResult(success = true, message = "Verified: element '$targetValue' is visible")
                } else {
                    ActionResult(success = false, message = "Verification failed: element '$targetValue' not visible")
                }
            }
            else -> ActionResult(success = true, message = "Verification '$expected' completed")
        }
    }

    private fun findBestElement(target: ActionTarget): android.view.accessibility.AccessibilityNodeInfo? {
        // Priority: resource ID > content description > exact text > partial text > class
        target.resourceId?.let {
            val node = accessibilityController.findNodeByResourceId(it)
            if (node != null) return node
        }
        target.contentDescription?.let {
            val node = accessibilityController.findNodeByContentDescription(it)
            if (node != null) return node
        }
        target.text?.let {
            val node = accessibilityController.findNodeByText(it, exact = true)
            if (node != null) return node
        }
        target.partialText?.let {
            val node = accessibilityController.findNodeByText(it, exact = false)
            if (node != null) return node
        }
        return null
    }

    private fun resolvePackageName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("facebook") -> "com.facebook.katana"
            lower.contains("telegram") -> "org.telegram.messenger"
            lower.contains("instagram") -> "com.instagram.android"
            lower.contains("youtube") -> "com.google.android.youtube"
            lower.contains("whatsapp") -> "com.whatsapp"
            lower.contains("chrome") -> "com.android.chrome"
            lower.contains("gmail") -> "com.google.android.gm"
            lower.contains("twitter") || lower == "x" -> "com.twitter.android"
            else -> name
        }
    }
}
