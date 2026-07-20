package vn.kai.board.input

/**
 * Small offline bigram seed for cold-start next-word / mid-word phrase hints.
 * ~100 pairs of common social Vietnamese (+ a few English), not a full N-gram pack.
 */
object PhraseSeedCatalog {
    private val rawPairs: List<Pair<String, String>> = listOf(
        "xin" to "chào", "xin" to "lỗi", "xin" to "phép",
        "cảm" to "ơn", "cảm" to "thấy",
        "làm" to "ơn", "làm" to "gì", "làm" to "sao", "làm" to "việc",
        "tôi" to "là", "tôi" to "đang", "tôi" to "muốn", "tôi" to "cần", "tôi" to "sẽ",
        "mình" to "đang", "mình" to "sẽ", "mình" to "muốn", "mình" to "cần",
        "bạn" to "có", "bạn" to "ơi", "bạn" to "đang", "bạn" to "muốn", "bạn" to "khỏe",
        "anh" to "ơi", "anh" to "có", "anh" to "đang",
        "em" to "ơi", "em" to "có", "em" to "đang",
        "chúng" to "ta", "chúng" to "mình",
        "hôm" to "nay", "hôm" to "qua", "hôm" to "sau",
        "bây" to "giờ",
        "ngày" to "mai", "ngày" to "kia",
        "không" to "sao", "không" to "biết", "không" to "có", "không" to "được",
        "có" to "thể", "có" to "không", "có" to "gì",
        "rất" to "vui", "rất" to "tốt", "rất" to "nhiều",
        "được" to "rồi", "được" to "không",
        "đang" to "làm", "đang" to "ở", "đang" to "đi",
        "sẽ" to "làm", "sẽ" to "đi", "sẽ" to "gọi",
        "muốn" to "ăn", "muốn" to "uống", "muốn" to "đi",
        "cần" to "gì", "cần" to "giúp",
        "đi" to "đâu", "đi" to "thôi", "đi" to "với",
        "về" to "nhà", "về" to "đi",
        "ở" to "đâu", "ở" to "nhà",
        "chào" to "bạn", "chào" to "buổi", "chào" to "anh", "chào" to "em",
        "buổi" to "sáng", "buổi" to "trưa", "buổi" to "chiều", "buổi" to "tối",
        "tối" to "nay", "sáng" to "nay", "trưa" to "nay", "chiều" to "nay",
        "vui" to "vẻ", "tốt" to "lắm", "tốt" to "rồi", "ổn" to "không",
        "sao" to "vậy", "sao" to "rồi", "gì" to "đó", "gì" to "vậy",
        "thế" to "nào", "thôi" to "nhé", "rồi" to "nhé",
        "kiểm" to "tra", "gọi" to "điện", "nhắn" to "tin",
        "hẹn" to "gặp", "gặp" to "nhau", "gặp" to "sau",
        "giúp" to "mình", "giúp" to "tôi", "với" to "mình",
        "cho" to "mình", "cho" to "tôi", "cho" to "phép",
        "của" to "tôi", "của" to "bạn",
        "trong" to "này", "ngoài" to "ra",
        "ăn" to "gì", "ăn" to "chưa", "uống" to "gì", "uống" to "chưa",
        "học" to "bài", "việc" to "gì",
        "giờ" to "nào", "lúc" to "nào", "khi" to "nào",
        "nếu" to "có", "vì" to "sao", "nhưng" to "mà",
        "mọi" to "người", "người" to "ta",
        "đây" to "này", "đó" to "nhé", "này" to "nhé",
        "ok" to "nhé", "hello" to "there", "thank" to "you",
        "good" to "morning", "see" to "you", "how" to "are", "are" to "you",
    )

    /** Left word (lowercase) → ordered right-hand continuations. */
    val bigrams: Map<String, List<String>> = rawPairs
        .groupBy({ it.first.lowercase() }, { it.second })
        .mapValues { (_, rights) -> rights.distinct() }

    fun pairs(): List<Pair<String, String>> = rawPairs.map { it.first.lowercase() to it.second }.distinct()

    fun size(): Int = pairs().size

    fun continuations(left: String): List<String> =
        bigrams[left.lowercase()] ?: emptyList()
}
