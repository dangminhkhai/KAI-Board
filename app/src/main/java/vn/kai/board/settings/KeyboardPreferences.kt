package vn.kai.board.settings

import android.content.Context
import android.content.SharedPreferences

object KeyboardPreferences {
    private const val FILE = "keyboard_preferences"
    const val HAPTIC = "haptic"
    const val HAPTIC_INTENSITY = "haptic_intensity"
    const val SOUND = "sound"
    const val SOUND_INTENSITY = "sound_intensity"
    const val POPUP = "popup"
    const val THEME = "theme"
    const val COLOR_STYLE = "color_style"
    const val HEIGHT_DP = "height_dp"
    const val NUMBER_ROW = "number_row"
    const val EXTENDED_SYMBOLS = "extended_symbols"
    const val LONG_PRESS_SYMBOLS = "long_press_symbols"
    const val ADJUSTMENT_MODE = "adjustment_mode"
    const val BOTTOM_OFFSET_DP = "bottom_offset_dp"
    const val WIDTH_PERCENT = "width_percent"
    const val LEFT_OFFSET_DP = "left_offset_dp"
    const val AUTO_CORRECT = "auto_correct"
    const val AUTO_CAPITALIZATION = "auto_capitalization"
    const val WORD_SUGGESTIONS = "word_suggestions"
    const val OFFLINE_MODE = "offline_mode"
    const val KEY_RADIUS_DP = "key_radius_dp"
    const val KEY_BORDER = "key_border"
    const val KEY_BORDER_WIDTH_DP = "key_border_width_dp"
    const val SMARTBAR_ORDER = "smartbar_order"
    const val THEME_EXTENSION_ID = "theme_extension_id"
    const val CLIPBOARD_EXPIRY = "clipboard_expiry"
    private val defaultSmartbarOrder = listOf("back", "mic", "translate", "ai", "clipboard", "settings", "emoji")

    fun haptic(context: Context) = prefs(context).getBoolean(HAPTIC, true)
    fun sound(context: Context) = prefs(context).getBoolean(SOUND, true)
    fun hapticLevel(context: Context) = prefs(context).getInt(HAPTIC_INTENSITY, 0).coerceIn(0, 100)
    fun soundLevel(context: Context) = prefs(context).getInt(SOUND_INTENSITY, 0).coerceIn(0, 100)
    fun hapticIntensity(context: Context): Int = when {
        !haptic(context) -> 0
        hapticLevel(context) == 0 -> -1
        else -> hapticLevel(context)
    }
    fun soundIntensity(context: Context): Int = when {
        !sound(context) -> 0
        soundLevel(context) == 0 -> -1
        else -> soundLevel(context)
    }
    fun popup(context: Context) = prefs(context).getBoolean(POPUP, true)
    fun theme(context: Context): ThemeMode {
        val preferences = prefs(context)
        if (preferences.contains("dark") && !preferences.contains(THEME)) {
            return if (preferences.getBoolean("dark", false)) ThemeMode.DARK else ThemeMode.LIGHT
        }
        return ThemeMode.fromStorage(preferences.getString(THEME, null))
    }
    fun colorStyle(context: Context) = KeyboardColorStyle.fromStorage(prefs(context).getString(COLOR_STYLE, null))
    fun heightDp(context: Context) = prefs(context).getInt(HEIGHT_DP, 220).coerceIn(170, 280)
    fun numberRow(context: Context) = prefs(context).getBoolean(NUMBER_ROW, false)
    fun extendedSymbols(context: Context) = prefs(context).getBoolean(EXTENDED_SYMBOLS, true)
    fun longPressSymbols(context: Context) = prefs(context).getBoolean(LONG_PRESS_SYMBOLS, false)
    fun adjustmentMode(context: Context) = prefs(context).getBoolean(ADJUSTMENT_MODE, false)
    fun bottomOffsetDp(context: Context) = prefs(context).getInt(BOTTOM_OFFSET_DP, 0).coerceIn(0, 80)
    fun widthPercent(context: Context) = prefs(context).getInt(WIDTH_PERCENT, 100).coerceIn(75, 100)
    fun leftOffsetDp(context: Context) = prefs(context).getInt(LEFT_OFFSET_DP, 0).coerceAtLeast(0)
    fun autoCorrect(context: Context) = prefs(context).getBoolean(AUTO_CORRECT, false)
    fun autoCapitalization(context: Context) = prefs(context).getBoolean(AUTO_CAPITALIZATION, true)
    fun wordSuggestions(context: Context) = prefs(context).getBoolean(WORD_SUGGESTIONS, true)
    fun offlineMode(context: Context) = prefs(context).getBoolean(OFFLINE_MODE, false)
    fun keyRadiusDp(context: Context) = prefs(context).getInt(KEY_RADIUS_DP, 7).coerceIn(0, 24)
    fun keyBorder(context: Context) = prefs(context).getBoolean(KEY_BORDER, false)
    fun keyBorderWidthDp(context: Context) = prefs(context).getInt(KEY_BORDER_WIDTH_DP, 1).coerceIn(1, 5)
    fun smartbarOrder(context: Context): List<String> {
        val stored = prefs(context).getString(SMARTBAR_ORDER, null)
            ?.split(',')?.filter { it in defaultSmartbarOrder }?.distinct().orEmpty()
        return stored + defaultSmartbarOrder.filterNot(stored::contains)
    }
    fun themeExtensionId(context: Context): String? = prefs(context).getString(THEME_EXTENSION_ID, null)
    fun clipboardExpiry(context: Context) = ClipboardExpiry.fromStorage(prefs(context).getString(CLIPBOARD_EXPIRY, null))

