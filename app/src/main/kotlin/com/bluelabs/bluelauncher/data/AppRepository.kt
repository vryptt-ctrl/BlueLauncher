package com.bluelabs.bluelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Reads only explicitly launchable TV/phone activities; avoids broad installed-app visibility. */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun loadInstalledApps(): List<LauncherApp> {
        val byPackage = LinkedHashMap<String, LauncherApp>()
        listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            queryActivities(intent).forEach activityLoop@{ info ->
                val activityInfo = info.activityInfo ?: return@activityLoop
                val packageName = activityInfo.packageName
                if (packageName == appContext.packageName || packageName in byPackage) return@activityLoop
                runCatching {
                    LauncherApp(
                        packageName = packageName,
                        label = info.loadLabel(packageManager).toString().trim(),
                        icon = activityInfo.loadIcon(packageManager),
                        category = if (Build.VERSION.SDK_INT >= 26) activityInfo.applicationInfo.category
                        else LauncherApp.CATEGORY_UNDEFINED,
                    )
                }.getOrNull()?.let { byPackage[packageName] = it }
            }
        }
        return byPackage.values.sortedBy { it.label.lowercase() }
    }

    fun launch(packageName: String): Boolean {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        return true
    }

    private fun queryActivities(intent: Intent) = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }
}
