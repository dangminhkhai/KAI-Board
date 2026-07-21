package vn.kai.board.input

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class PhrasePackInstallResult(val pairCount: Int, val bytes: Long, val source: String)

/**
 * Optional offline bigram pack built from OpenSubtitles VI co-occurrence, compound word lists,
 * and curated collocations (see tools/build_phrase_pack.py).
 *
 * Ranking: [PhraseLearningStore] personal (21-day decay) is always first; this pack is the
 * offline layer. There is no APK seed catalog.
 *
 * Load/parse runs on a worker via [warmUpAsync] (IME start / after install).
 */
object PhrasePack {
    private const val DIRECTORY = "phrase_packs"
    const val FILE_NAME = "vi_social.tsv"
    private const val META_NAME = "vi_social.meta"
    private const val ASSET_PATH = "phrase_packs/vi_social.tsv"
    /** Versioned raw URL on the project repo (fallback install uses bundled asset). */
    const val DOWNLOAD_URL =
        "https://raw.githubusercontent.com/dangminhkhai/KAI-Board/main/phrase-packs/vi_social.tsv"
    /** SHA-256 of the shipped pack body (UTF-8, LF). Update when regenerating the TSV. */
    const val EXPECTED_SHA256 = "5470326a46a5fb17cea6861c8e47031fb9cd2e1263c05bb942e845e16140b476"
    private const val MIN_PAIRS = 500
    /** Large packs (5–20 MB) are fine; typing stays light after [warmUpAsync] fills RAM. */
    private const val MAX_PAIRS = 1_200_000
    private const val MAX_DOWNLOAD_BYTES = 24L * 1024L * 1024L
    private val wordPattern = Regex("[\\p{L}Đđ]{1,32}")

    private val cache = AtomicReference<Map<String, List<String>>?>(null)
    private val warming = AtomicBoolean(false)

    fun isInstalled(context: Context): Boolean =
        packFile(context).let { it.isFile && it.length() > 0L }

    fun sizeBytes(context: Context): Long =
        packFile(context).takeIf(File::isFile)?.length() ?: 0L

    /** Prefer meta / cache so settings UI does not parse 200k lines on the main thread. */
    fun pairCount(context: Context): Int {
        cache.get()?.let { return it.values.sumOf { rights -> rights.size } }
        readMetaCount(context)?.let { return it }
        return 0
    }

    fun installFromAssets(context: Context): PhrasePackInstallResult {
        val bytes = context.assets.open(ASSET_PATH).use { it.readBytes() }
        return writeValidated(context, bytes, source = "asset")
    }

    /**
     * Prefer HTTPS download with SHA-256 check; on network failure, install bundled asset
     * so debug builds remain usable without a published host.
     */
    fun download(context: Context, allowAssetFallback: Boolean = true): PhrasePackInstallResult {
        return try {
            val bytes = fetchUrl(DOWNLOAD_URL)
            writeValidated(context, bytes, source = "https")
        } catch (error: Throwable) {
            if (!allowAssetFallback) throw error
            installFromAssets(context)
        }
    }

    fun delete(context: Context) {
        packFile(context).delete()
        metaFile(context).delete()
        cache.set(null)
    }

    fun continuations(context: Context, left: String, limit: Int = 3): List<String> {
        if (limit <= 0) return emptyList()
        val key = left.trim().lowercase(Locale.ROOT)
        if (key.isEmpty()) return emptyList()
        // Never parse the multi-MB TSV on the typing path: only use warm RAM cache.
        // If still cold, kick async warm-up and return empty this frame (personal still works).
        val cached = cache.get()
        if (cached == null) {
            warmUpAsync(context)
            return emptyList()
        }
        return cached[key].orEmpty().take(limit)
    }

    /**
     * Parse pack into memory if installed and not yet cached. Safe to call repeatedly.
     * Prefer [warmUpAsync] from UI / IME threads.
     */
    fun warmUp(context: Context) {
        if (!isInstalled(context)) return
        if (cache.get() != null) return
        map(context)
    }

