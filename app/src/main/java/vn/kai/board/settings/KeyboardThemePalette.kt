package vn.kai.board.settings

import android.content.Context

data class KeyboardThemePalette(
    val background: Int,
    val key: Int,
    val specialKey: Int,
    val pressed: Int,
    val text: Int,
    val hint: Int,
    val accent: Int,
    val actionKey: Int? = null,
    val gradientColors: IntArray? = null,
    val fontFamily: String = "sans-serif",
) {
    companion object {
        fun resolve(context: Context, dark: Boolean): KeyboardThemePalette {
            val extension = KeyboardPreferences.themeExtensionId(context)
                ?.let { ThemeExtensionStore.palette(context, it, dark) }
            return extension ?: resolve(KeyboardPreferences.colorStyle(context), dark)
        }

        fun resolve(preset: KeyboardColorStyle, dark: Boolean): KeyboardThemePalette =
            when (preset) {
                KeyboardColorStyle.CLASSIC -> if (dark) mintDark() else mintLight()
            }

        private fun mintLight() = KeyboardThemePalette(
            background = Color.rgb(226, 226, 226),
            key = Color.WHITE,
            specialKey = Color.WHITE,
            pressed = Color.rgb(207, 207, 207),
            text = Color.rgb(20, 20, 20),
            hint = Color.rgb(95, 99, 104),
            accent = Color.rgb(76, 175, 80),
            actionKey = Color.rgb(76, 175, 80),
        )

        private fun mintDark() = KeyboardThemePalette(
            background = Color.rgb(32, 33, 36),
            key = Color.rgb(60, 64, 67),
            specialKey = Color.rgb(60, 64, 67),
            pressed = Color.rgb(95, 99, 104),
            text = Color.rgb(241, 243, 244),
            hint = Color.rgb(189, 193, 198),
            accent = Color.rgb(76, 175, 80),
            actionKey = Color.rgb(76, 175, 80),
        )

        /** Pure ARGB helpers keep palette resolution testable on the local JVM. */
        private object Color {
            const val BLACK: Int = -0x1000000
            const val WHITE: Int = -0x1

            fun rgb(red: Int, green: Int, blue: Int): Int =
                argb(255, red, green, blue)

            fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
                ((alpha and 0xFF) shl 24) or
                    ((red and 0xFF) shl 16) or
                    ((green and 0xFF) shl 8) or
                    (blue and 0xFF)

        }
    }
}
