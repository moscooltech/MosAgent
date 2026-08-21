package com.moscool.agent.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Launches Android applications by package name.
 */
class AppLauncher(private val context: Context) {

    companion object {
        private const val TAG = "AppLauncher"

        private val KNOWN_PACKAGES = mapOf(
            "facebook" to "com.facebook.katana",
            "facebook lite" to "com.facebook.lite",
            "telegram" to "org.telegram.messenger",
            "instagram" to "com.instagram.android",
            "whatsapp" to "com.whatsapp",
            "whatsapp business" to "com.whatsapp.w4b",
            "youtube" to "com.google.android.youtube",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "gmail" to "com.google.android.gm",
            "chrome" to "com.android.chrome",
            "google maps" to "com.google.android.apps.maps",
            "linkedin" to "com.linkedin.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "spotify" to "com.spotify.music",
            "settings" to "com.android.settings"
        )
    }

    /**
     * Launch an app by package name or display name.
     */
    fun launch(packageNameOrName: String): Boolean {
        val packageName = resolvePackageName(packageNameOrName)

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "Launched: $packageName")
                true
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName: ${e.message}")
            false
        }
    }

    /**
     * Check if an app is installed.
     */
    fun isInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get the installed package name for a known app.
     */
    fun resolvePackageName(name: String): String {
        val lowerName = name.lowercase().trim()

        // Check known packages first
        KNOWN_PACKAGES[lowerName]?.let { return it }

        // If it looks like a package name already, return it
        if (lowerName.contains(".") && lowerName.count { it == '.' } >= 2) {
            return lowerName
        }

        // Try to find by label
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
            val match = resolveInfos.find {
                it.loadLabel(context.packageManager).toString().lowercase().contains(lowerName)
            }
            match?.activityInfo?.packageName ?: lowerName
        } catch (_: Exception) {
            lowerName
        }
    }
}
