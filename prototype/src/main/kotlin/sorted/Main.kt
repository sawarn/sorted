package sorted

fun main(args: Array<String>) {
    val message = args.joinToString(" ").ifBlank {
        "Sent Rs.350.00\nFrom HDFC Bank A/C *1234\nTo Swiggy\nOn 07/08/26\nRef <ref>"
    }
    println(SmsParser.parse(message).toPrettyString())
}

fun ParsedTransaction.toPrettyString(): String {
    return """
        {
          "isTransaction": $isTransaction,
          "status": "${status.name.lowercase()}",
          "amount": ${amount?.toString() ?: "null"},
          "currency": ${currency?.quote() ?: "null"},
          "direction": "${direction.name.lowercase()}",
          "merchantRaw": ${merchantRaw?.quote() ?: "null"},
          "merchantNormalized": ${merchantNormalized?.quote() ?: "null"},
          "miscCategory": ${miscCategory?.quote() ?: "null"},
          "departmentCategory": ${departmentCategory?.quote() ?: "null"},
          "paymentMode": "${paymentMode.name.lowercase()}",
          "accountHint": ${accountHint?.quote() ?: "null"},
          "transactionDate": ${transactionDate?.quote() ?: "null"},
          "transactionTime": ${transactionTime?.quote() ?: "null"},
          "transactionType": "${transactionType.name.lowercase()}",
          "categorySource": "${categorySource.name.lowercase()}",
          "confidence": $confidence,
          "ignoreReason": ${ignoreReason?.quote() ?: "null"}
        }
    """.trimIndent()
}

private fun String.quote(): String = "\"" + replace("\"", "\\\"") + "\""

