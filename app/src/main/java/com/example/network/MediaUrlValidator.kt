package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URI

sealed class ValidatedMediaResult {
    data class Valid(
        val finalUrl: String,
        val mimeType: String?,
        val detectedFormat: String, // "MP4", "HLS", "DASH", "MKV"
        val httpStatusCode: Int,
        val contentLength: Long?,
        val headers: Map<String, String>
    ) : ValidatedMediaResult()

    data class Invalid(
        val reason: String,
        val httpStatusCode: Int? = null
    ) : ValidatedMediaResult()
}

object MediaUrlValidator {
    private const val TAG = "MediaUrlValidator"

    suspend fun validate(
        rawUrl: String,
        customHeaders: Map<String, String> = emptyMap()
    ): ValidatedMediaResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()

        if (trimmed.isEmpty()) {
            return@withContext ValidatedMediaResult.Invalid("Media URL is empty")
        }

        if (trimmed.startsWith("magnet:", ignoreCase = true)) {
            return@withContext ValidatedMediaResult.Invalid(
                "Cannot stream raw torrent magnet without resolving via Debrid provider."
            )
        }

        try {
            val uri = URI.create(trimmed)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                return@withContext ValidatedMediaResult.Invalid("Unsupported URL protocol: $scheme")
            }
        } catch (e: Exception) {
            return@withContext ValidatedMediaResult.Invalid("Malformed media URL: ${e.message}")
        }

        val lowerUrl = trimmed.lowercase()
        val defaultFormat = when {
            lowerUrl.contains(".mpd") -> "DASH"
            lowerUrl.contains(".m3u8") -> "HLS"
            lowerUrl.contains(".mkv") -> "MKV"
            lowerUrl.contains(".webm") -> "WEBM"
            else -> "MP4"
        }

        // Lightweight network probe: exploratory only, not blocking playback
        try {
            val reqBuilder = Request.Builder()
                .url(trimmed)
                .header(
                    "User-Agent",
                    customHeaders["User-Agent"]
                        ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                .header("Accept", "*/*")

            customHeaders.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true)) {
                    reqBuilder.header(k, v)
                }
            }

            // Use HEAD or GET with small range
            reqBuilder.header("Range", "bytes=0-1024")

            val response = NetworkClient.okHttpClient.newCall(reqBuilder.build()).execute()
            val code = response.code
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type")?.lowercase()
            val contentLength = response.header("Content-Length")?.toLongOrNull()
            response.close()

            Log.d(TAG, "[VALIDATOR] Probed stream URL: code=$code, type=$contentType, finalUrl=$finalUrl")

            val format = when {
                defaultFormat == "DASH" || contentType?.contains("dash+xml") == true -> "DASH"
                defaultFormat == "HLS" || contentType?.contains("mpegurl") == true -> "HLS"
                defaultFormat == "MKV" || contentType?.contains("matroska") == true -> "MKV"
                defaultFormat == "WEBM" || contentType?.contains("webm") == true -> "WEBM"
                else -> defaultFormat
            }

            return@withContext ValidatedMediaResult.Valid(
                finalUrl = finalUrl,
                mimeType = contentType,
                detectedFormat = format,
                httpStatusCode = code,
                contentLength = contentLength,
                headers = customHeaders
            )
        } catch (e: Exception) {
            Log.w(TAG, "Network probe warning for $trimmed: ${e.message}. Proceeding with stream playback.")
            return@withContext ValidatedMediaResult.Valid(
                finalUrl = trimmed,
                mimeType = null,
                detectedFormat = defaultFormat,
                httpStatusCode = 0,
                contentLength = null,
                headers = customHeaders
            )
        }
    }
}
