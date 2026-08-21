package com.moscool.agent.social

import com.moscool.agent.automation.AppLauncher
import com.moscool.agent.model.SocialPost

/**
 * Telegram adapter — stub for future full implementation.
 * Provides capability metadata so the architecture supports Telegram.
 */
class TelegramAdapter(private val appLauncher: AppLauncher) : SocialPlatform {

    override val platformName = "Telegram"
    override val packageName = "org.telegram.messenger"
    override val isInstalled: Boolean
        get() = appLauncher.isInstalled(packageName)

    override suspend fun createPost(post: SocialPost): Boolean {
        // TODO: Implement Telegram message sending
        return false
    }

    override suspend fun preparePost(post: SocialPost): Boolean {
        // TODO: Implement Telegram message preparation
        return false
    }

    override suspend fun attachImage(imagePath: String): Boolean {
        return false
    }

    override suspend fun publishPost(): Boolean {
        return false
    }

    override suspend fun verifyPost(): Boolean {
        return false
    }

    override fun getCapabilities() = PlatformCapabilities(
        supportsText = true,
        supportsImages = true,
        supportsVideos = true,
        supportsLinks = true,
        supportsPolls = true,
        supportsReactions = false,
        maxTextLength = 4096,
        supportsHashtags = false,
        supportsMentions = true
    )
}
