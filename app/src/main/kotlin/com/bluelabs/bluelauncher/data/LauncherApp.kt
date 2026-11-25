package com.bluelabs.bluelauncher.data

import android.graphics.drawable.Drawable

data class LauncherApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val category: Int = CATEGORY_UNDEFINED,
) {
    companion object {
        const val CATEGORY_UNDEFINED = -1
    }
}
