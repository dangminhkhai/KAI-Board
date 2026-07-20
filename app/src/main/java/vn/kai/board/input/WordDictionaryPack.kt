package vn.kai.board.input

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class WordDictionaryDownloadResult(val wordCount: Int, val bytes: Long)

enum class DictionaryLanguagePack(val code: String, val limit: Int, val fileName: String) {
    VIETNAMESE("vi", 40_000, "frequency_vi.tsv"),
    ENGLISH("en", 15_000, "frequency_en.tsv"),
}

/** Optional frequency dictionary. Download/build/load must only run on a worker thread. */
object WordDictionaryPack {
    private const val DIRECTORY = "language_models"
    private const val SOURCE_COMMIT = "525f9b560de45753a5ea01069454e72e9aa541c6"
    private const val MAX_DOWNLOAD_BYTES = 4L * 1024L * 1024L
    private val patterns = mapOf(
        DictionaryLanguagePack.VIETNAMESE to Regex("[\\p{L}Đđ]{2,32}"),
        DictionaryLanguagePack.ENGLISH to Regex("[A-Za-z][A-Za-z'-]{1,31}"),
    )

    fun isDownloaded(context: Context, pack: DictionaryLanguagePack): Boolean =
        packFile(context, pack).let { it.isFile && it.length() > 0L }
    fun sizeBytes(context: Context, pack: DictionaryLanguagePack): Long =
        packFile(context, pack).takeIf(File::isFile)?.length() ?: 0L

    fun load(context: Context): Int {
        val sources = buildList {
            add(context.assets.open("suggestions.tsv").reader(Charsets.UTF_8) to SuggestionLanguage.UNKNOWN)
            DictionaryLanguagePack.entries.forEach { pack ->
                packFile(context, pack).takeIf(File::isFile)?.let { file ->
                    val language = if (pack == DictionaryLanguagePack.VIETNAMESE) SuggestionLanguage.VIETNAMESE
                        else SuggestionLanguage.ENGLISH
                    add(file.reader(Charsets.UTF_8) to language)
                }
            }
        }
        return VietnameseSuggestionEngine.loadSources(sources)
    }

    fun download(context: Context, pack: DictionaryLanguagePack): WordDictionaryDownloadResult {
        val target = packFile(context, pack)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${pack.fileName}.tmp")
        try {
            val source = Source(pack.code, pack.limit, requireNotNull(patterns[pack]))
            val total: Int
            temporary.bufferedWriter(Charsets.UTF_8).use { writer ->
                total = downloadSource(source, writer)
            }
            check(total == pack.limit) { "Gói từ điển tải về không đủ dữ liệu" }
            check(temporary.length() in 1..MAX_DOWNLOAD_BYTES) { "Gói từ điển có kích thước không hợp lệ" }
            check(temporary.renameTo(target) || runCatching {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }.isSuccess) { "Không thể lưu gói từ điển" }
            load(context)
            return WordDictionaryDownloadResult(total, target.length())
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    fun delete(context: Context, pack: DictionaryLanguagePack) {
        packFile(context, pack).delete()
        load(context)
    }

    private fun downloadSource(source: Source, writer: java.io.Writer): Int {
        val url = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/$SOURCE_COMMIT/content/2016/${source.language}/${source.language}_50k.txt"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "text/plain")
        try {
            check(connection.responseCode == 200) { "Không thể tải từ điển ${source.language} (HTTP ${connection.responseCode})" }
            var accepted = 0
            connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (row in lines) {
                    if (accepted >= source.limit) break
                    val split = row.lastIndexOf(' ')
                    if (split <= 0) continue
                    val word = row.substring(0, split).trim()
                    val frequency = row.substring(split + 1).toLongOrNull() ?: continue
                    if (!source.pattern.matches(word) || frequency <= 0L) continue
                    writer.append(frequency.toString()).append('\t').append(word).append('\n')
                    accepted++
                }
            }
            check(accepted == source.limit) { "Từ điển ${source.language} chỉ có $accepted/${source.limit} từ hợp lệ" }
            return accepted
        } finally {
            connection.disconnect()
        }
    }

    private fun packFile(context: Context, pack: DictionaryLanguagePack) =
        File(File(context.filesDir, DIRECTORY), pack.fileName)
    private data class Source(val language: String, val limit: Int, val pattern: Regex)
}