    fun setSmartbarOrder(context: Context, order: List<String>) =
        prefs(context).edit().putString(SMARTBAR_ORDER, order.joinToString(",")).apply()

    fun setBoolean(context: Context, key: String, value: Boolean) =
        prefs(context).edit().putBoolean(key, value).apply()

    fun setHeightDp(context: Context, value: Int) =
        prefs(context).edit().putInt(HEIGHT_DP, value.coerceIn(170, 280)).apply()

    fun setKeyRadiusDp(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_RADIUS_DP, value.coerceIn(0, 24)).apply()

    fun setKeyBorderWidthDp(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_BORDER_WIDTH_DP, value.coerceIn(1, 5)).apply()

    fun setIntensity(context: Context, key: String, value: Int) =
        prefs(context).edit().putInt(key, value.coerceIn(0, 100)).apply()

    fun setBottomOffsetDp(context: Context, value: Int) =
        prefs(context).edit().putInt(BOTTOM_OFFSET_DP, value.coerceIn(0, 80)).apply()

    fun setKeyboardGeometry(context: Context, heightDp: Int, bottomDp: Int, widthPercent: Int, leftDp: Int) =
        prefs(context).edit()
            .putInt(HEIGHT_DP, heightDp.coerceIn(170, 280))
            .putInt(BOTTOM_OFFSET_DP, bottomDp.coerceIn(0, 80))
            .putInt(WIDTH_PERCENT, widthPercent.coerceIn(75, 100))
            .putInt(LEFT_OFFSET_DP, leftDp.coerceAtLeast(0))
            .apply()

    fun setTheme(context: Context, value: ThemeMode) =
        prefs(context).edit().putString(THEME, value.storageValue).apply()

    fun setColorStyle(context: Context, value: KeyboardColorStyle) =
        prefs(context).edit().putString(COLOR_STYLE, value.storageValue).remove(THEME_EXTENSION_ID).apply()

    fun setThemeExtension(context: Context, id: String?) = prefs(context).edit().apply {
        if (id == null) remove(THEME_EXTENSION_ID) else putString(THEME_EXTENSION_ID, id)
    }.apply()

    fun setClipboardExpiry(context: Context, value: ClipboardExpiry) =
        prefs(context).edit().putString(CLIPBOARD_EXPIRY, value.storageValue).apply()

    fun register(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).registerOnSharedPreferenceChangeListener(listener)

    fun unregister(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

enum class ClipboardExpiry(val storageValue: String, val durationMillis: Long?) {
    NEVER("never", null), ONE_HOUR("one_hour", 60 * 60 * 1000L), ONE_DAY("one_day", 24 * 60 * 60 * 1000L);

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: ONE_DAY
    }
}

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

enum class KeyboardColorStyle(val storageValue: String) {
    CLASSIC("classic");

    companion object {
        fun fromStorage(value: String?): KeyboardColorStyle =
            entries.firstOrNull { it.storageValue == value } ?: CLASSIC
    }
}
