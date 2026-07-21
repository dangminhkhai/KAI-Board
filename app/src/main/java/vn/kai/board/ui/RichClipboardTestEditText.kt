package vn.kai.board.ui

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.google.android.material.textfield.TextInputEditText

/**
 * Settings test field: accepts normal text + IME [commitContent] images for rich clipboard tests.
 */
class RichClipboardTestEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextInputEditText(context, attrs) {

    var onImageReceived: ((Uri) -> Unit)? = null

    init {
        ViewCompat.setOnReceiveContentListener(this, IMAGE_MIME_TYPES) { _, payload ->
            val split = payload.partition { item -> item.uri != null }
            var handled = false
            for (i in 0 until split.first.clip.itemCount) {
                val uri = split.first.clip.getItemAt(i).uri ?: continue
                onImageReceived?.invoke(uri)
                handled = true
            }
            if (handled) split.second else payload
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null
        EditorInfoCompat.setContentMimeTypes(outAttrs, IMAGE_MIME_TYPES)
        return InputConnectionCompat.createWrapper(base, outAttrs) { info, flags, _ ->
            acceptImageInfo(info, flags)
        }
    }

    private fun acceptImageInfo(info: InputContentInfoCompat, flags: Int): Boolean {
        val uri = info.contentUri
        val mime = runCatching { info.description.getMimeType(0) }.getOrNull().orEmpty()
        if (mime.isNotEmpty() && !mime.startsWith("image/") && mime != "*/*") return false
        runCatching { info.requestPermission() }
        if (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
            runCatching { info.requestPermission() }
        }
        onImageReceived?.invoke(uri)
        return true
    }

    companion object {
        val IMAGE_MIME_TYPES = arrayOf(
            "image/*",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif",
        )
    }
}
