package com.bluelabs.bluelauncher.ui.components

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.ui.TvUi

internal class SearchBarView(
    private val context: Context,
    private var palette: Palette,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun onQueryChanged(query: String)
        fun onSearchSubmit()
        fun onDpadDown()
    }

    val panel = FrameLayout(context).apply {
        clipChildren = false
        clipToPadding = false
    }

    private val bar = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16f), 0, dp(10f), 0)
    }

    private val searchIcon = TextView(context)
    val searchBox = EditText(context)
    private val clearButton = TextView(context)
    private var isProgrammaticUpdate = false

    init {
        buildView()
    }

    private fun dp(v: Float) = TvUi.dp(context, v)

    private fun buildView() {
        searchIcon.apply {
            typeface = TvUi.icons(context)
            text = GLYPH_SEARCH.toString()
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(palette.muted)
        }
        bar.addView(searchIcon, TvUi.params(dp(24f), dp(24f), right = dp(10f)))

        searchBox.apply {
            typeface = TvUi.inter(context)
            hint = context.getString(R.string.search_hint)
            textSize = 14.5f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            background = null
            setPadding(0, 0, dp(8f), 0)
            setHintTextColor(palette.muted)
            setTextColor(palette.text)
            setOnFocusChangeListener { _, focused ->
                paintBar(focused)
                bar.animate().cancel()
                bar.animate()
                    .scaleX(if (focused) 1.02f else 1f)
                    .scaleY(if (focused) 1.02f else 1f)
                    .translationZ(if (focused) dp(6f).toFloat() else 0f)
                    .setDuration(if (focused) 150L else 120L)
                    .setInterpolator(if (focused) TvUi.focusInterpolator else TvUi.interpolator)
                    .start()
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val value = s?.toString().orEmpty()
                    updateClearVisibility(value.isNotEmpty())
                    if (!isProgrammaticUpdate) {
                        callbacks.onQueryChanged(value)
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            setOnEditorActionListener { _, _, _ ->
                callbacks.onSearchSubmit()
                true
            }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    callbacks.onDpadDown()
                    true
                } else false
            }
        }
        bar.addView(searchBox, LinearLayout.LayoutParams(0, -1, 1f))

        clearButton.apply {
            typeface = TvUi.interBold(context)
            text = "×"
            textSize = 16f
            gravity = Gravity.CENTER
            contentDescription = context.getString(R.string.clear_search)
            visibility = View.GONE
            TvUi.setSelectableBackground(this, { palette }, 16f, pill = true)
            setOnClickListener {
                clear()
                searchBox.requestFocus()
            }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    callbacks.onDpadDown()
                    true
                } else false
            }
        }
        bar.addView(clearButton, TvUi.params(dp(30f), dp(30f)))

        val barWidth = (context.resources.displayMetrics.widthPixels * 0.44f).toInt()
            .coerceIn(dp(340f), dp(460f))
        panel.addView(bar, FrameLayout.LayoutParams(barWidth, dp(48f)))
        paintBar(searchBox.isFocused)

        panel.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                callbacks.onDpadDown()
                true
            } else false
        }
    }

    private fun updateClearVisibility(hasText: Boolean) {
        clearButton.visibility = if (hasText) View.VISIBLE else View.GONE
    }

    private fun paintBar(focused: Boolean) {
        bar.background = TvUi.inputBackground(context, palette, 24f, focused)
        searchIcon.setTextColor(if (focused) palette.accentBright else palette.muted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bar.outlineAmbientShadowColor = palette.backgroundDeep
            bar.outlineSpotShadowColor = palette.glow
        }
    }

    fun focus() {
        searchBox.requestFocus()
        searchBox.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun clear(): Boolean {
        if (searchBox.text.isNotEmpty()) {
            searchBox.setText("")
            return true
        }
        return false
    }

    fun setQuerySilently(query: String) {
        if (searchBox.text.toString() != query) {
            isProgrammaticUpdate = true
            searchBox.setText(query)
            searchBox.setSelection(searchBox.text.length)
            isProgrammaticUpdate = false
            updateClearVisibility(query.isNotEmpty())
        }
    }

    fun updatePalette(newPalette: Palette) {
        this.palette = newPalette
        searchBox.setHintTextColor(palette.muted)
        searchBox.setTextColor(palette.text)
        clearButton.background = TvUi.pillBackground(context, palette, 16f, clearButton.hasFocus())
        clearButton.setTextColor(if (clearButton.hasFocus()) palette.onAccent else palette.text)
        paintBar(searchBox.isFocused)
    }

    companion object {
        private const val GLYPH_SEARCH = '\uEF7A'
    }
}
