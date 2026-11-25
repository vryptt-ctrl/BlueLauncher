package com.bluelabs.bluelauncher.ui

import android.graphics.Color
import com.bluelabs.bluelauncher.data.LauncherPreferences

/** Color roles for every painted surface; palettes derive from the chosen wallpaper style. */
data class Palette(
    val background: Int,
    val backgroundDeep: Int,
    val surface: Int,
    val surfaceRaised: Int,
    val accent: Int,
    val accentBright: Int,
    val text: Int,
    val muted: Int,
    val outline: Int,
    val wallpaperStart: Int,
    val wallpaperEnd: Int,
    val glow: Int,
    val onAccent: Int,
) {
    companion object {
        fun fromWallpaper(wallpaper: String) = when (wallpaper) {
            LauncherPreferences.WALLPAPER_INK -> Palette(
                Color.rgb(10, 11, 17), Color.rgb(7, 8, 12), Color.rgb(23, 24, 34), Color.rgb(37, 39, 55),
                Color.rgb(154, 171, 255), Color.rgb(199, 209, 255), Color.rgb(244, 245, 251),
                Color.rgb(156, 160, 182), Color.rgb(58, 60, 78), Color.rgb(20, 21, 32), Color.rgb(10, 11, 16),
                Color.rgb(129, 140, 248), Color.rgb(10, 11, 16),
            )
            LauncherPreferences.WALLPAPER_OCEAN -> Palette(
                Color.rgb(4, 15, 22), Color.rgb(3, 10, 17), Color.rgb(11, 32, 44), Color.rgb(17, 45, 60),
                Color.rgb(103, 232, 249), Color.rgb(165, 243, 252), Color.rgb(236, 250, 253),
                Color.rgb(148, 180, 192), Color.rgb(32, 72, 90), Color.rgb(9, 32, 44), Color.rgb(4, 15, 22),
                Color.rgb(34, 211, 238), Color.rgb(4, 15, 22),
            )
            else -> Palette(
                Color.rgb(7, 11, 20), Color.rgb(5, 8, 15), Color.rgb(17, 25, 42), Color.rgb(28, 39, 63),
                Color.rgb(96, 165, 250), Color.rgb(147, 197, 253), Color.rgb(238, 242, 250),
                Color.rgb(148, 163, 190), Color.rgb(45, 58, 88), Color.rgb(12, 20, 38), Color.rgb(7, 11, 20),
                Color.rgb(59, 130, 246), Color.rgb(8, 13, 24),
            )
        }
    }
}
