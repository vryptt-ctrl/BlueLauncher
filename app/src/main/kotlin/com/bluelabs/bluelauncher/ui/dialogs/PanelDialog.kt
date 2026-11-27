package com.bluelabs.bluelauncher.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.ui.TvUi

/**
 * Custom in-theme dialog panel: gradient card, accent stroke, spotlight focus.
 * Follows the active [Palette].
 */
internal object PanelDialog {

    fun show(
        context: Context,
        palette: Palette,
        title: String,
        subtitle: String?,
        content: View,
        maxWidthDp: Float = 560f,
        dismissAction: ((Dialog) -> Unit)? = null,
    ): Dialog {
        val dp = { v: Float -> TvUi.dp(content, v) }

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(palette.surfaceRaised, palette.surface),
            ).apply {
                cornerRadius = dp(24f).toFloat()
                setStroke(dp(1.5f), palette.outline)
            }
            elevation = dp(16f).toFloat()
            setPadding(dp(24f), dp(20f), dp(24f), dp(18f))
        }

        panel.addView(TextView(context).apply {
            typeface = TvUi.interBold(context)
            text = title
            textSize = 19f
            setTextColor(palette.text)
        })

        if (subtitle != null) {
            panel.addView(
                TextView(context).apply {
                    typeface = TvUi.inter(context)
                    text = subtitle
                    textSize = 12.5f
                    setTextColor(palette.muted)
                },
                TvUi.params(-2, -2, top = dp(4f)),
            )
        }

        panel.addView(
            View(context).apply { setBackgroundColor((palette.outline and 0x00FFFFFF) or (0x80 shl 24)) },
            TvUi.params(-1, dp(1f), top = dp(12f), bottom = dp(10f)),
        )

        // Cap the scrollable body so long lists never push the Cancel button off-screen.
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val maxBodyHeight = (context.resources.displayMetrics.heightPixels * 0.62f).toInt()
        panel.addView(
            content,
            LinearLayout.LayoutParams(-1, minOf(content.measuredHeight, maxBodyHeight).coerceAtLeast(dp(80f))).apply {
                bottomMargin = dp(6f)
            },
        )

        val cancelButton = TextView(context).apply {
            typeface = TvUi.interBold(context)
            text = context.getString(android.R.string.cancel)
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(dp(22f), 0, dp(22f), 0)
            TvUi.setSelectableBackground(this, { palette }, 22f, pill = true)
        }
        panel.addView(
            cancelButton,
            LinearLayout.LayoutParams(-2, dp(40f)).apply {
                gravity = Gravity.END
                topMargin = dp(10f)
            },
        )

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.62f).toInt().coerceAtMost(dp(maxWidthDp)),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        // Panel scales up from 92% while the scrim fades in; symmetric fade-out on dismiss.
        panel.alpha = 0f
        panel.scaleX = 0.92f
        panel.scaleY = 0.92f
        panel.pivotY = dp(24f).toFloat()
        dialog.setOnShowListener {
            panel.animate().cancel()
            panel.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(190L)
                .setInterpolator(TvUi.focusInterpolator)
                .start()
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                true
            } else false
        }
        dialog.show()

        cancelButton.setOnClickListener {
            val finish: (Dialog) -> Unit = dismissAction ?: Dialog::dismiss
            panel.animate().cancel()
            panel.animate()
                .alpha(0f).scaleX(0.94f).scaleY(0.94f)
                .setDuration(130L)
                .setInterpolator(TvUi.interpolator)
                .withEndAction { finish(dialog) }
                .start()
        }
        cancelButton.requestFocus()
        return dialog
    }

    /** Dialog-wide list option row: check mark + label with a selectable focus card. */
    fun panelOption(
        context: Context,
        palette: Palette,
        label: String,
        selected: Boolean,
        action: () -> Unit,
    ): TextView {
        val dp = { v: Float -> TvUi.dp(context, v) }
        return TextView(context).apply {
            typeface = if (selected) TvUi.interBold(context) else TvUi.inter(context)
            text = if (selected) "✓   $label" else "     $label"
            textSize = 13.5f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), 0, dp(14f), 0)
            setTextColor(if (selected) palette.accentBright else palette.text)
            TvUi.setSelectableBackground(this, { palette }, 13f)
            setOnClickListener { action() }
        }
    }
}
