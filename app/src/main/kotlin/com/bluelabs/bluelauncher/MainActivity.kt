package com.bluelabs.bluelauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bluelabs.bluelauncher.data.AppCategory
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.ui.LauncherViews
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.viewmodel.LauncherViewModel

/** One activity, one short-lived receiver and no foreground/background service. */
class MainActivity : ComponentActivity(), LauncherViews.Callbacks {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: LauncherViewModel
    private lateinit var launcherViews: LauncherViews
    private var receiverRegistered = false

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importWallpaper(uri)
    }

    private val clockTick = object : Runnable {
        override fun run() {
            if (!isFinishing && ::launcherViews.isInitialized) launcherViews.updateClock()
            mainHandler.postDelayed(this, CLOCK_REFRESH_MS)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED -> scheduleAppRefresh()
            }
        }
    }

    private val refreshApps = Runnable {
        if (viewModel.dirtyApps && !isFinishing && !isDestroyed) {
            viewModel.loadApps()
        }
    }

    /** Coalesces the burst of package broadcasts an install or update produces. */
    private fun scheduleAppRefresh() {
        viewModel.markAppsDirty()
        mainHandler.removeCallbacks(refreshApps)
        mainHandler.postDelayed(refreshApps, PACKAGE_REFRESH_DELAY_MS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startNanos = System.nanoTime()
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        viewModel = LauncherViewModel(this)
        launcherViews = LauncherViews(
            this,
            Palette.fromWallpaper(viewModel.preferences.wallpaper),
            viewModel.preferences,
            this,
        )

        viewModel.onModelUpdated = { model, isFirstLoad ->
            mainHandler.post {
                if (!isFinishing && !isDestroyed) {
                    launcherViews.renderModel(model)
                    if (isFirstLoad && model.displayApps.isNotEmpty() && model.query.isEmpty()) {
                        launcherViews.root.post {
                            if (!isFinishing) launcherViews.focusFirstApp()
                        }
                    }
                }
            }
        }

        viewModel.onWallpaperUpdated = { wallpaper ->
            mainHandler.post {
                if (!isFinishing && !isDestroyed) {
                    launcherViews.updatePalette(Palette.fromWallpaper(wallpaper))
                }
            }
        }

        viewModel.onLaunchFailed = {
            mainHandler.post {
                if (!isFinishing && !isDestroyed) {
                    launcherViews.showLaunchError()
                }
            }
        }

        launcherViews.setDiagnostics { viewModel.getDiagnostics() }
        setContentView(launcherViews.root)
        launcherViews.root.requestFocus()

        viewModel.startupElapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        registerBackHandling()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.removeCallbacks(clockTick)
        mainHandler.post(clockTick)
        if (viewModel.dirtyApps) viewModel.loadApps()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(clockTick)
        mainHandler.removeCallbacks(refreshApps)
        if (::launcherViews.isInitialized) launcherViews.stopHeroPulse()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (viewModel.dirtyApps) viewModel.loadApps()
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        viewModel.dispose()
        if (::launcherViews.isInitialized) launcherViews.dispose()
        mainHandler.removeCallbacks(refreshApps)
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun registerBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                launcherViews.onBackPressed()
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> { openSettings(); return true }
            KeyEvent.KEYCODE_SEARCH -> { launcherViews.focusSearch(); return true }
            KeyEvent.KEYCODE_INFO -> { cycleCategory(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun launch(app: LauncherApp) {
        viewModel.launchApp(app)
    }

    override fun toggleFavorite(app: LauncherApp) {
        viewModel.toggleFavorite(app)
    }

    override fun reorderApp(app: LauncherApp, direction: Int) {
        viewModel.reorderApp(app, direction)
    }

    override fun openSettings() {
        launcherViews.showSettings(
            onWallpaper = { wallpaper ->
                viewModel.updateWallpaper(wallpaper)
            },
            onSections = { favorites, recent ->
                viewModel.updateSections(favorites, recent)
            },
            onClockSettings = { clock, date, clock24 ->
                viewModel.updateClockSettings(clock, date, clock24)
                launcherViews.updateClock()
            },
            onReset = {
                viewModel.resetLayout()
                launcherViews.updateClock()
            },
            onChooseImage = { wallpaperPicker.launch(arrayOf("image/*")) },
            onAbout = { launcherViews.showAbout() },
        )
    }

    override fun queryChanged(query: String) {
        viewModel.setQuery(query)
    }

    override fun selectCategory(category: AppCategory) {
        viewModel.setCategory(category)
    }

    override fun cycleCategory() {
        viewModel.cycleCategory()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(packageReceiver, filter)
        }
        receiverRegistered = true
    }

    companion object {
        private const val CLOCK_REFRESH_MS = 60_000L
        private const val PACKAGE_REFRESH_DELAY_MS = 700L
    }
}
