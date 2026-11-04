package com.perceptionparadox.oneura.data.remote

import com.perceptionparadox.oneura.data.remote.api.OneuraAiApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.compose.runtime.mutableStateListOf

object NetworkModule {

    private var currentBaseUrl: String = "http://127.0.0.1:11434/"
    private var currentTimeout: Long = 60

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OneuraNetwork", message)
        InternalLogger.log("NET", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var okHttpClient = buildClient(currentTimeout)
    private var retrofit = buildRetrofit(currentBaseUrl, okHttpClient)

    var apiService: OneuraAiApi = retrofit.create(OneuraAiApi::class.java)
        private set

    fun updateConfig(baseUrl: String, timeoutSeconds: Long) {
        try {
            var trimmedUrl = baseUrl.trim()
            
            // Emergency Fix: If URLs got mashed together (e.g. http://...http://...), take only the first one
            if (trimmedUrl.count { it == ':' } > 2 && trimmedUrl.contains("http", ignoreCase = true)) {
                val secondHttp = trimmedUrl.indexOf("http", startIndex = 7, ignoreCase = true)
                if (secondHttp != -1) {
                    trimmedUrl = trimmedUrl.substring(0, secondHttp).trim()
                }
            }

            if (currentBaseUrl == trimmedUrl && currentTimeout == timeoutSeconds) return
            
            Log.d("OneuraNetwork", "Updating Config: URL=$trimmedUrl, Timeout=$timeoutSeconds")
            InternalLogger.log("SYS", "Updating Network Config: $trimmedUrl")
            
            currentBaseUrl = if (trimmedUrl.endsWith("/")) trimmedUrl else "$trimmedUrl/"
            currentTimeout = timeoutSeconds
            
            okHttpClient = buildClient(currentTimeout)
            retrofit = buildRetrofit(currentBaseUrl, okHttpClient)
            apiService = retrofit.create(OneuraAiApi::class.java)
        } catch (e: Exception) {
            Log.e("OneuraNetwork", "Failed to update config with URL: $baseUrl", e)
            InternalLogger.log("ERR", "Invalid URL format. Please check settings.")
        }
    }

    private fun buildClient(timeout: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

object InternalLogger {
    private val _logs = mutableStateListOf<LogEntry>()
    val logs: List<LogEntry> = _logs

    fun log(tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), tag, message)
        _logs.add(0, entry)
        if (_logs.size > 500) _logs.removeAt(_logs.size - 1)
    }

    fun clear() {
        _logs.clear()
    }

    data class LogEntry(val timestamp: Long, val tag: String, val message: String)
}
