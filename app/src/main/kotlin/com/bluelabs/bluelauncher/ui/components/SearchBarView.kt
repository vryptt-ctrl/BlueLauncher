package com.bluelabs.bluelauncher.ui.components

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
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

    val searchBox = EditText(context)
    private var isProgrammaticUpdate = false

    init {
        buildView()
    }

    private fun dp(v: Float) = TvUi.dp(context, v)

    private fun buildView() {
        searchBox.apply {
            hint = context.getString(R.string.search_hint)
            textSize = 15f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(20f), 0, dp(16f), 0)
            setHintTextColor(palette.muted)
            setTextColor(palette.text)
            background = TvUi.pillBackground(palette, 26f)
            TvUi.setSelectableBackground(this, { palette }, 26f, pill = true)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!isProgrammaticUpdate) {
                        callbacks.onQueryChanged(s?.toString().orEmpty())
                    }
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
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
        panel.addView(searchBox, FrameLayout.LayoutParams(dp(380f), dp(48f)))
        panel.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                callbacks.onDpadDown()
                true
            } else false
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
        }
    }

    fun updatePalette(newPalette: Palette) {
        this.palette = newPalette
        searchBox.setHintTextColor(palette.muted)
        searchBox.setTextColor(palette.text)
        searchBox.background = TvUi.pillBackground(palette, 26f)
    }
}
