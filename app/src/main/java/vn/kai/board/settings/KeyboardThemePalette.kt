package vn.kai.board.settings

import android.content.Context
import android.graphics.Color

data class KeyboardThemePalette(
    val background: Int,
    val key: Int,
    val specialKey: Int,
    val pressed: Int,
    val text: Int,
    val hint: Int,
    val accent: Int,
    val gradientColors: IntArray? = null,
) {
    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun resolve(context: Context, preset: KeyboardColorStyle, dark: Boolean): KeyboardThemePalette =
            if (dark) dark(Color.rgb(59, 130, 246)) else light(Color.rgb(37, 99, 235))

        private fun light(accent: Int) = tinted(accent, Color.rgb(226, 232, 240))

        private fun dark(accent: Int) = KeyboardThemePalette(
            Color.rgb(17, 24, 39), Color.rgb(51, 65, 85), Color.rgb(30, 41, 59),
            blend(accent, Color.WHITE, 0.18f), Color.rgb(248, 250, 252), Color.rgb(148, 163, 184), accent,
        )

        private fun tinted(accent: Int, background: Int) = KeyboardThemePalette(
            background, Color.WHITE, blend(background, accent, 0.22f), blend(accent, Color.WHITE, 0.45f),
            Color.rgb(15, 23, 42), Color.rgb(71, 85, 105), accent,
        )

        private fun blend(first: Int, second: Int, secondWeight: Float): Int {
            val w = secondWeight.coerceIn(0f, 1f)
            return Color.rgb(
                (Color.red(first) * (1f - w) + Color.red(second) * w).toInt(),
                (Color.green(first) * (1f - w) + Color.green(second) * w).toInt(),
                (Color.blue(first) * (1f - w) + Color.blue(second) * w).toInt(),
            )
        }
    }
}
