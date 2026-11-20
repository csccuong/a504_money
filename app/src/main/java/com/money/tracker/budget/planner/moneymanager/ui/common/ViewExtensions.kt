package com.money.tracker.budget.planner.moneymanager.ui.common

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import java.text.NumberFormat
import java.util.Locale

fun View.show() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun Context.compatColor(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Attaches live US-locale thousand-separator formatting (e.g. 1,000,000) to an integer numeric EditText.
 */
fun EditText.attachThousandSeparator() {
    addTextChangedListener(object : TextWatcher {
        private var currentFormatted = ""
        private var isUpdating = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (isUpdating) return
            val raw = s?.toString().orEmpty()
            if (raw == currentFormatted) return

            isUpdating = true
            try {
                val cleaned = raw.replace(",", "")
                if (cleaned.isEmpty()) {
                    currentFormatted = ""
                    setText("")
                    return
                }

                val parsed = cleaned.toLongOrNull() ?: return
                val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)
                currentFormatted = formatted
                setText(formatted)
                setSelection(formatted.length)
            } finally {
                isUpdating = false
            }
        }
    })
}

fun EditText.cleanNumericText(): String = text?.toString().orEmpty().replace(",", "").trim()
