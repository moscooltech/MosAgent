package com.moscool.agent.agent

import com.moscool.agent.automation.AccessibilityController
import com.moscool.agent.model.ActionResult
import com.moscool.agent.model.ActionType
import com.moscool.agent.model.AgentAction

/**
 * Verifies that actions were completed successfully.
 */
class ActionVerifier(private val accessibilityController: AccessibilityController) {

    /**
     * Verify that a specific action was executed correctly.
     */
    suspend fun verify(action: AgentAction, result: ActionResult): VerificationResult {
        if (!result.success) {
            return VerificationResult(
                verified = false,
                message = "Action did not execute successfully: ${result.message}"
            )
        }

        return when (action.action) {
            ActionType.OPEN_APP -> verifyAppOpened(action, result)
            ActionType.TAP -> verifyTap(action, result)
            ActionType.TYPE -> verifyType(action, result)
            ActionType.SCROLL -> verifyScroll(action, result)
            ActionType.PRESS_BACK -> verifyBackPress(result)
            ActionType.READ_SCREEN -> verifyReadScreen(action, result)
            ActionType.COMPLETE -> VerificationResult(verified = true, message = "Task completed")
            ActionType.STOP -> VerificationResult(verified = true, message = "Agent stopped")
            else -> VerificationResult(verified = result.success, message = result.message ?: "Verified")
        }
    }

    private fun verifyAppOpened(action: AgentAction, result: ActionResult): VerificationResult {
        val snapshot = accessibilityController.captureScreenSnapshot()
        val expectedPackage = action.target?.packageName
        return if (expectedPackage != null && snapshot.packageName == expectedPackage) {
            VerificationResult(verified = true, message = "App opened: ${snapshot.packageName}")
        } else if (snapshot.packageName != "com.moscool.agent") {
            VerificationResult(verified = true, message = "App changed to: ${snapshot.packageName}")
        } else {
            VerificationResult(
                verified = false,
                message = "App may not have opened. Current: ${snapshot.packageName}"
            )
        }
    }

    private fun verifyTap(action: AgentAction, result: ActionResult): VerificationResult {
        // After a tap, the screen usually changes. Verify by checking if the element is gone
        // or the screen has changed.
        val snapshot = accessibilityController.captureScreenSnapshot()
        val targetText = action.target?.text
        return if (targetText != null) {
            // If the tapped element is no longer on screen, it likely navigated somewhere
            val elementStillVisible = snapshot.elements.any {
                it.text?.equals(targetText, ignoreCase = true) == true
            }
            if (elementStillVisible) {
                VerificationResult(verified = true, message = "Element tapped, still visible (may be loading)")
            } else {
                VerificationResult(verified = true, message = "Element tapped, screen changed")
            }
        } else {
            VerificationResult(verified = true, message = "Tap executed")
        }
    }

    private fun verifyType(action: AgentAction, result: ActionResult): VerificationResult {
        // After typing, try to find the text on screen
        val text = action.text ?: return VerificationResult(verified = true, message = "Type executed")
        val snapshot = accessibilityController.captureScreenSnapshot()
        val found = snapshot.elements.any { el ->
            el.text?.contains(text, ignoreCase = true) == true
        }
        return if (found) {
            VerificationResult(verified = true, message = "Text verified on screen")
        } else {
            VerificationResult(
                verified = false,
                message = "Typed text not found on screen. The field may not have accepted input."
            )
        }
    }

    private fun verifyScroll(action: AgentAction, result: ActionResult): VerificationResult {
        // Scroll is hard to verify statically; trust the result
        return VerificationResult(verified = true, message = "Scroll executed")
    }

    private fun verifyBackPress(result: ActionResult): VerificationResult {
        return VerificationResult(verified = true, message = "Back press executed")
    }

    private fun verifyReadScreen(action: AgentAction, result: ActionResult): VerificationResult {
        val snapshot = accessibilityController.captureScreenSnapshot()
        return if (snapshot.elements.isNotEmpty()) {
            VerificationResult(
                verified = true,
                message = "Screen read: ${snapshot.elements.size} elements found"
            )
        } else {
            VerificationResult(verified = false, message = "No elements found on screen")
        }
    }

    data class VerificationResult(
        val verified: Boolean,
        val message: String
    )
}
