package vn.kai.board.input

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject
import vn.kai.board.settings.KeyboardPreferences
import java.io.File
import java.security.MessageDigest

enum class ClipboardEntryKind {
    TEXT, HTML, IMAGE;

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.name == value } ?: TEXT
    }
}

data class ClipboardEntry(
    val id: String,
    val kind: ClipboardEntryKind,
    /** Plain-text preview and default paste payload for TEXT/HTML. */
    val text: String,
    val html: String? = null,
    /** File name under filesDir/clipboard_images/ (not absolute path). */
    val imageFileName: String? = null,
    val mimeType: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val kindLabel: String
        get() = when (kind) {
            ClipboardEntryKind.IMAGE -> "ẢNH"
            ClipboardEntryKind.HTML -> "HTML"
            ClipboardEntryKind.TEXT -> ""
        }

    fun imageFile(context: Context): File? =
        imageFileName?.let { File(ClipboardHistoryStore.imagesDir(context), it) }?.takeIf { it.isFile }
}

object ClipboardHistoryStore {
    private const val FILE = "clipboard_history"
    private const val KEY = "text_items"
    private const val LIMIT = 12
    private const val IMAGE_DIR = "clipboard_images"
    private const val MAX_IMAGE_SIDE = 1280
    private const val JPEG_QUALITY = 85
    /**
     * Fingerprint of the last clip the user removed from history.
     * Prevents re-importing the same primary clip when opening the panel after delete/clear.
     */
    @Volatile
    private var suppressedClipFingerprint: String? = null

    fun imagesDir(context: Context): File =
        File(context.filesDir, IMAGE_DIR).also { if (!it.exists()) it.mkdirs() }

    fun add(context: Context, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        addEntry(
            context,
            ClipboardEntry(
                id = stableId("text", clean),
                kind = ClipboardEntryKind.TEXT,
                text = clean,
            ),
        )
    }

    /** Capture primary clip: image → HTML → plain text. */
    fun captureClip(context: Context, clip: ClipData) {
        if (clip.itemCount == 0) return
        val fingerprint = clipFingerprint(context, clip)
        if (fingerprint != null && fingerprint == suppressedClipFingerprint) {
            // User deleted this exact system clip from history — do not resurrect it.
            return
        }
        // A different clip supersedes the suppression.
        if (fingerprint != null && fingerprint != suppressedClipFingerprint) {
            suppressedClipFingerprint = null
        }
        val item = clip.getItemAt(0)
        val description = clip.description

        // 1) Image URI
        val uri = item.uri
        if (uri != null && looksLikeImage(context, uri, description?.getMimeType(0))) {
            addImageFromUri(context, uri)?.let { return }
        }
        // Some OEMs put image URI only in description extras; try all items.
        for (i in 0 until clip.itemCount) {
            val u = clip.getItemAt(i).uri ?: continue
            if (looksLikeImage(context, u, null)) {
                addImageFromUri(context, u)?.let { return }
            }
        }

        // 2) HTML rich text
        val html = item.htmlText?.trim().orEmpty()
        val plain = item.text?.toString()?.trim().orEmpty()
            .ifEmpty { item.coerceToText(context)?.toString()?.trim().orEmpty() }
        if (html.isNotEmpty() && html != plain && looksLikeHtml(html)) {
            val text = plain.ifEmpty { stripHtml(html) }
            if (text.isNotEmpty()) {
                addEntry(
                    context,
                    ClipboardEntry(
                        id = stableId("html", html),
                        kind = ClipboardEntryKind.HTML,
                        text = text,
                        html = html,
                    ),
                )
                return
            }
        }

        // 3) Plain text
        if (plain.isNotEmpty()) add(context, plain)
    }

    /** Remember current primary clip so history will not re-add it after user delete/clear. */
    fun suppressCurrentPrimaryClip(context: Context) {
        val clip = context.getSystemService(android.content.ClipboardManager::class.java)?.primaryClip
            ?: return
        suppressedClipFingerprint = clipFingerprint(context, clip)
    }

