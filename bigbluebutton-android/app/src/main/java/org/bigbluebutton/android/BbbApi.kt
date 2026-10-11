package org.bigbluebutton.android

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/** Minimal client for the BigBlueButton HTTP API (create + signed join URL). */
object BbbApi {

    class ApiException(message: String) : Exception(message)

    enum class Role(val apiValue: String) { VIEWER("VIEWER"), MODERATOR("MODERATOR") }

    // The server accepts any of these and detects the algorithm from the checksum length.
    private val ALGORITHMS = listOf("SHA-256", "SHA-1")

    /** "bbb.example.com" or "https://bbb.example.com/bigbluebutton" -> ".../bigbluebutton/api" */
    fun normalizeApiBase(input: String): String {
        var s = input.trim()
        if (!s.contains("://")) s = "https://$s"
        s = s.trimEnd('/').removeSuffix("/bigbluebutton/api").removeSuffix("/bigbluebutton")
        return "$s/bigbluebutton/api"
    }

    fun normalizeLink(input: String): String? {
        var s = input.trim()
        if (s.isEmpty() || s.contains(' ')) return null
        if (!s.contains("://")) s = "https://$s"
        val url = runCatching { URL(s) }.getOrNull() ?: return null
        return if (url.protocol == "https" || url.protocol == "http") s else null
    }

    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8").replace("+", "%20")

    private fun digest(algo: String, data: String): String =
        MessageDigest.getInstance(algo).digest(data.toByteArray()).joinToString("") { "%02x".format(it) }

    /** checksum = hash(callName + queryString + secret) */
    fun signedUrl(apiBase: String, call: String, params: List<Pair<String, String>>, secret: String, algo: String): String {
        val query = params.joinToString("&") { "${it.first}=${enc(it.second)}" }
        val sum = digest(algo, call + query + secret)
        return "$apiBase/$call?$query&checksum=$sum"
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        return try {
            val stream = if (conn.responseCode < 400) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            conn.disconnect()
        }
    }

    private fun tag(xml: String, name: String): String? =
        Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1)?.trim()

    /**
     * Creates the meeting (no-op if it is already running) and returns a signed join URL.
     * Blocking: call from a background dispatcher.
     */
    fun createAndJoin(
        server: String,
        secret: String,
        meetingId: String,
        meetingName: String,
        fullName: String,
        role: Role,
    ): String {
        val base = normalizeApiBase(server)
        var lastError = "unknown error"
        for (algo in ALGORITHMS) {
            val createUrl = signedUrl(
                base, "create",
                listOf("meetingID" to meetingId, "name" to meetingName.ifBlank { meetingId }),
                secret, algo,
            )
            val xml = get(createUrl)
            if (tag(xml, "returncode") == "SUCCESS") {
                return signedUrl(
                    base, "join",
                    listOf(
                        "fullName" to fullName,
                        "meetingID" to meetingId,
                        "role" to role.apiValue,
                        "redirect" to "true",
                    ),
                    secret, algo,
                )
            }
            lastError = tag(xml, "message") ?: tag(xml, "messageKey") ?: "invalid response"
            if (tag(xml, "messageKey") != "checksumError") break
        }
        throw ApiException(lastError)
    }
}
