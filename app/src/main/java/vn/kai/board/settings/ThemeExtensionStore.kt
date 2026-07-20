package vn.kai.board.settings

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class ThemeExtension(val id: String, val name: String, val author: String)

object ThemeExtensionStore {
    private const val DIRECTORY = "theme_extensions"
    private const val MAX_BYTES = 64 * 1024
    private val safeId = Regex("[a-z0-9][a-z0-9._-]{1,47}")

    fun installed(context: Context): List<ThemeExtension> = directory(context).listFiles()
        .orEmpty().filter { it.extension == "json" }.mapNotNull { file ->
            runCatching { metadata(JSONObject(file.readText())) }.getOrNull()
        }.sortedBy { it.name.lowercase() }

    fun installFromUrl(context: Context, source: String): ThemeExtension {
        val url = URL(source.trim())
        require(url.protocol == "https") { "Theme chỉ được tải qua HTTPS" }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000; readTimeout = 8_000
            instanceFollowRedirects = false
        }
        return connection.inputStream.use { install(context, it) }
    }

    fun install(context: Context, input: InputStream): ThemeExtension {
        val bytes = input.use {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BYTES) { "Gói theme vượt quá 64 KB" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val extension = validate(json)
        val target = File(directory(context), "${extension.id}.json")
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(bytes)
        require(temporary.renameTo(target) || run { temporary.copyTo(target, overwrite = true); temporary.delete(); true })
        return extension
    }

    fun palette(context: Context, id: String, dark: Boolean): KeyboardThemePalette? = runCatching {
        require(safeId.matches(id))
        val json = JSONObject(File(directory(context), "$id.json").readText())
        validate(json)
        parsePalette(json.getJSONObject(if (dark && json.has("dark")) "dark" else "light"))
    }.getOrNull()

    fun delete(context: Context, id: String) {
        if (!safeId.matches(id)) return
        File(directory(context), "$id.json").delete()
        if (KeyboardPreferences.themeExtensionId(context) == id) KeyboardPreferences.setThemeExtension(context, null)
    }

    fun exportPackages(context: Context): JSONArray = JSONArray().apply {
        directory(context).listFiles().orEmpty().filter { it.extension == "json" }.forEach { file ->
            runCatching { JSONObject(file.readText()).also(::validate) }.getOrNull()?.let(::put)
        }
    }

    fun importPackages(context: Context, packages: JSONArray) {
        require(packages.length() <= 32) { "Bản sao lưu chứa quá nhiều theme" }
        for (index in 0 until packages.length()) {
            packages.getJSONObject(index).toString().byteInputStream().use { install(context, it) }
        }
    }

    fun exportBytes(context: Context, id: String): ByteArray {
        require(safeId.matches(id)) { "ID theme không hợp lệ" }
        val file = File(directory(context), "$id.json")
        require(file.isFile && file.length() <= MAX_BYTES) { "Không tìm thấy theme" }
        return file.readBytes()
    }

    fun fileForSharing(context: Context, id: String): File {
        exportBytes(context, id)
        return File(directory(context), "$id.json")
    }

    private fun validate(json: JSONObject): ThemeExtension {
        require(json.optInt("schemaVersion") == 1) { "Phiên bản gói theme không được hỗ trợ" }
        val extension = metadata(json)
        require(safeId.matches(extension.id)) { "ID theme không hợp lệ" }
        parsePalette(json.getJSONObject("light"))
        if (json.has("dark")) parsePalette(json.getJSONObject("dark"))
        return extension
    }

    private fun metadata(json: JSONObject) = ThemeExtension(
        id = json.getString("id"),
        name = json.getString("name").take(60),
        author = json.optString("author", "Không rõ").take(60),
    )

    private fun parsePalette(value: JSONObject): KeyboardThemePalette {
        fun color(name: String) = parseColor(value.getString(name))
        val font = value.optString("fontFamily", "sans-serif")
            .takeIf { it in setOf("sans-serif", "sans-serif-rounded", "serif", "monospace") }
            ?: "sans-serif"
        val gradient = value.optJSONArray("gradient")?.let { array ->
            IntArray(array.length().coerceAtMost(4)) { parseColor(array.getString(it)) }.takeIf { it.size >= 2 }
        }
        return KeyboardThemePalette(
            background = color("background"), key = color("key"), specialKey = color("specialKey"),
            pressed = color("pressed"), text = color("text"), hint = color("hint"), accent = color("accent"),
            actionKey = value.optString("actionKey").takeIf(String::isNotBlank)?.let(::parseColor),
            gradientColors = gradient, fontFamily = font,
        )
    }

    private fun parseColor(raw: String): Int {
        val hex = raw.removePrefix("#")
        require(hex.length == 6 || hex.length == 8) { "Màu theme không hợp lệ" }
        return (if (hex.length == 6) "FF$hex" else hex).toLong(16).toInt()
    }

    private fun directory(context: Context) = File(context.filesDir, DIRECTORY).apply { mkdirs() }
}
