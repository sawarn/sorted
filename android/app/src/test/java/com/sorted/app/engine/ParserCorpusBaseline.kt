package com.sorted.app.engine

import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Regression harness.
 *
 * Replays a local SMS corpus through [SmsParser] and [ImportDecisionPolicy] and writes one
 * line per message describing what the engine decided. Diffing two runs of this file shows
 * the exact behavioural delta of a parser change.
 *
 * The corpus holds real messages and is never committed. Point `SORTED_CORPUS` at a TSV of
 * `id <tab> address <tab> body` with `\n`, `\r`, `\t` and `\\` backslash-escaped in the body.
 * Output goes to `SORTED_BASELINE_OUT`, or `build/parser-baseline.tsv` by default.
 *
 * When `SORTED_CORPUS` is unset the test skips, so ordinary builds are unaffected.
 */
class ParserCorpusBaseline {
    @Test
    fun `replay corpus and record engine decisions`() {
        val corpusPath = System.getenv("SORTED_CORPUS")
        assumeTrue("SORTED_CORPUS not set; skipping corpus replay", !corpusPath.isNullOrBlank())

        val corpus = File(corpusPath)
        assumeTrue("corpus file missing: $corpusPath", corpus.isFile)

        val outPath = System.getenv("SORTED_BASELINE_OUT") ?: "build/parser-baseline.tsv"
        val out = File(outPath).apply { parentFile?.mkdirs() }

        var total = 0
        var transactions = 0
        val decisionCounts = sortedMapOf<String, Int>()

        out.bufferedWriter().use { writer ->
            writer.write(
                listOf(
                    "id", "isTransaction", "amount", "currency", "direction",
                    "merchant", "miscCategory", "departmentCategory", "type",
                    "status", "confidence", "categorySource", "evidenceConfidence",
                    "decision", "reason", "ignoreReason"
                ).joinToString("\t")
            )
            writer.newLine()

            corpus.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val cols = line.split('\t')
                if (cols.size < 3) return@forEachLine

                val id = cols[0]
                val address = cols[1].unescape()
                val body = cols[2].unescape()

                val parsed = SmsParser.parse(body, sourceAddress = address)
                val assessment = ImportDecisionPolicy.assess(parsed)

                total++
                if (parsed.isTransaction) transactions++
                decisionCounts.merge(assessment.decision.name, 1, Int::plus)

                writer.write(
                    listOf(
                        id,
                        parsed.isTransaction.toString(),
                        parsed.amount?.toString() ?: "",
                        parsed.currency ?: "",
                        parsed.direction.name,
                        parsed.merchantNormalized ?: "",
                        parsed.miscCategory ?: "",
                        parsed.departmentCategory ?: "",
                        parsed.transactionType.name,
                        parsed.status.name,
                        parsed.confidence.toString(),
                        parsed.categorySource.name,
                        parsed.evidenceConfidence.toString(),
                        assessment.decision.name,
                        assessment.reason,
                        parsed.ignoreReason ?: ""
                    ).joinToString("\t")
                )
                writer.newLine()
            }
        }

        println("corpus replay: $total messages, $transactions parsed as transactions")
        decisionCounts.forEach { (decision, count) -> println("  $decision = $count") }
        println("baseline written to ${out.absolutePath}")
    }

    private fun String.unescape(): String {
        val sb = StringBuilder(length)
        var i = 0
        while (i < length) {
            val c = this[i]
            if (c == '\\' && i + 1 < length) {
                when (this[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    else -> { sb.append(c); i++ }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
