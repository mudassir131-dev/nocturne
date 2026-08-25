/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

private val SupportedMimeTypes = arrayOf(
    "image/gif",
    "image/png",
    "image/jpeg",
    "image/webp",
    "image/*"
)

/**
 * Custom Android EditText that advertises GIF/Image MIME types to Gboard & system IMEs
 * and catches rich content commits (GIFs, stickers, memes from Google Keyboard).
 */
class GboardGifEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : EditText(context, attrs, defStyleAttr) {

    var onMediaCommitListener: ((Uri) -> Unit)? = null

    init {
        background = null
        isVerticalScrollBarEnabled = false

        // 1. Android 12+ / Modern ViewCompat Content Receiver
        ViewCompat.setOnReceiveContentListener(
            this,
            SupportedMimeTypes
        ) { _, payload ->
            val clip = payload.clip
            for (i in 0 until clip.itemCount) {
                val uri = clip.getItemAt(i).uri
                if (uri != null) {
                    onMediaCommitListener?.invoke(uri)
                    return@setOnReceiveContentListener null
                }
            }
            payload
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs) ?: return null

        // Explicitly declare supported rich MIME types for Gboard & SwiftKey
        EditorInfoCompat.setContentMimeTypes(outAttrs, SupportedMimeTypes)

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo: InputContentInfoCompat, flags: Int, _: Bundle? ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
            ) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false
                }
            }

            val uri = inputContentInfo.contentUri
            onMediaCommitListener?.invoke(uri)
            true
        }

        return InputConnectionCompat.createWrapper(ic, outAttrs, callback)
    }
}

/**
 * Jetpack Compose wrapper for GboardGifEditText with Material 3 styling
 */
@Composable
fun RichGifInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onMediaReceived: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnMediaReceived = rememberUpdatedState(onMediaReceived)
    val textColorArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColorArgb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        AndroidView<GboardGifEditText>(
            factory = { context ->
                GboardGifEditText(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    this.hint = placeholder
                    this.setHintTextColor(hintColorArgb)
                    this.setTextColor(textColorArgb)
                    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    this.maxLines = 4
                    this.isSingleLine = false

                    this.onMediaCommitListener = { uri ->
                        currentOnMediaReceived.value(uri)
                    }

                    this.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val newText = s?.toString() ?: ""
                            if (newText != value) {
                                currentOnValueChange.value(newText)
                            }
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }
            },
            update = { editText ->
                if (editText.text.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
                editText.setTextColor(textColorArgb)
                editText.setHintTextColor(hintColorArgb)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
