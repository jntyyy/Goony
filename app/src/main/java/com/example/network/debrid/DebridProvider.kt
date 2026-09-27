package com.example.network.debrid

sealed class DebridResult {
    data class Success(
        val streamUrl: String,
        val filename: String? = null,
        val sizeBytes: Long? = null,
        val headers: Map<String, String> = emptyMap(),
        val providerName: String
    ) : DebridResult()

    data class Error(
        val message: String,
        val statusCode: Int? = null,
        val providerName: String
    ) : DebridResult()
}

interface DebridProvider {
    val name: String
    fun isConfigured(): Boolean
    suspend fun checkCache(hash: String): Boolean
    suspend fun resolveStream(magnetOrQuery: String): DebridResult
}
