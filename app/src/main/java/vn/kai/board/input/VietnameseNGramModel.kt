package vn.kai.board.input

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class NGramDownloadResult(val entryCount: Int, val bytes: Long)

object VietnameseNGramModel {
    private const val DIRECTORY = "language_models"
    private const val FILE_NAME = "vi_vtb_ngram.tsv"
    private const val MAX_BIGRAMS = 3_000
    private const val MAX_TRIGRAMS = 3_000
    private const val SOURCE_COMMIT = "3c971fd1c5d03561e00b267d3d0057b81a9bea84"
    private val sourceFiles = listOf("train", "dev", "test").map { split ->
        "https://raw.githubusercontent.com/UniversalDependencies/UD_Vietnamese-VTB/$SOURCE_COMMIT/vi_vtb-ud-$split.conllu"
    }
    private val tokenRegex = Regex("[\\p{L}Đđ]{2,32}")

    @Volatile private var cachedEntries: List<Pair<List<String>, Int>>? = null

    fun isDownloaded(context: Context): Boolean = modelFile(context).let { it.isFile && it.length() > 0L }
    fun sizeBytes(context: Context): Long = modelFile(context).takeIf(File::isFile)?.length() ?: 0L

    fun entryCount(context: Context): Int = entries(context).size

    fun entries(context: Context): List<Pair<List<String>, Int>> {
        cachedEntries?.let { return it }
        val file = modelFile(context)
        if (!file.isFile) return emptyList()
        return runCatching {
            file.useLines { lines ->
                lines.mapNotNull { row ->
                    val fields = row.split('\t')
                    val count = fields.firstOrNull()?.toIntOrNull() ?: return@mapNotNull null
                    val words = fields.drop(1)
                    if (words.size !in 2..3 || words.any { clean(it) == null }) null else words to count
                }.toList()
            }
        }.getOrDefault(emptyList()).also { cachedEntries = it }
    }

    fun download(context: Context): NGramDownloadResult {
        val counts = HashMap<List<String>, Int>()
        sourceFiles.forEach { source ->
            val connection = URL(source).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "text/plain")
            try {
                check(connection.responseCode == 200) { "Không thể tải gói tiếng Việt (HTTP ${connection.responseCode})" }
                connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.filter { it.startsWith("# text = ") }.forEach { row ->
                        val words = tokenRegex.findAll(row.removePrefix("# text = "))
                            .map { it.value.lowercase(Locale.ROOT) }
                            .toList()
                        words.windowed(2).forEach { increment(counts, it) }
                        words.windowed(3).forEach { increment(counts, it) }
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
        val selected = counts.entries
            .filter { it.key.size == 2 }
            .sortedByDescending { it.value }
            .take(MAX_BIGRAMS) + counts.entries
            .filter { it.key.size == 3 }
            .sortedByDescending { it.value }
            .take(MAX_TRIGRAMS)
        check(selected.isNotEmpty()) { "Gói N-gram tải về không có dữ liệu hợp lệ" }

        val target = modelFile(context)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "$FILE_NAME.tmp")
        temporary.bufferedWriter(Charsets.UTF_8).use { writer ->
            selected.forEach { (words, count) ->
                writer.append(count.toString()).append('\t')
                    .append(words.joinToString("\t")).append('\n')
            }
        }
        check(temporary.renameTo(target) || runCatching {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }.isSuccess) { "Không thể lưu gói N-gram" }
        cachedEntries = selected.map { it.key to it.value }
        return NGramDownloadResult(selected.size, target.length())
    }

    fun delete(context: Context) {
        modelFile(context).delete()
        cachedEntries = null
    }

    private fun modelFile(context: Context) = File(File(context.filesDir, DIRECTORY), FILE_NAME)
    private fun clean(value: String): String? = value.trim().lowercase(Locale.ROOT)
        .takeIf { it.matches(tokenRegex) }
    private fun increment(values: MutableMap<List<String>, Int>, words: List<String>) {
        values[words] = (values[words] ?: 0) + 1
    }
}
