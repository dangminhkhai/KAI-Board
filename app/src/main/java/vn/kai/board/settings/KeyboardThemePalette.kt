package vn.kai.board.settings

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
        fun resolve(preset: KeyboardColorStyle, dark: Boolean): KeyboardThemePalette =
            when (preset) {
                KeyboardColorStyle.CLASSIC -> if (dark) mintDark() else mintLight()
                KeyboardColorStyle.AI_GRADIENT_2026 -> if (dark) gradientDark() else gradientLight()
                KeyboardColorStyle.OCEAN -> if (dark) oceanDark() else oceanLight()
            }

        private fun mintLight() = KeyboardThemePalette(
            background = Color.rgb(241, 245, 249),
            key = Color.WHITE,
            specialKey = Color.rgb(204, 238, 220),
            pressed = Color.rgb(151, 220, 192),
            text = Color.rgb(15, 23, 42),
            hint = Color.rgb(71, 85, 105),
            accent = Color.rgb(31, 127, 112),
        )

        private fun mintDark() = KeyboardThemePalette(
            background = Color.rgb(17, 24, 39),
            key = Color.BLACK,
            specialKey = Color.rgb(31, 76, 68),
            pressed = Color.rgb(62, 130, 109),
            text = Color.rgb(248, 250, 252),
            hint = Color.rgb(148, 163, 184),
            accent = Color.rgb(31, 127, 112),
        )

        private fun gradientLight() = KeyboardThemePalette(
            background = Color.rgb(238, 242, 255),
            key = Color.WHITE,
            specialKey = Color.rgb(221, 214, 254),
            pressed = Color.rgb(196, 181, 253),
            text = Color.rgb(15, 23, 42),
            hint = Color.rgb(71, 85, 105),
            accent = Color.rgb(79, 70, 229),
            gradientColors = intArrayOf(
                Color.rgb(207, 250, 254),
                Color.rgb(221, 214, 254),
                Color.rgb(252, 231, 243),
            ),
        )

        private fun gradientDark() = KeyboardThemePalette(
            background = Color.rgb(7, 17, 31),
            key = Color.BLACK,
            specialKey = Color.rgb(49, 46, 129),
            pressed = Color.rgb(91, 33, 182),
            text = Color.rgb(248, 250, 252),
            hint = Color.rgb(196, 181, 253),
            accent = Color.rgb(139, 92, 246),
            gradientColors = intArrayOf(
                Color.rgb(7, 17, 31),
                Color.rgb(23, 37, 84),
                Color.rgb(59, 7, 100),
            ),
        )

        private fun oceanLight() = tinted(
            accent = Color.rgb(37, 99, 235),
            background = Color.rgb(226, 232, 240),
        )

        private fun oceanDark() = KeyboardThemePalette(
            background = Color.rgb(17, 24, 39),
            key = Color.BLACK,
            specialKey = Color.rgb(30, 41, 59),
            pressed = blend(Color.rgb(59, 130, 246), Color.WHITE, 0.18f),
            text = Color.rgb(248, 250, 252),
            hint = Color.rgb(148, 163, 184),
            accent = Color.rgb(59, 130, 246),
        )

        private fun tinted(accent: Int, background: Int) = KeyboardThemePalette(
            background = background,
            key = Color.WHITE,
            specialKey = blend(background, accent, 0.22f),
            pressed = blend(accent, Color.WHITE, 0.45f),
            text = Color.rgb(15, 23, 42),
            hint = Color.rgb(71, 85, 105),
            accent = accent,
        )

        private fun blend(first: Int, second: Int, secondWeight: Float): Int {
            val w = secondWeight.coerceIn(0f, 1f)
            return Color.rgb(
                (Color.red(first) * (1f - w) + Color.red(second) * w).toInt(),
                (Color.green(first) * (1f - w) + Color.green(second) * w).toInt(),
                (Color.blue(first) * (1f - w) + Color.blue(second) * w).toInt(),
            )
        }

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

            fun red(color: Int): Int = color ushr 16 and 0xFF
            fun green(color: Int): Int = color ushr 8 and 0xFF
            fun blue(color: Int): Int = color and 0xFF
        }
    }
}
