package com.moscool.agent.social

import com.moscool.agent.model.SocialPost

/**
 * Abstract interface for social media platform adapters.
 * Each platform implements this to provide platform-specific automation.
 */
interface SocialPlatform {

    val platformName: String
    val packageName: String
    val isInstalled: Boolean

    /**
     * High-level: Create a post on this platform.
     */
    suspend fun createPost(post: SocialPost): Boolean

    /**
     * High-level: Prepare a post (fill in content) without publishing.
     */
    suspend fun preparePost(post: SocialPost): Boolean

    /**
     * Attach an image to the current post draft.
     */
    suspend fun attachImage(imagePath: String): Boolean

    /**
     * Publish the prepared post.
     */
    suspend fun publishPost(): Boolean

    /**
     * Verify that the post was published successfully.
     */
    suspend fun verifyPost(): Boolean

    /**
     * Get the platform's capabilities.
     */
    fun getCapabilities(): PlatformCapabilities
}

data class PlatformCapabilities(
    val supportsText: Boolean = true,
    val supportsImages: Boolean = false,
    val supportsVideos: Boolean = false,
    val supportsLinks: Boolean = false,
    val supportsPolls: Boolean = false,
    val supportsReactions: Boolean = false,
    val maxTextLength: Int = Int.MAX_VALUE,
    val supportsHashtags: Boolean = true,
    val supportsMentions: Boolean = true
)
