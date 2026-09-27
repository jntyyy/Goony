package com.example.network.debrid

import android.util.Log
import com.example.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern

class RealDebridProvider(private val apiKeyProvider: () -> String) : DebridProvider {
    override val name: String = "Real-Debrid"

    private val tag = "RealDebridProvider"
    private val baseUrl = "https://api.real-debrid.com/rest/1.0"

    override fun isConfigured(): Boolean = apiKeyProvider().trim().isNotEmpty()

    fun extractHash(magnetOrHash: String): String {
        val trimmed = magnetOrHash.trim()
        if (trimmed.startsWith("magnet:", ignoreCase = true)) {
            val matcher = Pattern.compile("btih:([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE).matcher(trimmed)
            if (matcher.find()) {
                return matcher.group(1)?.lowercase() ?: trimmed
            }
        }
        return trimmed.lowercase()
    }

    override suspend fun checkCache(hash: String): Boolean = withContext(Dispatchers.IO) {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) return@withContext false

        try {
            val infoHash = extractHash(hash)
            val url = "$baseUrl/torrents/instantAvailability/$infoHash"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $key")
                .build()

            val resp = NetworkClient.okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (resp.isSuccessful && body.isNotEmpty()) {
                val json = JSONObject(body)
                val hashObj = json.optJSONObject(infoHash) ?: json.optJSONObject(infoHash.lowercase())
                val rdArr = hashObj?.optJSONArray("rd")
                return@withContext rdArr != null && rdArr.length() > 0
            }
        } catch (e: Exception) {
            Log.e(tag, "Real-Debrid checkCache failed: ${e.message}")
        }
        false
    }

    override suspend fun resolveStream(magnetOrQuery: String): DebridResult = withContext(Dispatchers.IO) {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) {
            return@withContext DebridResult.Error(
                message = "Real-Debrid API key is missing. Please configure it in Settings.",
                providerName = name
            )
        }

        val trimmed = magnetOrQuery.trim()
        val isMagnet = trimmed.startsWith("magnet:", ignoreCase = true)
        val isTorrentHash = !isMagnet && trimmed.matches(Regex("^[a-fA-F0-9]{40}$"))

