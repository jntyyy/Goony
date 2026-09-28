package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class StashPerformer(
    val id: String,
    val name: String,
    val disambiguation: String? = null,
    val aliases: List<String> = emptyList(),
    val gender: String? = null,
    val country: String? = null,
    val imageUrl: String? = null
)

data class StashStudio(
    val id: String,
    val name: String,
    val parentName: String? = null,
    val logoUrl: String? = null
)

data class StashScene(
    val id: String,
    val title: String,
    val details: String? = null,
    val date: String? = null,
    val studioId: String? = null,
    val studioName: String? = null,
    val studioLogo: String? = null,
    val coverUrl: String? = null,
    val femalePerformers: List<StashPerformer> = emptyList()
)

data class StashSceneQueryResult(
    val count: Int,
    val scenes: List<StashScene>
)

object StashDbApiService {
    private const val GRAPHQL_ENDPOINT = "https://stashdb.org/graphql"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun searchPerformers(query: String, apiKey: String): Result<List<StashPerformer>> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("StashDB API Key is required"))
            }

            val gqlQuery = """
                query SearchPerformers(${'$'}term: String!) {
                  searchPerformers(term: ${'$'}term, limit: 30) {
                    count
                    performers {
                      id
                      name
                      disambiguation
                      aliases
                      gender
                      country
                      images {
                        url
                      }
                    }
                  }
                }
            """.trimIndent()

            val bodyJson = JSONObject().apply {
                put("query", gqlQuery)
                put("variables", JSONObject().apply { put("term", query) })
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .header("ApiKey", apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("StashDB Error: HTTP ${response.code}"))
            }

            val json = JSONObject(rawBody)
            if (json.has("errors")) {
                val errorMsg = json.getJSONArray("errors").optJSONObject(0)?.optString("message") ?: "GraphQL query error"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val dataObj = json.optJSONObject("data")
            val performersArray = dataObj?.optJSONObject("searchPerformers")?.optJSONArray("performers")
                ?: dataObj?.optJSONArray("searchPerformer")
                ?: JSONArray()

            val results = mutableListOf<StashPerformer>()

            for (i in 0 until performersArray.length()) {
                val item = performersArray.optJSONObject(i) ?: continue
                val gender = item.optString("gender", "").uppercase()

                // Skip strictly male performers, but accept FEMALE, TRANSGENDER_FEMALE, or unspecified/empty
                if (gender == "MALE") {
                    continue
                }

                val id = item.optString("id")
                val name = item.optString("name")
                if (id.isBlank() || name.isBlank()) continue

                val disambiguation = item.optString("disambiguation").ifBlank { null }
                val country = item.optString("country").ifBlank { null }

                val aliasesList = mutableListOf<String>()
                val aliasesArray = item.optJSONArray("aliases")
                if (aliasesArray != null) {
                    for (j in 0 until aliasesArray.length()) {
                        val alias = aliasesArray.optString(j)
                        if (alias.isNotBlank()) aliasesList.add(alias)
                    }
                }

                val imagesArray = item.optJSONArray("images")
                val imageUrl = if (imagesArray != null && imagesArray.length() > 0) {
                    imagesArray.optJSONObject(0)?.optString("url")?.ifBlank { null }
                } else null

                results.add(
                    StashPerformer(
                        id = id,
                        name = name,
                        disambiguation = disambiguation,
                        aliases = aliasesList,
                        gender = gender,
                        country = country,
                        imageUrl = imageUrl
                    )
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchStudios(query: String, apiKey: String): Result<List<StashStudio>> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("StashDB API Key is required"))
            }

            val gqlQuery = """
                query SearchStudios(${'$'}term: String!) {
                  searchStudio(term: ${'$'}term, limit: 30) {
                    id
                    name
                    images {
                      url
                    }
                    parent {
                      id
                      name
                    }
                  }
                }
            """.trimIndent()

            val bodyJson = JSONObject().apply {
                put("query", gqlQuery)
                put("variables", JSONObject().apply { put("term", query) })
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .header("ApiKey", apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("StashDB Error: HTTP ${response.code}"))
            }

            val json = JSONObject(rawBody)
            if (json.has("errors")) {
                val errorMsg = json.getJSONArray("errors").optJSONObject(0)?.optString("message") ?: "GraphQL query error"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val dataObj = json.optJSONObject("data")
            val studiosArray = dataObj?.optJSONArray("searchStudio") ?: JSONArray()
            val results = mutableListOf<StashStudio>()

            for (i in 0 until studiosArray.length()) {
                val item = studiosArray.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                if (id.isBlank() || name.isBlank()) continue

                val parentName = item.optJSONObject("parent")?.optString("name")?.ifBlank { null }
                val imagesArray = item.optJSONArray("images")
                val logoUrl = if (imagesArray != null && imagesArray.length() > 0) {
                    imagesArray.optJSONObject(0)?.optString("url")?.ifBlank { null }
                } else null

                results.add(
                    StashStudio(
                        id = id,
                        name = name,
                        parentName = parentName,
                        logoUrl = logoUrl
                    )
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryPerformerScenes(
        performerId: String,
        apiKey: String,
        page: Int = 1,
        perPage: Int = 50
    ): Result<StashSceneQueryResult> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("StashDB API Key is required"))
            }

            val gqlQuery = """
                query QueryPerformerScenes(${'$'}input: SceneQueryInput!) {
                  queryScenes(input: ${'$'}input) {
                    count
                    scenes {
                      id
                      title
                      details
                      date
                      images {
                        url
                      }
                      studio {
                        id
                        name
                        images {
                          url
                        }
                      }
                      performers {
                        as
                        performer {
                          id
                          name
                          gender
                          images {
                            url
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val inputObj = JSONObject().apply {
                put("performers", JSONObject().apply {
                    put("value", JSONArray().apply { put(performerId) })
                    put("modifier", "INCLUDES")
                })
                put("page", page)
                put("per_page", perPage)
                put("direction", "DESC")
                put("sort", "DATE")
            }

            val bodyJson = JSONObject().apply {
                put("query", gqlQuery)
                put("variables", JSONObject().apply { put("input", inputObj) })
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .header("ApiKey", apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("StashDB Error: HTTP ${response.code}"))
            }

            val json = JSONObject(rawBody)
            if (json.has("errors")) {
                val errorMsg = json.getJSONArray("errors").optJSONObject(0)?.optString("message") ?: "GraphQL query error"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val dataObj = json.optJSONObject("data")
            val queryScenesObj = dataObj?.optJSONObject("queryScenes")
            val count = queryScenesObj?.optInt("count", 0) ?: 0
            val scenesArray = queryScenesObj?.optJSONArray("scenes") ?: JSONArray()
            val results = parseScenesJson(scenesArray)

            Result.success(StashSceneQueryResult(count = count, scenes = results))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryStudioScenes(
        studioId: String,
        apiKey: String,
        page: Int = 1,
        perPage: Int = 50
    ): Result<StashSceneQueryResult> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("StashDB API Key is required"))
            }

            val gqlQuery = """
                query QueryStudioScenes(${'$'}input: SceneQueryInput!) {
                  queryScenes(input: ${'$'}input) {
                    count
                    scenes {
                      id
                      title
                      details
                      date
                      images {
                        url
                      }
                      studio {
                        id
                        name
                        images {
                          url
                        }
                      }
                      performers {
                        as
                        performer {
                          id
                          name
                          gender
                          images {
                            url
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            // Try parentStudio first (matches parent studio + all its sub-studios and standalone studios)
            val parentStudioInput = JSONObject().apply {
                put("parentStudio", studioId)
                put("page", page)
                put("per_page", perPage)
                put("direction", "DESC")
                put("sort", "DATE")
            }

            val bodyJson = JSONObject().apply {
                put("query", gqlQuery)
                put("variables", JSONObject().apply { put("input", parentStudioInput) })
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .header("ApiKey", apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("StashDB Error: HTTP ${response.code}"))
            }

            val json = JSONObject(rawBody)
            if (json.has("errors")) {
                val errorMsg = json.getJSONArray("errors").optJSONObject(0)?.optString("message") ?: "GraphQL query error"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val dataObj = json.optJSONObject("data")
            val queryScenesObj = dataObj?.optJSONObject("queryScenes")
            val count = queryScenesObj?.optInt("count", 0) ?: 0
            val scenesArray = queryScenesObj?.optJSONArray("scenes") ?: JSONArray()
            var results = parseScenesJson(scenesArray)

            // If parentStudio returned 0 results, fallback to exact studios filter
            if (results.isEmpty() && count == 0) {
                val fallbackInput = JSONObject().apply {
                    put("studios", JSONObject().apply {
                        put("value", JSONArray().apply { put(studioId) })
                        put("modifier", "INCLUDES")
                    })
                    put("page", page)
                    put("per_page", perPage)
                    put("direction", "DESC")
                    put("sort", "DATE")
                }

                val fallbackBody = JSONObject().apply {
                    put("query", gqlQuery)
                    put("variables", JSONObject().apply { put("input", fallbackInput) })
                }

                val fallbackReq = Request.Builder()
                    .url(GRAPHQL_ENDPOINT)
                    .header("ApiKey", apiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(fallbackBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val fallbackResp = NetworkClient.okHttpClient.newCall(fallbackReq).execute()
                val fallbackRaw = fallbackResp.body?.string() ?: ""
                if (fallbackResp.isSuccessful) {
                    val fallbackJson = JSONObject(fallbackRaw)
                    val fallbackData = fallbackJson.optJSONObject("data")?.optJSONObject("queryScenes")
                    val fallbackCount = fallbackData?.optInt("count", 0) ?: 0
                    val fallbackArray = fallbackData?.optJSONArray("scenes") ?: JSONArray()
                    results = parseScenesJson(fallbackArray)
                    return@withContext Result.success(StashSceneQueryResult(count = fallbackCount, scenes = results))
                }
            }

            Result.success(StashSceneQueryResult(count = count, scenes = results))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseScenesJson(scenesArray: JSONArray): List<StashScene> {
        val results = mutableListOf<StashScene>()
        for (i in 0 until scenesArray.length()) {
            val item = scenesArray.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue

            val rawTitle = item.optString("title").ifBlank { null }
            val details = item.optString("details").ifBlank { null }
            val date = item.optString("date").ifBlank { null }

            val imagesArray = item.optJSONArray("images")
            val coverUrl = if (imagesArray != null && imagesArray.length() > 0) {
                imagesArray.optJSONObject(0)?.optString("url")?.ifBlank { null }
            } else null

            val studioObj = item.optJSONObject("studio")
            val studioId = studioObj?.optString("id")?.ifBlank { null }
            val studioName = studioObj?.optString("name")?.ifBlank { null }
            val studioImages = studioObj?.optJSONArray("images")
            val studioLogo = if (studioImages != null && studioImages.length() > 0) {
                studioImages.optJSONObject(0)?.optString("url")?.ifBlank { null }
            } else null

            // Parse performers (skip strictly male, keep female / transgender female / unspecified)
            val femalePerformers = mutableListOf<StashPerformer>()
            val performersArray = item.optJSONArray("performers")
            if (performersArray != null) {
                for (j in 0 until performersArray.length()) {
                    val perfEntry = performersArray.optJSONObject(j) ?: continue
                    val perfObj = perfEntry.optJSONObject("performer") ?: continue
                    val gender = perfObj.optString("gender", "").uppercase()

                    // Skip strictly male performers
                    if (gender == "MALE") {
                        continue
                    }

                    val perfId = perfObj.optString("id")
                    val perfName = perfObj.optString("name")
                    if (perfId.isBlank() || perfName.isBlank()) continue

                    val perfImages = perfObj.optJSONArray("images")
                    val perfImage = if (perfImages != null && perfImages.length() > 0) {
                        perfImages.optJSONObject(0)?.optString("url")?.ifBlank { null }
                    } else null

                    femalePerformers.add(
                        StashPerformer(
                            id = perfId,
                            name = perfName,
                            gender = gender,
                            imageUrl = perfImage
                        )
                    )
                }
            }

            // Provide fallback title if blank
            val title = rawTitle ?: if (!studioName.isNullOrBlank() && !date.isNullOrBlank()) {
                "$studioName - $date"
            } else if (!date.isNullOrBlank()) {
                "Scene $date"
            } else {
                "Scene ${id.take(8)}"
            }

            results.add(
                StashScene(
                    id = id,
                    title = title,
                    details = details,
                    date = date,
                    studioId = studioId,
                    studioName = studioName,
                    studioLogo = studioLogo,
                    coverUrl = coverUrl,
                    femalePerformers = femalePerformers
                )
            )
        }
        return results
    }

    suspend fun queryScenesByText(
        text: String,
        apiKey: String,
        page: Int = 1,
        perPage: Int = 50
    ): Result<StashSceneQueryResult> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("StashDB API Key is required"))
            }

            val gqlQuery = """
                query QueryScenesByText(${'$'}input: SceneQueryInput!) {
                  queryScenes(input: ${'$'}input) {
                    count
                    scenes {
                      id
                      title
                      details
                      date
                      images {
                        url
                      }
                      studio {
                        id
                        name
                        images {
                          url
                        }
                      }
                      performers {
                        as
                        performer {
                          id
                          name
                          gender
                          images {
                            url
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val inputObj = JSONObject().apply {
                if (text.isNotBlank()) {
                    put("text", text)
                }
                put("page", page)
                put("per_page", perPage)
                put("direction", "DESC")
                put("sort", "DATE")
            }

            val bodyJson = JSONObject().apply {
                put("query", gqlQuery)
                put("variables", JSONObject().apply { put("input", inputObj) })
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .header("ApiKey", apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("StashDB Error: HTTP ${response.code}"))
            }

            val json = JSONObject(rawBody)
            if (json.has("errors")) {
                val errorMsg = json.getJSONArray("errors").optJSONObject(0)?.optString("message") ?: "GraphQL query error"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val dataObj = json.optJSONObject("data")
            val queryScenesObj = dataObj?.optJSONObject("queryScenes")
            val count = queryScenesObj?.optInt("count", 0) ?: 0
            val scenesArray = queryScenesObj?.optJSONArray("scenes") ?: JSONArray()
            val results = parseScenesJson(scenesArray)

            Result.success(StashSceneQueryResult(count = count, scenes = results))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryRecentScenes(
        apiKey: String,
        page: Int = 1,
        perPage: Int = 50
    ): Result<StashSceneQueryResult> = queryScenesByText(text = "", apiKey = apiKey, page = page, perPage = perPage)

    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext false
            val result = searchPerformers("a", apiKey.trim())
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
}
