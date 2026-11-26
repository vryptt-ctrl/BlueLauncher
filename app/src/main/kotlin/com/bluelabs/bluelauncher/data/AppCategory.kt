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
        // Only the package is inspected: labels like "TV Remote" or "TV Settings" are
        // launchers/utilities, not media apps.
        val packageName = app.packageName.lowercase()
        return MEDIA_HINTS.any(packageName::contains)
    }

    private fun isGame(app: LauncherApp): Boolean {
        if (app.category == ApplicationInfo.CATEGORY_GAME) return true
        val text = "${app.packageName} ${app.label}".lowercase()
        return GAME_HINTS.any(text::contains)
    }

    companion object {
        /**
         * Package-name fragments that reliably mean "TV/streaming app". Matching is done on
         * the package only, and the generic words the previous list used ("tv", "stream",
         * "player", "audio", "video", "music") are gone: they matched the system TV settings,
         * the remote and every preinstalled player, so almost everything landed in Media.
         * Entries must be space-free, because a package name never contains a space.
         */
        private val MEDIA_HINTS = listOf(
            "netflix", "youtube", "plex", "spotify", "primevideo", "amazonvideo", "disney",
            "hulu", "vlc", "kodi", "twitch", "deezer", "tidal", "podcast",
        )

        private val GAME_HINTS = listOf("game", "gaming", "steam", "xbox", "emulator", "retro")
    }
}
