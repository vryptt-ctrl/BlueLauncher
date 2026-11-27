package com.bluelabs.bluelauncher.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.bluelabs.bluelauncher.BuildConfig
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.ui.TvUi

internal object AboutDialog {

    fun show(
        context: Context,
        palette: Palette,
        diagnosticsProvider: (() -> String)?,
    ): Dialog {
        val dp = { v: Float -> TvUi.dp(context, v) }
        val message = TextView(context).apply {
            typeface = TvUi.inter(context)
            text = context.getString(R.string.about_message, diagnosticsProvider?.invoke().orEmpty())
            textSize = 13f
            setTextColor(palette.text)
            setLineSpacing(dp(3f).toFloat(), 1f)
        }
        return PanelDialog.show(
            context = context,
            palette = palette,
            title = context.getString(R.string.about_title, BuildConfig.VERSION_NAME),
            subtitle = null,
            content = message,
            dismissAction = { dialog -> dialog.dismiss() },
        )
    }
}
