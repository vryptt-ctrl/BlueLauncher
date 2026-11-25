package com.bluelabs.bluelauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** Imports a background once, downsampled and RGB_565, to avoid full-resolution wallpaper heaps. */
object WallpaperStore {
    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1080

    fun import(context: Context, uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        val scaled = scaleToFit(bitmap)
        if (scaled !== bitmap) bitmap.recycle()
        val target = File(context.filesDir, wallpaperFileName(uri))
        FileOutputStream(target).use { output ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 86, output)) { "Wallpaper encoding failed" }
        }
        scaled.recycle()
        target.absolutePath
    }.getOrNull()

    fun load(context: Context, path: String, width: Int, height: Int): Bitmap? {
        if (path.isBlank() || !File(path).isFile) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight).coerceAtLeast(
                    sampleSize(width.coerceAtLeast(1), height.coerceAtLeast(1)),
                )
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(path, options)
        }.getOrNull()
    }

    private fun wallpaperFileName(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }
        return "wallpaper_$digest.jpg"
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= MAX_WIDTH && height / (sample * 2) >= MAX_HEIGHT) sample *= 2
        return sample
    }

    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_WIDTH && bitmap.height <= MAX_HEIGHT) return bitmap
        val ratio = minOf(MAX_WIDTH.toFloat() / bitmap.width, MAX_HEIGHT.toFloat() / bitmap.height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }
}
