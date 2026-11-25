package com.bluelabs.bluelauncher.data

import android.content.pm.ApplicationInfo

enum class AppCategory {
    ALL,
    FAVORITES,
    MEDIA,
    GAMES;

    fun filter(apps: List<LauncherApp>, favorites: Collection<String>): List<LauncherApp> = when (this) {
        ALL -> apps
        FAVORITES -> apps.filter { it.packageName in favorites }
        MEDIA -> apps.filter(::isMedia)
        GAMES -> apps.filter(::isGame)
    }

    private fun isMedia(app: LauncherApp): Boolean {
        if (app.category == ApplicationInfo.CATEGORY_AUDIO ||
            app.category == ApplicationInfo.CATEGORY_VIDEO ||
            app.category == ApplicationInfo.CATEGORY_IMAGE
        ) return true
        val text = "${app.packageName} ${app.label}".lowercase()
        return MEDIA_HINTS.any(text::contains)
    }

    private fun isGame(app: LauncherApp): Boolean {
        if (app.category == ApplicationInfo.CATEGORY_GAME) return true
        val text = "${app.packageName} ${app.label}".lowercase()
        return GAME_HINTS.any(text::contains)
    }

    companion object {
        private val MEDIA_HINTS = listOf(
            "video", "movies", "movie", "cinema", "stream", "music", "audio", "player",
            "netflix", "prime video", "youtube", "plex", "spotify", "tv", "podcast",
        )
        private val GAME_HINTS = listOf("game", "gaming", "steam", "xbox", "emulator", "retro")
    }
}
