package com.sorted.app.engine

/** Removes payment-routing and reference noise without guessing a merchant's identity. */
object MerchantNormalizer {
    private val url = Regex("""(?i)\b(?:https?://|www\.)\S+""")
    private val referenceTail = Regex(
        """(?i)\b(?:ref(?:erence)?|rrn|utr|txn(?:\s*id)?|transaction\s*id|upi\s*ref(?:erence)?)\b.*$"""
    )
    private val routingPrefix = Regex(
        """(?i)^(?:(?:vpa\s+)|(?:upi|p2m|p2a|pos|ecom)\s*[/#:_-]\s*)+"""
    )
    private val websiteSuffix = Regex("""(?i)\.(?:com|in|co|net|org)\b""")
    private val separators = Regex("""[^\p{L}\p{N}]+""")
    private val whitespace = Regex("""\s+""")

    fun normalize(value: String?): String? {
        var candidate = value.orEmpty().trim()
        if (candidate.isEmpty()) return null

        candidate = candidate
            .replace(url, " ")
            .replace(referenceTail, " ")
            .replace(routingPrefix, "")
            .substringBefore('@')
            .replace(websiteSuffix, " ")
            .replace(separators, " ")
            .replace(whitespace, " ")
            .trim()

        if (candidate.isEmpty() || candidate.all(Char::isDigit)) return null
        return candidate
            .lowercase()
            .split(' ')
            .joinToString(" ") { token -> token.replaceFirstChar(Char::uppercase) }
            .takeIf(String::isNotBlank)
    }

    fun key(value: String?): String = normalize(value)?.uppercase().orEmpty()
}
