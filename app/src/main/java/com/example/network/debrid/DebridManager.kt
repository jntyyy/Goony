package com.example.network.debrid

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DebridManager(
    private val torboxKeyProvider: () -> String,
    private val realDebridKeyProvider: () -> String
) {
    private val tag = "DebridManager"

    val torboxProvider = TorboxProvider(torboxKeyProvider)
    val realDebridProvider = RealDebridProvider(realDebridKeyProvider)

    fun hasAnyProviderConfigured(): Boolean {
        return torboxProvider.isConfigured() || realDebridProvider.isConfigured()
    }

    suspend fun resolve(queryOrMagnet: String): DebridResult = coroutineScope {
        val torboxConfigured = torboxProvider.isConfigured()
        val rdConfigured = realDebridProvider.isConfigured()

        if (!torboxConfigured && !rdConfigured) {
            return@coroutineScope DebridResult.Error(
                message = "No Debrid provider is configured. Please add your Torbox or Real-Debrid API key in Settings.",
                providerName = "None"
            )
        }

        // If only one provider is configured, execute directly without extra overhead
        if (torboxConfigured && !rdConfigured) {
            Log.d(tag, "Resolving via Torbox (single configured provider)...")
            return@coroutineScope torboxProvider.resolveStream(queryOrMagnet)
        }
        if (!torboxConfigured && rdConfigured) {
            Log.d(tag, "Resolving via Real-Debrid (single configured provider)...")
            return@coroutineScope realDebridProvider.resolveStream(queryOrMagnet)
        }

        // Both are configured: Ultra-Fast Concurrent Racing
        // Execute both providers in parallel and return the first success immediately
        Log.d(tag, "Concurrent Debrid Racing: Resolving via Torbox & Real-Debrid simultaneously...")
        val resultChannel = Channel<DebridResult>(capacity = Channel.BUFFERED)

        val jobTorbox = launch {
            try {
                val res = torboxProvider.resolveStream(queryOrMagnet)
                resultChannel.send(res)
            } catch (e: Exception) {
                resultChannel.send(DebridResult.Error(e.message ?: "Torbox failed", null, "Torbox"))
            }
        }

        val jobRd = launch {
            try {
                val res = realDebridProvider.resolveStream(queryOrMagnet)
                resultChannel.send(res)
            } catch (e: Exception) {
                resultChannel.send(DebridResult.Error(e.message ?: "Real-Debrid failed", null, "Real-Debrid"))
            }
        }

        var firstError: DebridResult.Error? = null
        var completedCount = 0

        while (completedCount < 2) {
            val result = resultChannel.receive()
            completedCount++

            if (result is DebridResult.Success) {
                // Cancel the slower/pending job immediately
                jobTorbox.cancel()
                jobRd.cancel()
                Log.d(tag, "Speed Race WON by ${result.providerName}!")
                return@coroutineScope result
            } else if (result is DebridResult.Error) {
                if (firstError == null) {
                    firstError = result
                }
            }
        }

        return@coroutineScope firstError ?: DebridResult.Error(
            message = "Failed to resolve stream with configured Debrid providers",
            providerName = "Debrid"
        )
    }
}

