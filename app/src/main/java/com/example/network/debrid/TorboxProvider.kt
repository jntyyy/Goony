package com.example.network.debrid

import android.util.Log
import com.example.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

class TorboxProvider(private val apiKeyProvider: () -> String) : DebridProvider {
    override val name: String = "Torbox"

    private val tag = "TorboxProvider"
    private val baseUrl = "https://api.torbox.app/v1/api"

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
            val url = "$baseUrl/torrents/checkcached?hash=$infoHash&format=object"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $key")
                .header("Accept", "application/json")
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful && body.isNotEmpty()) {
                val json = JSONObject(body)
                val success = json.optBoolean("success", false)
                val data = json.optJSONObject("data")
                if (success && data != null) {
                    val torrentData = data.opt(infoHash) ?: data.opt(infoHash.uppercase())
                    if (torrentData != null && torrentData != JSONObject.NULL && torrentData != false) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Torbox checkCache failed: ${e.message}")
        }
        false
    }

    override suspend fun resolveStream(magnetOrQuery: String): DebridResult = withContext(Dispatchers.IO) {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) {
            return@withContext DebridResult.Error(
                message = "Torbox API key is missing. Please configure it in Settings.",
                providerName = name
            )
        }

        val trimmed = magnetOrQuery.trim()
        val infoHash = extractHash(trimmed)
        val isMagnet = trimmed.startsWith("magnet:", ignoreCase = true)
        val isHash = !isMagnet && trimmed.matches(Regex("^[a-fA-F0-9]{40}$"))
        val magnetUrl = when {
            isMagnet -> trimmed
            isHash -> "magnet:?xt=urn:btih:$infoHash"
            else -> trimmed
        }

        try {
            Log.d(tag, "[TORBOX] Ultra-fast stream resolution for info_hash: $infoHash")

            var torrentId: String? = null
            var selectedFileId: String? = null
            var filename: String? = null
            var sizeBytes: Long? = null

            // Step 1: Add torrent directly via createtorrent (Instant add without slow full-account listing)
            val formBody = FormBody.Builder()
                .add("magnet", magnetUrl)
                .add("seed", "1")
                .add("allow_zip", "false")
                .build()

            val createReq = Request.Builder()
                .url("$baseUrl/torrents/createtorrent")
                .header("Authorization", "Bearer $key")
                .header("x-api-key", key)
                .post(formBody)
                .build()

            val createResp = NetworkClient.okHttpClient.newCall(createReq).execute()
            val createBody = createResp.body?.string().orEmpty()

            if (createResp.isSuccessful && createBody.isNotEmpty()) {
                val createJson = JSONObject(createBody)
                if (createJson.optBoolean("success", false)) {
                    val dataObj = createJson.optJSONObject("data")
                    torrentId = dataObj?.opt("torrent_id")?.toString()
                        ?: dataObj?.opt("id")?.toString()
                        ?: createJson.opt("torrent_id")?.toString()
                        ?: createJson.opt("id")?.toString()
                        ?: createJson.optString("data", "").takeIf { it.isNotEmpty() }
                    Log.d(tag, "[TORBOX] Torrent added/located with ID: $torrentId")
                }
            }

            // If createtorrent did not return ID directly (e.g. already existed), fallback to targeted search
            if (torrentId.isNullOrEmpty()) {
                try {
                    val listUrl = "$baseUrl/torrents/mylist"
                    val listReq = Request.Builder()
                        .url(listUrl)
                        .header("Authorization", "Bearer $key")
                        .header("x-api-key", key)
                        .build()

                    val listResp = NetworkClient.okHttpClient.newCall(listReq).execute()
                    val listBody = listResp.body?.string().orEmpty()

                    if (listResp.isSuccessful && listBody.isNotEmpty()) {
                        val listJson = JSONObject(listBody)
                        val dataArr = listJson.optJSONArray("data")
                        if (dataArr != null) {
                            for (i in 0 until dataArr.length()) {
                                val item = dataArr.optJSONObject(i) ?: continue
                                val itemHash = item.optString("hash", "").lowercase()
                                if (itemHash == infoHash) {
                                    torrentId = item.opt("id")?.toString() ?: item.optString("id", "")
                                    filename = item.optString("name", null)
                                    val filesArr = item.optJSONArray("files")
                                    selectedFileId = pickBestVideoFile(filesArr)
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Torbox list fallback failed: ${e.message}")
                }
            }

            // Step 2: Ensure we have the selected file ID
            if (!torrentId.isNullOrEmpty() && selectedFileId.isNullOrEmpty()) {
                delay(200) // Fast 200ms check
                val infoReq = Request.Builder()
                    .url("$baseUrl/torrents/mylist?id=$torrentId")
                    .header("Authorization", "Bearer $key")
                    .header("x-api-key", key)
                    .build()

                val infoResp = NetworkClient.okHttpClient.newCall(infoReq).execute()
                val infoBody = infoResp.body?.string().orEmpty()
                if (infoResp.isSuccessful && infoBody.isNotEmpty()) {
                    val infoJson = JSONObject(infoBody)
                    val dataObj = infoJson.optJSONObject("data")
                    if (dataObj != null) {
                        filename = dataObj.optString("name", filename)
                        val filesArr = dataObj.optJSONArray("files")
                        selectedFileId = pickBestVideoFile(filesArr)
                    } else {
                        val dataArr = infoJson.optJSONArray("data")
                        if (dataArr != null && dataArr.length() > 0) {
                            val firstObj = dataArr.optJSONObject(0)
                            if (firstObj != null) {
                                filename = firstObj.optString("name", filename)
                                selectedFileId = pickBestVideoFile(firstObj.optJSONArray("files"))
                            }
                        }
                    }
                }
            }

            if (torrentId.isNullOrEmpty()) {
                return@withContext DebridResult.Error(
                    "Unable to create or retrieve torrent on Torbox account",
                    null,
                    name
                )
            }

            // Step 4: Request direct download/stream link
            val fileParam = if (!selectedFileId.isNullOrEmpty()) "&file_id=$selectedFileId" else ""
            val requestDlUrl = "$baseUrl/torrents/requestdl?token=$key&torrent_id=$torrentId$fileParam&zip_link=false"

            val dlReq = Request.Builder()
                .url(requestDlUrl)
                .header("Authorization", "Bearer $key")
                .header("x-api-key", key)
                .build()

            val dlResp = NetworkClient.okHttpClient.newCall(dlReq).execute()
            val dlBody = dlResp.body?.string().orEmpty()

            if (!dlResp.isSuccessful) {
                return@withContext DebridResult.Error(
                    "Torbox requestdl HTTP ${dlResp.code}: $dlBody",
                    dlResp.code,
                    name
                )
            }

            val dlJson = JSONObject(dlBody)
            if (dlJson.optBoolean("success", false)) {
                val downloadUrl = if (dlJson.optJSONObject("data") != null) {
                    dlJson.getJSONObject("data").optString("url")
                } else {
                    dlJson.optString("data")
                }.trim()

                if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                    Log.d(tag, "[TORBOX] Successfully obtained direct stream URL: $downloadUrl")
                    return@withContext DebridResult.Success(
                        streamUrl = downloadUrl,
                        filename = filename,
                        sizeBytes = sizeBytes,
                        headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        ),
                        providerName = name
                    )
                } else {
                    return@withContext DebridResult.Error(
                        "Torbox returned invalid stream URL: $downloadUrl",
                        null,
                        name
                    )
                }
            } else {
                val detail = dlJson.optString("detail", "Torbox failed to generate stream link")
                return@withContext DebridResult.Error(detail, dlResp.code, name)
            }

        } catch (e: Exception) {
            Log.e(tag, "Torbox resolveStream error", e)
            return@withContext DebridResult.Error(
                message = "Torbox resolution error: ${e.message}",
                statusCode = null,
                providerName = name
            )
        }
    }

    private fun pickBestVideoFile(filesArr: JSONArray?): String? {
        if (filesArr == null || filesArr.length() == 0) return null
        var bestId: String? = null
        var largestSize = -1L

        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".webm", ".m4v", ".ts")

        for (i in 0 until filesArr.length()) {
            val f = filesArr.optJSONObject(i) ?: continue
            val id = f.opt("id")?.toString() ?: i.toString()
            val name = f.optString("name", "").lowercase()
            val size = f.optLong("size", f.optLong("bytes", 0L))

            val isVideo = videoExtensions.any { name.endsWith(it) }
            if (isVideo) {
                if (size > largestSize) {
                    largestSize = size
                    bestId = id
                }
            } else if (bestId == null && i == 0) {
                bestId = id
            }
        }
        return bestId
    }
}
