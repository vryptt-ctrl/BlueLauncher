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
    val glow: Int,
    val onAccent: Int,
) {
    companion object {
        fun fromWallpaper(wallpaper: String) = when (wallpaper) {
            LauncherPreferences.WALLPAPER_INK -> Palette(
                background = Color.rgb(10, 11, 17),
                backgroundDeep = Color.rgb(7, 8, 12),
                surface = Color.rgb(23, 24, 34),
                surfaceRaised = Color.rgb(37, 39, 55),
                accent = Color.rgb(154, 171, 255),
                accentBright = Color.rgb(199, 209, 255),
                text = Color.rgb(244, 245, 251),
                muted = Color.rgb(156, 160, 182),
                outline = Color.rgb(58, 60, 78),
                wallpaperStart = Color.rgb(20, 21, 32),
                glow = Color.rgb(129, 140, 248),
                onAccent = Color.rgb(10, 11, 16),
            )
            LauncherPreferences.WALLPAPER_OCEAN -> Palette(
                background = Color.rgb(4, 15, 22),
                backgroundDeep = Color.rgb(3, 10, 17),
                surface = Color.rgb(11, 32, 44),
                surfaceRaised = Color.rgb(17, 45, 60),
                accent = Color.rgb(103, 232, 249),
                accentBright = Color.rgb(165, 243, 252),
                text = Color.rgb(236, 250, 253),
                muted = Color.rgb(148, 180, 192),
                outline = Color.rgb(32, 72, 90),
                wallpaperStart = Color.rgb(9, 32, 44),
                glow = Color.rgb(34, 211, 238),
                onAccent = Color.rgb(4, 15, 22),
            )
            else -> Palette(
                background = Color.rgb(7, 11, 20),
                backgroundDeep = Color.rgb(5, 8, 15),
                surface = Color.rgb(17, 25, 42),
                surfaceRaised = Color.rgb(28, 39, 63),
                accent = Color.rgb(96, 165, 250),
                accentBright = Color.rgb(147, 197, 253),
                text = Color.rgb(238, 242, 250),
                muted = Color.rgb(148, 163, 190),
                outline = Color.rgb(45, 58, 88),
                wallpaperStart = Color.rgb(12, 20, 38),
                glow = Color.rgb(59, 130, 246),
                onAccent = Color.rgb(8, 13, 24),
            )
        }
    }
}
