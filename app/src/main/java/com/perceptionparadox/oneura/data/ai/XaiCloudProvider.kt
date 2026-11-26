package com.perceptionparadox.oneura.data.ai

import com.perceptionparadox.oneura.data.remote.InternalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * xAI chat completions. The key is sent only as an Authorization header to
 * api.x.ai and is never written to logs or to the request body.
 * This client has no HTTP logging interceptor, so headers are not dumped.
 */
class XaiCloudProvider(
    private val apiKey: String,
    private val companionModel: String,
    private val backgroundModel: String,
    private val timeoutSeconds: Long = 120L,
    private val endpoint: String = XaiModels.CHAT_COMPLETIONS_URL,
    private val httpClient: OkHttpClient? = null
) : AiProvider {

    override val providerName: String = "xAI (Grok)"

    override suspend fun generateResponse(
        prompt: String,
        systemPrompt: String,
        purpose: GenerationPurpose
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(XaiCallException("Add an xAI API key in Settings."))
        }

        val model = XaiModels.resolveModel(purpose, companionModel, backgroundModel)
        val payload = XaiChatMapper.buildRequestJson(model, systemPrompt, prompt)
        val client = httpClient ?: buildClient(timeoutSeconds)
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $cleanKey")
            .post(payload.toRequestBody(JSON))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body.string()
                if (!response.isSuccessful) {
                    val message = XaiChatMapper.messageForHttp(response.code, raw, cleanKey)
                    InternalLogger.log("ERR", "xAI HTTP ${response.code} for $model")
                    return@withContext Result.failure(XaiCallException(message))
                }
                try {
                    val text = XaiChatMapper.parseAssistantText(raw)
                    InternalLogger.log("SYS", "xAI replied with $model")
                    Result.success(text)
                } catch (e: XaiCallException) {
                    InternalLogger.log("ERR", e.message ?: "xAI response could not be read")
                    Result.failure(e)
                }
            }
        } catch (e: IOException) {
            val message = XaiChatMapper.messageForTransport(e)
            InternalLogger.log("ERR", message)
            Result.failure(XaiCallException(message))
        } catch (e: XaiCallException) {
            Result.failure(e)
        } catch (e: Exception) {
            InternalLogger.log("ERR", "xAI request failed")
            Result.failure(XaiCallException("xAI request failed."))
        }
    }

    private fun buildClient(timeoutSeconds: Long): OkHttpClient {
        val timeout = timeoutSeconds.coerceIn(10L, 300L)
        return OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
