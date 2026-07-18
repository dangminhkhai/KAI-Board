package vn.kai.board.input

import java.text.Normalizer
import java.util.Locale

data class EmojiGroup(val icon: String, val values: List<String>)

/** Unicode emoji rendered by Android's system emoji font, grouped like CLDR keyboards. */
object EmojiCatalog {
    val groups = listOf(
        EmojiGroup("☺︎", listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😋","🤪","😎")),
        EmojiGroup("♙", listOf("👋","🤚","🖐️","✋","🖖","👌","🤌","🤏","✌️","🤞","🫶","🤟","🤘","🤙","👈","👉","👆","👇","👍","👏")),
        EmojiGroup("♧", listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🦄","🐝")),
        EmojiGroup("♨︎", listOf("🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍔","🍕","🍜")),
        EmojiGroup("⌂", listOf("🚗","🚕","🚌","🏎️","🚓","🚑","🚒","🚲","✈️","🚀","🚁","⛵","🏠","🏢","🏖️","🏝️","⛰️","🌋","🗼","🗽")),
        EmojiGroup("★", listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🏓","🏸","🥅","⛳","🏹","🎣","🥊","🎮","🎯","🏆")),
        EmojiGroup("⌘", listOf("⌚","📱","💻","⌨️","🖥️","🖨️","📷","🎥","📺","☎️","💡","🔦","📚","✏️","📌","🔑","🎁","🎈","✉️","❤️")),
        EmojiGroup("✦", listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","💯","💢","💥","💫","💦","💨","🔥","✨","⭐","✅","❌","❓")),
        EmojiGroup("⚑", listOf("🏳️","🏴","🏁","🚩","🇻🇳","🇺🇸","🇬🇧","🇯🇵","🇰🇷","🇨🇳","🇫🇷","🇩🇪","🇮🇹","🇨🇦","🇦🇺","🇸🇬","🇹🇭","🇮🇩","🇮🇳","🇧🇷")),
    )

    private val aliases = mapOf(
        "😀" to "mặt vui cười smile happy", "😂" to "cười nước mắt laugh tears", "😍" to "yêu thích love heart eyes",
        "❤️" to "tim yêu love heart", "👍" to "thích tốt like yes", "🙏" to "cảm ơn cầu nguyện pray thanks",
        "🔥" to "lửa nóng fire hot", "✅" to "đúng hoàn thành check done", "❌" to "sai xóa cross no",
        "🎉" to "chúc mừng tiệc party celebrate", "🎁" to "quà tặng gift", "🚗" to "xe ô tô car",
        "📱" to "điện thoại phone mobile", "💻" to "máy tính laptop computer", "📷" to "máy ảnh camera photo",
        "🐶" to "chó dog", "🐱" to "mèo cat", "🍎" to "táo apple", "⚽" to "bóng đá football soccer",
        "🇻🇳" to "việt nam vietnam cờ flag",
    )
    private val groupKeywords = listOf(
        "mặt cảm xúc vui buồn face emotion", "tay người hand gesture", "động vật con vật animal",
        "đồ ăn trái cây food fruit", "xe du lịch địa điểm travel car place", "thể thao trò chơi sport game",
        "đồ vật công nghệ object technology", "biểu tượng tim symbol heart", "cờ quốc gia flag country",
    )

    fun search(query: String, limit: Int = 10): List<String> {
        val needle = normalize(query.trim())
        if (needle.isEmpty() || limit <= 0) return emptyList()
        return groups.asSequence().flatMapIndexed { index, group ->
            val groupWords = normalize(groupKeywords[index])
            group.values.asSequence().filter { emoji ->
                groupWords.contains(needle) || normalize(aliases[emoji].orEmpty()).contains(needle)
            }
        }.distinct().take(limit).toList()
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)
}
