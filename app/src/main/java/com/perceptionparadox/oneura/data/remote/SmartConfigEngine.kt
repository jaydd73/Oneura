package com.perceptionparadox.oneura.data.remote

import android.util.Log
import com.perceptionparadox.oneura.data.remote.api.OneuraAiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class SmartConfigEngine(
    private val onUpdate: (url: String, model: String?) -> Unit
) {
    private val candidates = listOf(
        "http://127.0.0.1:11434",  // Local / Termux
        "http://10.0.2.2:11434",   // Emulator
        "http://localhost:11434",  // Local alias
        "http://127.0.0.1:8000"    // Reverse Proxy
    )

    suspend fun runDiscovery() = withContext(Dispatchers.IO) {
        InternalLogger.log("SYS", "Smart Discovery: Scanning 4 locations...")
        
        var foundUrl: String? = null
        var foundModel: String? = null

        for (baseUrl in candidates) {
            try {
                InternalLogger.log("SYS", "Probing $baseUrl...")
                
                val cleanUrl = baseUrl.replace("http://", "").replace("https://", "")
                val parts = cleanUrl.split(":")
                val host = parts[0]
                val port = if (parts.size > 1) parts[1].split("/")[0].toInt() else 11434
                
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(host, port), 2000)
                    socket.close()
                    InternalLogger.log("SYS", "TCP Ping Success: $baseUrl")
                } catch (e: Exception) {
                    InternalLogger.log("SYS", "TCP Ping Failed: $baseUrl (${e.message})")
                    continue
                }

                // If TCP worked, try the API
                val tempClient = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                
                val tempService = Retrofit.Builder()
                    .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                    .client(tempClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(OneuraAiApi::class.java)
                
                val response = tempService.getModels()
                if (response.isSuccessful && !response.body()?.models.isNullOrEmpty()) {
                    foundUrl = baseUrl
                    foundModel = response.body()?.models?.firstOrNull()?.name
                    InternalLogger.log("SYS", "Ollama found! Using model: $foundModel")
                    break
                } else {
                    InternalLogger.log("ERR", "Ollama present at $baseUrl but returned no models.")
                }
            } catch (e: Exception) {
                InternalLogger.log("ERR", "API check failed at $baseUrl: ${e.message}")
            }
        }

        if (foundUrl != null) {
            onUpdate(foundUrl, foundModel)
        } else {
            InternalLogger.log("ERR", "Discovery Finished: No Ollama found. Is it running in Termux?")
        }
    }
}