    /** One-shot background warm-up (no wake lock, no periodic work). */
    fun warmUpAsync(context: Context) {
        if (!isInstalled(context)) return
        if (cache.get() != null) return
        if (!warming.compareAndSet(false, true)) return
        val app = context.applicationContext
        Thread({
            try {
                runCatching { warmUp(app) }
            } finally {
                warming.set(false)
            }
        }, "kai-phrase-pack-warmup").start()
    }

    fun invalidateCache() {
        cache.set(null)
    }

    /** Pure parser for unit tests (no Android Context). */
    fun parseTsv(text: String): Map<String, List<String>> {
        val ordered = LinkedHashMap<String, MutableList<String>>()
        var accepted = 0
        text.lineSequence().forEach { raw ->
            if (accepted >= MAX_PAIRS) return@forEach
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split('\t', ' ').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 2) return@forEach
            val left = parts[0].lowercase(Locale.ROOT)
            val right = parts[1]
            if (!wordPattern.matches(left) || !wordPattern.matches(right)) return@forEach
            val bucket = ordered.getOrPut(left) { mutableListOf() }
            val rightKey = right.lowercase(Locale.ROOT)
            if (bucket.none { it.equals(rightKey, ignoreCase = true) }) {
                bucket += right
                accepted++
            }
        }
        return ordered.mapValues { it.value.toList() }
    }

    private fun map(context: Context): Map<String, List<String>> {
        cache.get()?.let { return it }
        val file = packFile(context)
        if (!file.isFile || file.length() == 0L) {
            cache.set(emptyMap())
            return emptyMap()
        }
        val parsed = parseTsv(file.readText(Charsets.UTF_8))
        cache.set(parsed)
        writeMeta(context, parsed.values.sumOf { it.size }, file.length())
        return parsed
    }

    private fun writeValidated(context: Context, bytes: ByteArray, source: String): PhrasePackInstallResult {
        check(bytes.isNotEmpty() && bytes.size <= MAX_DOWNLOAD_BYTES) {
            "Gói cụm từ có kích thước không hợp lệ"
        }
        val digest = sha256Hex(bytes)
        check(digest.equals(EXPECTED_SHA256, ignoreCase = true)) {
            "Gói cụm từ không khớp chữ ký SHA-256"
        }
        val text = bytes.toString(Charsets.UTF_8)
        val parsed = parseTsv(text)
        val count = parsed.values.sumOf { it.size }
        check(count in MIN_PAIRS..MAX_PAIRS) {
            "Gói cụm từ có số cặp không hợp lệ ($count)"
        }
        val target = packFile(context)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "$FILE_NAME.tmp")
        try {
            temporary.writeBytes(bytes)
            check(
                temporary.renameTo(target) || runCatching {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }.isSuccess,
            ) { "Không thể lưu gói cụm từ" }
            cache.set(parsed)
            writeMeta(context, count, target.length())
            return PhrasePackInstallResult(count, target.length(), source)
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    private fun fetchUrl(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "text/plain")
        try {
            check(connection.responseCode == 200) {
                "Không thể tải gói cụm từ (HTTP ${connection.responseCode})"
            }
            val bytes = connection.inputStream.use { it.readBytes() }
            check(bytes.size in 1..MAX_DOWNLOAD_BYTES.toInt()) {
                "Gói cụm từ tải về có kích thước không hợp lệ"
            }
            return bytes
        } finally {
            connection.disconnect()
        }
    }

    private fun packFile(context: Context): File =
        File(File(context.filesDir, DIRECTORY), FILE_NAME)

    private fun metaFile(context: Context): File =
        File(File(context.filesDir, DIRECTORY), META_NAME)

    private fun writeMeta(context: Context, count: Int, bytes: Long) {
        runCatching {
            metaFile(context).writeText("$count\n$bytes\n", Charsets.UTF_8)
        }
    }

    private fun readMetaCount(context: Context): Int? {
        val file = metaFile(context)
        if (!file.isFile) return null
        val first = file.readText(Charsets.UTF_8).lineSequence().firstOrNull()?.trim().orEmpty()
        return first.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
