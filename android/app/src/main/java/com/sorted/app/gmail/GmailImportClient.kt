package com.sorted.app.gmail

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId

data class GmailRawMessage(
    val id: String,
    val threadId: String?,
    val internalDateMillis: Long?,
    val receivedDate: String?,
    val from: String?,
    val subject: String?,
    val snippet: String?,
    val bodyText: String
)

class GmailImportClient {
    fun fetchCandidateMessages(
        accessToken: String,
        maxResults: Int = GmailImportPlan.MaxResults
    ): List<GmailRawMessage> {
        val encodedQuery = URLEncoder.encode(GmailImportPlan.CandidateQuery, "UTF-8")
        val listUrl = "$ApiBase/users/me/messages?maxResults=$maxResults&includeSpamTrash=false&q=$encodedQuery"
        val listResponse = getJson(listUrl, accessToken)
        val messages = listResponse.optJSONArray("messages") ?: JSONArray()

        return (0 until messages.length())
            .mapNotNull { index -> messages.optJSONObject(index)?.optString("id") }
            .filter { it.isNotBlank() }
            .map { id -> fetchMessage(id, accessToken) }
    }

    private fun fetchMessage(id: String, accessToken: String): GmailRawMessage {
        val url = "$ApiBase/users/me/messages/$id?format=full"
        val json = getJson(url, accessToken)
        val payload = json.optJSONObject("payload")
        val internalDateMillis = json.optString("internalDate").toLongOrNull()
        val bodyText = payload?.let(::extractBodyText).orEmpty()

        return GmailRawMessage(
            id = json.optString("id"),
            threadId = json.optString("threadId").takeIf { it.isNotBlank() },
            internalDateMillis = internalDateMillis,
            receivedDate = internalDateMillis?.toIsoDate(),
            from = payload.header("From"),
            subject = payload.header("Subject"),
            snippet = json.optString("snippet").takeIf { it.isNotBlank() },
            bodyText = bodyText.ifBlank { json.optString("snippet") }
        )
    }

    private fun getJson(url: String, accessToken: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (code !in 200..299) {
            throw IllegalStateException("Gmail API error $code: ${body.take(240)}")
        }

        return JSONObject(body)
    }

    private fun JSONObject?.header(name: String): String? {
        val headers = this?.optJSONArray("headers") ?: return null
        for (index in 0 until headers.length()) {
            val header = headers.optJSONObject(index) ?: continue
            if (header.optString("name").equals(name, ignoreCase = true)) {
                return header.optString("value").takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractBodyText(payload: JSONObject): String {
        val texts = mutableListOf<String>()
        collectTextParts(payload, texts)
        return texts.joinToString("\n").compactWhitespace()
    }

    private fun collectTextParts(payload: JSONObject, texts: MutableList<String>) {
        val mimeType = payload.optString("mimeType")
        val data = payload.optJSONObject("body")?.optString("data").orEmpty()
        if (data.isNotBlank() && mimeType.startsWith("text/", ignoreCase = true)) {
            val decoded = decodeBase64Url(data)
            texts.add(if (mimeType.contains("html", ignoreCase = true)) decoded.stripHtml() else decoded)
        }

        val parts = payload.optJSONArray("parts") ?: return
        for (index in 0 until parts.length()) {
            parts.optJSONObject(index)?.let { collectTextParts(it, texts) }
        }
    }

    private fun decodeBase64Url(value: String): String {
        val normalized = value.replace('-', '+').replace('_', '/')
        val padding = when (normalized.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        return String(Base64.decode(normalized + padding, Base64.DEFAULT), Charsets.UTF_8)
    }

    private fun Long.toIsoDate(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    private fun String.stripHtml(): String {
        return replace(Regex("""(?is)<(script|style).*?</\1>"""), " ")
            .replace(Regex("""(?s)<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
    }

    private fun String.compactWhitespace(): String {
        return replace(Regex("""[ \t\r\n]+"""), " ").trim()
    }

    private companion object {
        const val ApiBase = "https://gmail.googleapis.com/gmail/v1"
    }
}
