package com.moscool.agent.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.moscool.agent.model.UIElementInfo
import com.moscool.agent.model.UISnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Main Accessibility Service that provides UI automation capabilities.
 * This is the core of the automation layer.
 */
class AccessibilityController : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityController"
        private var instance: AccessibilityController? = null

        fun getInstance(): AccessibilityController? = instance

        fun isServiceEnabled(): Boolean = instance != null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var foregroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                foregroundPackage = it.packageName?.toString()
            }
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ── Screen Snapshot ──

    fun captureScreenSnapshot(): UISnapshot {
        val rootNode = rootInActiveWindow ?: return UISnapshot()
        val elements = mutableListOf<UIElementInfo>()
        traverseNode(rootNode, elements, depth = 0)
        return UISnapshot(
            packageName = foregroundPackage,
            activityName = rootNode.className?.toString(),
            elements = elements
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo,
        elements: MutableList<UIElementInfo>,
        depth: Int
    ) {
        if (depth > 20) return // Prevent infinite recursion

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val info = UIElementInfo(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            resourceId = node.viewIdResourceName,
            className = node.className?.toString(),
            packageName = node.packageName?.toString(),
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            isEnabled = node.isEnabled,
            bounds = bounds,
            depth = depth,
            childCount = node.childCount
        )

        if (info.text != null || info.contentDescription != null || info.resourceId != null || info.isClickable || info.isEditable) {
            elements.add(info)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, elements, depth + 1)
            child.recycle()
        }
    }

    // ── Node Finding ──

    fun findNodeByText(text: String, exact: Boolean = true): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeByTextRecursive(rootNode, text, exact)
    }

    private fun findNodeByTextRecursive(
        node: AccessibilityNodeInfo,
        text: String,
        exact: Boolean
    ): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val match = if (exact) {
            nodeText.equals(text, ignoreCase = true)
        } else {
            nodeText.contains(text, ignoreCase = true)
        }

        if (match && (node.isClickable || node.isEditable || node.isVisibleToUser)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByTextRecursive(child, text, exact)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    fun findNodeByContentDescription(description: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeByDescRecursive(rootNode, description)
    }

    private fun findNodeByDescRecursive(
        node: AccessibilityNodeInfo,
        description: String
    ): AccessibilityNodeInfo? {
        val nodeDesc = node.contentDescription?.toString() ?: ""
        if (nodeDesc.equals(description, ignoreCase = true) && node.isClickable) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByDescRecursive(child, description)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    fun findNodeByResourceId(resourceId: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val results = rootNode.findAccessibilityNodeInfosByViewId(resourceId)
        return results.firstOrNull().also {
            results.forEach { node -> node.recycle() }
        }
    }

    fun findEditableNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findEditableRecursive(rootNode)
    }

    private fun findEditableRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableRecursive(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    fun findScrollableNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findScrollableRecursive(rootNode)
    }

    private fun findScrollableRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableRecursive(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    // ── Actions ──

    fun performActionOnNode(
        node: AccessibilityNodeInfo,
        action: Int,
        arguments: Bundle? = null
    ): Boolean {
        return try {
            node.performAction(action, arguments)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform action on node: ${e.message}")
            false
        }
    }

    fun executeGlobalAction(action: Int): Boolean {
        return try {
            super.performGlobalAction(action)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform global action: ${e.message}")
            false
        }
    }

    fun performSwipe(direction: String): com.moscool.agent.model.ActionResult {
        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels

        val startX: Float
        val startY: Float
        val endX: Float
        val endY: Float

        when (direction) {
            "up" -> {
                startX = width / 2f
                startY = height * 0.7f
                endX = width / 2f
                endY = height * 0.3f
            }
            "down" -> {
                startX = width / 2f
                startY = height * 0.3f
                endX = width / 2f
                endY = height * 0.7f
            }
            "left" -> {
                startX = width * 0.8f
                startY = height / 2f
                endX = width * 0.2f
                endY = height / 2f
            }
            "right" -> {
                startX = width * 0.2f
                startY = height / 2f
                endX = width * 0.8f
                endY = height / 2f
            }
            else -> return com.moscool.agent.model.ActionResult(
                success = false,
                message = "Unknown swipe direction: $direction"
            )
        }

        return performGesture(startX, startY, endX, endY, 300)
    }

    private fun performGesture(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long
    ): com.moscool.agent.model.ActionResult {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    durationMs
                )
            )
            .build()

        val completed = CompletableDeferred<Boolean>()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                completed.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                completed.complete(false)
            }
        }, null)

        return com.moscool.agent.model.ActionResult(
            success = true,
            message = "Gesture performed"
        )
    }

    fun pasteText(text: String): Boolean {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("moscool_input", text)
            clipboard.setPrimaryClip(clip)

            // Try to paste via action
            val editableNode = findEditableNode() ?: return false
            val pastePerformed = editableNode.performAction(
                AccessibilityNodeInfo.ACTION_PASTE
            )
            pastePerformed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to paste text: ${e.message}")
            false
        }
    }

    fun getForegroundPackage(): String? = foregroundPackage
}
