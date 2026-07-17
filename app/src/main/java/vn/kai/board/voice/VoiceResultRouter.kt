package vn.kai.board.voice

enum class VoiceTarget(val maxCharacters: Int) {
    NORMAL(2_000),
    TRANSLATION(500),
    AI_COMMAND(2_000),
}

data class VoiceResult(val target: VoiceTarget, val text: String)

object VoiceResultRouter {
    fun prepare(targetName: String?, rawText: String?): VoiceResult? {
        val target = runCatching { VoiceTarget.valueOf(targetName.orEmpty()) }
            .getOrDefault(VoiceTarget.NORMAL)
        val text = rawText?.trim().orEmpty().take(target.maxCharacters)
        return text.takeIf(String::isNotEmpty)?.let { VoiceResult(target, it) }
    }
}
