package com.moscool.agent.social

import android.util.Log
import com.moscool.agent.automation.AccessibilityController
import com.moscool.agent.automation.AppLauncher
import com.moscool.agent.model.ActionType
import com.moscool.agent.model.AgentAction
import com.moscool.agent.model.SocialPost
import kotlinx.coroutines.delay

/**
 * Facebook automation adapter.
 * Uses the accessibility service to navigate Facebook and create posts.
 * Dynamically inspects the UI rather than hard-coding coordinates.
 */
class FacebookAdapter(
    private val accessibilityController: AccessibilityController,
    private val appLauncher: AppLauncher
) : SocialPlatform {

    companion object {
        private const val TAG = "FacebookAdapter"
        private const val FACEBOOK_PACKAGE = "com.facebook.katana"
    }

    override val platformName = "Facebook"
    override val packageName = FACEBOOK_PACKAGE
    override val isInstalled: Boolean
        get() = appLauncher.isInstalled(FACEBOOK_PACKAGE)

    override suspend fun createPost(post: SocialPost): Boolean {
        if (!isInstalled) {
            Log.w(TAG, "Facebook is not installed")
            return false
        }

        // Step 1: Open Facebook
        if (!appLauncher.launch(FACEBOOK_PACKAGE)) {
            Log.e(TAG, "Failed to launch Facebook")
            return false
        }
        delay(2000) // Wait for app to load

        // Step 2: Find and tap the "What's on your mind?" or post creation button
        val postButton = findPostCreationButton()
        if (postButton == null) {
            Log.e(TAG, "Could not find post creation button")
            return false
        }

        val tapped = accessibilityController.performActionOnNode(
            postButton,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
        )
        if (!tapped) {
            Log.e(TAG, "Failed to tap post creation button")
            return false
        }
        delay(2000) // Wait for post composer to open

        // Step 3: Find the text input and type the post
        val editableField = accessibilityController.findEditableNode()
        if (editableField == null) {
            Log.e(TAG, "Could not find text input field")
            return false
        }

        val bundle = android.os.Bundle()
        bundle.putCharSequence(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            post.fullText
        )
        val typed = accessibilityController.performActionOnNode(
            editableField,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
            bundle
        )
        delay(1000)

        if (!typed) {
            // Fallback: try clipboard paste
            accessibilityController.pasteText(post.fullText)
            delay(500)
        }

        Log.i(TAG, "Post content entered")
        return true
    }

    override suspend fun preparePost(post: SocialPost): Boolean {
        return createPost(post)
    }

    override suspend fun attachImage(imagePath: String): Boolean {
        // Look for photo/video button in the post composer
        val photoButton = accessibilityController.findNodeByContentDescription("Photo/video")
            ?: accessibilityController.findNodeByContentDescription("Photo")
            ?: accessibilityController.findNodeByText("Photo/video", exact = false)
            ?: accessibilityController.findNodeByText("Photo", exact = true)

        if (photoButton == null) {
            Log.w(TAG, "Could not find photo attachment button")
            return false
        }

        val tapped = accessibilityController.performActionOnNode(
            photoButton,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
        )
        delay(1500)
        return tapped
    }

    override suspend fun publishPost(): Boolean {
        // Find and tap the Post/Publish button
        val postButton = accessibilityController.findNodeByText("Post", exact = true)
            ?: accessibilityController.findNodeByText("Post", exact = false)
            ?: accessibilityController.findNodeByContentDescription("Post")

        if (postButton == null) {
            Log.e(TAG, "Could not find Post button")
            return false
        }

        val tapped = accessibilityController.performActionOnNode(
            postButton,
            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
        )
        delay(2000)
        return tapped
    }

    override suspend fun verifyPost(): Boolean {
        // After posting, check if we're back on the feed or if the post appears
        delay(2000)
        val snapshot = accessibilityController.captureScreenSnapshot()
        // If we're back on the feed, the post was likely published
        return snapshot.packageName == FACEBOOK_PACKAGE
    }

    override fun getCapabilities() = PlatformCapabilities(
        supportsText = true,
        supportsImages = true,
        supportsVideos = false,
        supportsLinks = true,
        supportsPolls = false,
        supportsReactions = false,
        maxTextLength = 63206,
        supportsHashtags = true,
        supportsMentions = true
    )

    private fun findPostCreationButton(): android.view.accessibility.AccessibilityNodeInfo? {
        // Try multiple selectors for the post creation entry point
        return accessibilityController.findNodeByText("What's on your mind", exact = false)
            ?: accessibilityController.findNodeByText("Create a post", exact = false)
            ?: accessibilityController.findNodeByContentDescription("Create a post")
            ?: accessibilityController.findNodeByContentDescription("Write something")
            ?: accessibilityController.findNodeByContentDescription("What's on your mind")
            ?: accessibilityController.findNodeByText("Photo/video", exact = false)
            ?: accessibilityController.findNodeByContentDescription("Create new post")
    }
}