    private fun clipFingerprint(context: Context, clip: ClipData): String? {
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        item.uri?.toString()?.let { return "uri:$it" }
        val html = item.htmlText?.trim().orEmpty()
        if (html.isNotEmpty()) return "html:${stableId("html", html)}"
        val plain = item.text?.toString()?.trim().orEmpty()
            .ifEmpty { item.coerceToText(context)?.toString()?.trim().orEmpty() }
        if (plain.isNotEmpty()) return "text:${stableId("text", plain)}"
        return null
    }

    fun readEntries(context: Context): List<ClipboardEntry> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        var needsRewrite = false
        val entries = runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val value = array.get(index)
                if (value is JSONObject) {
                    parseEntry(value).also { if (!value.has("id") || !value.has("kind")) needsRewrite = true }
                } else {
                    needsRewrite = true
                    val text = value.toString()
                    ClipboardEntry(id = stableId("text", text), kind = ClipboardEntryKind.TEXT, text = text)
                }
            }
        }.getOrDefault(emptyList())

        val duration = KeyboardPreferences.clipboardExpiry(context).durationMillis
        val filtered = entries.filter { entry ->
            val alive = entry.pinned || duration == null ||
                System.currentTimeMillis() - entry.createdAt < duration
            val imageOk = entry.kind != ClipboardEntryKind.IMAGE || entry.imageFile(context) != null
            if (!alive || !imageOk) {
                if (entry.kind == ClipboardEntryKind.IMAGE) deleteImageFile(context, entry.imageFileName)
                false
            } else true
        }
        if (needsRewrite || filtered.size != entries.size) write(context, filtered)
        return filtered.sortedWith(compareByDescending<ClipboardEntry> { it.pinned }.thenByDescending { it.createdAt })
    }

    fun get(context: Context, id: String): ClipboardEntry? =
        readEntries(context).firstOrNull { it.id == id }

    fun togglePinned(context: Context, id: String) = update(context) { entries ->
        entries.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }
    }

    /** Legacy pin by text (old long-press path). */
    fun togglePinnedByText(context: Context, text: String) = update(context) { entries ->
        entries.map {
            if (it.kind != ClipboardEntryKind.IMAGE && it.text == text) it.copy(pinned = !it.pinned) else it
        }
    }

    fun delete(context: Context, id: String) {
        update(context) { entries ->
            entries.firstOrNull { it.id == id }?.let { deleteImageFile(context, it.imageFileName) }
            entries.filterNot { it.id == id }
        }
        suppressCurrentPrimaryClip(context)
    }

    /** Remove all non-pinned history items (and their image files). Pinned entries stay. */
    fun clearUnpinned(context: Context): Int {
        val entries = readEntries(context)
        val keep = entries.filter { it.pinned }
        val removed = entries.filterNot { it.pinned }
        removed.forEach { deleteImageFile(context, it.imageFileName) }
        write(context, keep)
        // Stop the still-held system clip from reappearing as a "new" history row.
        if (removed.isNotEmpty()) suppressCurrentPrimaryClip(context)
        return removed.size
    }

    fun decodeThumbnail(context: Context, entry: ClipboardEntry, maxPx: Int): Bitmap? {
        val file = entry.imageFile(context) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) sample *= 2
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private fun addImageFromUri(context: Context, uri: Uri): ClipboardEntry? {
        return runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: guessMime(uri) ?: "image/jpeg"
            if (!mime.startsWith("image/")) return null
            val raw = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (raw.isEmpty()) return null
            val digest = sha1(raw)
            val id = stableId("image", digest)
            // Already stored?
            readEntries(context).firstOrNull { it.id == id }?.let {
                addEntry(context, it.copy(createdAt = System.currentTimeMillis()))
                return it
            }
            val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
            val scaled = scaleDown(decoded, MAX_IMAGE_SIDE)
            if (scaled !== decoded) decoded.recycle()
            val fileName = "$id.jpg"
            val outFile = File(imagesDir(context), fileName)
            outFile.outputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            scaled.recycle()
            val entry = ClipboardEntry(
                id = id,
                kind = ClipboardEntryKind.IMAGE,
                text = "Ảnh",
                imageFileName = fileName,
                mimeType = "image/jpeg",
            )
            addEntry(context, entry)
            entry
        }.getOrNull()
    }

    private fun addEntry(context: Context, entry: ClipboardEntry) {
        val old = readEntries(context)
        val previous = old.firstOrNull { it.id == entry.id }
        val merged = entry.copy(
            pinned = previous?.pinned == true || entry.pinned,
            createdAt = System.currentTimeMillis(),
        )
        // Drop duplicate id; if replacing image keep one file.
        old.filter { it.id == entry.id && it.imageFileName != merged.imageFileName }
            .forEach { deleteImageFile(context, it.imageFileName) }
        val updated = listOf(merged) + old.filterNot { it.id == entry.id }
        // Cap image count to avoid filling storage (still within LIMIT total).
        val trimmed = updated.take(LIMIT)
        val removed = updated.drop(LIMIT)
        removed.forEach { deleteImageFile(context, it.imageFileName) }
        write(context, trimmed)
    }

    private fun update(context: Context, transform: (List<ClipboardEntry>) -> List<ClipboardEntry>) {
        write(context, transform(readEntries(context)))
    }

    private fun write(context: Context, entries: List<ClipboardEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("kind", entry.kind.name)
                    .put("text", entry.text)
                    .put("html", entry.html)
                    .put("imageFileName", entry.imageFileName)
                    .put("mimeType", entry.mimeType)
                    .put("pinned", entry.pinned)
                    .put("createdAt", entry.createdAt),
            )
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
        // Best-effort: delete orphaned image files.
        val keep = entries.mapNotNull { it.imageFileName }.toSet()
        imagesDir(context).listFiles()?.forEach { file ->
            if (file.name !in keep) file.delete()
        }
    }

    private fun parseEntry(obj: JSONObject): ClipboardEntry {
        val text = obj.optString("text")
        val kind = ClipboardEntryKind.fromStorage(obj.optString("kind").ifBlank { null })
        val id = obj.optString("id").ifBlank {
            when (kind) {
                ClipboardEntryKind.IMAGE -> stableId("image", obj.optString("imageFileName", text))
                ClipboardEntryKind.HTML -> stableId("html", obj.optString("html", text))
                ClipboardEntryKind.TEXT -> stableId("text", text)
            }
        }
        return ClipboardEntry(
            id = id,
            kind = kind,
            text = text,
            html = obj.optString("html").takeIf { it.isNotBlank() },
            imageFileName = obj.optString("imageFileName").takeIf { it.isNotBlank() },
            mimeType = obj.optString("mimeType").takeIf { it.isNotBlank() },
            pinned = obj.optBoolean("pinned"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        )
    }

    private fun looksLikeImage(context: Context, uri: Uri, mimeHint: String?): Boolean {
        val mime = mimeHint ?: context.contentResolver.getType(uri) ?: guessMime(uri)
        return mime?.startsWith("image/") == true
    }

    private fun guessMime(uri: Uri): String? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase()
        return if (ext.isNullOrBlank()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun looksLikeHtml(html: String): Boolean =
        html.contains('<') && html.contains('>') &&
            Regex("(?i)<(html|body|div|p|span|br|b|i|a|img|table)\\b").containsMatchIn(html)

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("&amp;", RegexOption.IGNORE_CASE), "&")
            .replace(Regex("&lt;", RegexOption.IGNORE_CASE), "<")
            .replace(Regex("&gt;", RegexOption.IGNORE_CASE), ">")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun scaleDown(source: Bitmap, maxSide: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = maxOf(w, h)
        if (longest <= maxSide) return source
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(source, (w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), true)
    }

    private fun deleteImageFile(context: Context, fileName: String?) {
        if (fileName.isNullOrBlank()) return
        File(imagesDir(context), fileName).delete()
    }

    private fun stableId(prefix: String, material: String): String {
        val digest = sha1(material.toByteArray())
        return "$prefix-${digest.take(16)}"
    }

    private fun sha1(bytes: ByteArray): String {
        val hex = MessageDigest.getInstance("SHA-1").digest(bytes)
        return hex.joinToString("") { "%02x".format(it) }
    }
}