        try {
            Log.d(tag, "[REAL-DEBRID] Resolving stream for input: $trimmed")

            if (isMagnet || isTorrentHash) {
                val infoHash = extractHash(trimmed)
                val magnetUri = if (isTorrentHash) "magnet:?xt=urn:btih:$infoHash" else trimmed

                // Step 1: Add magnet to Real-Debrid
                val addBody = FormBody.Builder().add("magnet", magnetUri).build()
                val addReq = Request.Builder()
                    .url("$baseUrl/torrents/addMagnet")
                    .header("Authorization", "Bearer $key")
                    .post(addBody)
                    .build()

                val addResp = NetworkClient.okHttpClient.newCall(addReq).execute()
                val addStr = addResp.body?.string().orEmpty()

                if (!addResp.isSuccessful) {
                    return@withContext DebridResult.Error(
                        "Real-Debrid addMagnet HTTP ${addResp.code}: $addStr",
                        addResp.code,
                        name
                    )
                }

                val addJson = JSONObject(addStr)
                val torrentId = addJson.optString("id", "")
                if (torrentId.isEmpty()) {
                    return@withContext DebridResult.Error(
                        "Real-Debrid failed to return torrent ID",
                        addResp.code,
                        name
                    )
                }

                Log.d(tag, "[REAL-DEBRID] Magnet added, torrentId=$torrentId. Polling status...")

                // Step 2: Ultra-Fast Polling (200ms interval) - Instant file selection and link capture
                var rawLink: String? = null
                var filename: String? = null
                var filesSelected = false

                for (attempt in 1..15) {
                    if (attempt > 1) {
                        delay(200)
                    } else {
                        delay(50)
                    }

                    val infoReq = Request.Builder()
                        .url("$baseUrl/torrents/info/$torrentId")
                        .header("Authorization", "Bearer $key")
                        .build()

                    val infoResp = NetworkClient.okHttpClient.newCall(infoReq).execute()
                    val infoStr = infoResp.body?.string().orEmpty()
                    if (!infoResp.isSuccessful || infoStr.isEmpty()) continue

                    val infoJson = JSONObject(infoStr)
                    val status = infoJson.optString("status")
                    filename = infoJson.optString("filename", filename)

                    // If waiting for file selection, select all files immediately
                    if (!filesSelected && status.equals("waiting_files_selection", ignoreCase = true)) {
                        val selectBody = FormBody.Builder().add("files", "all").build()
                        val selectReq = Request.Builder()
                            .url("$baseUrl/torrents/selectFiles/$torrentId")
                            .header("Authorization", "Bearer $key")
                            .post(selectBody)
                            .build()
                        val selResp = NetworkClient.okHttpClient.newCall(selectReq).execute()
                        selResp.close()
                        filesSelected = true
                        continue
                    }

                    // Check if links are already populated
                    val linksArr = infoJson.optJSONArray("links")
                    if (linksArr != null && linksArr.length() > 0) {
                        rawLink = linksArr.optString(0)
                        break
                    }

                    if (status.equals("downloaded", ignoreCase = true)) {
                        val links = infoJson.optJSONArray("links")
                        if (links != null && links.length() > 0) {
                            rawLink = links.optString(0)
                            break
                        }
                    }

                    // If downloading but not instant cached
                    if (status.equals("downloading", ignoreCase = true) || status.equals("queued", ignoreCase = true)) {
                        val progress = infoJson.optDouble("progress", 0.0)
                        if (attempt >= 8) {
                            return@withContext DebridResult.Error(
                                "Real-Debrid: Torrent is downloading to cloud (${progress.toInt()}%). Instant streaming requires pre-cached files.",
                                null,
                                name
                            )
                        }
                    }
                }

                if (rawLink.isNullOrEmpty()) {
                    return@withContext DebridResult.Error(
                        "Real-Debrid: Torrent links are not ready or torrent is not cached in RD cloud.",
                        null,
                        name
                    )
                }

                // Step 3: Unrestrict the RD link to get direct stream URL
                return@withContext unrestrictLink(rawLink, key, filename)

            } else {
                // Direct file hoster link (e.g. 1fichier, rapidgator, mega)
                return@withContext unrestrictLink(trimmed, key, null)
            }

        } catch (e: Exception) {
            Log.e(tag, "Real-Debrid resolveStream error", e)
            return@withContext DebridResult.Error(
                message = "Real-Debrid resolution error: ${e.message}",
                statusCode = null,
                providerName = name
            )
        }
    }

    private fun unrestrictLink(rawLink: String, key: String, initialFilename: String?): DebridResult {
        Log.d(tag, "[REAL-DEBRID] Unrestricting link: $rawLink")
        val form = FormBody.Builder().add("link", rawLink).build()
        val unrestrictReq = Request.Builder()
            .url("$baseUrl/unrestrict/link")
            .header("Authorization", "Bearer $key")
            .post(form)
            .build()

        val resp = NetworkClient.okHttpClient.newCall(unrestrictReq).execute()
        val bodyStr = resp.body?.string().orEmpty()

        if (!resp.isSuccessful) {
            return DebridResult.Error(
                "Real-Debrid unrestrict HTTP ${resp.code}: $bodyStr",
                resp.code,
                name
            )
        }

        val json = JSONObject(bodyStr)
        val downloadUrl = json.optString("download", "")
        val filename = json.optString("filename", initialFilename)
        val filesize = json.optLong("filesize", 0L)

        if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
            Log.d(tag, "[REAL-DEBRID] Successfully unrestricted download URL: $downloadUrl")
            return DebridResult.Success(
                streamUrl = downloadUrl,
                filename = filename,
                sizeBytes = if (filesize > 0) filesize else null,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                ),
                providerName = name
            )
        } else {
            return DebridResult.Error(
                "Real-Debrid returned invalid download link in response: $bodyStr",
                resp.code,
                name
            )
        }
    }
}
