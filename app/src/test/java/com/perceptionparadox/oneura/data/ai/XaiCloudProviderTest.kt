package com.perceptionparadox.oneura.data.ai

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class XaiCloudProviderTest {
    private val server = MockWebServer()
    private val apiKey = "unit-test-key"

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        try {
            server.shutdown()
        } catch (_: Exception) {
            // The connection-failure test already stopped the server.
        }
    }

    @Test
    fun companionCallPostsChatCompletionsWithoutTheKeyInTheBody() = runBlocking {
        server.enqueue(MockResponse().setBody(completion("Hello from Grok")))
        val result = provider().generateResponse("Hi", "Be brief", GenerationPurpose.COMPANION)

        assertEquals("Hello from Grok", result.getOrThrow())
        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer $apiKey", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertFalse(body.contains(apiKey))
        assertTrue(body.contains("\"model\":\"grok-4.6\""))
        assertTrue(body.contains("\"role\":\"system\""))
        assertTrue(body.contains("Be brief"))
    }

    @Test
    fun backgroundCallUsesTheFastModel() = runBlocking {
        server.enqueue(MockResponse().setBody(completion("noted")))
        val result = provider(
            companion = "grok-4.6",
            background = "grok-4.3"
        ).generateResponse("Reflect", "JSON only", GenerationPurpose.BACKGROUND)

        assertTrue(result.isSuccess)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"model\":\"grok-4.3\""))
        assertFalse(body.contains("grok-4.6"))
    }

    @Test
    fun blankModelFieldsFallBackToDocumentedDefaults() = runBlocking {
        server.enqueue(MockResponse().setBody(completion("ok")))
        provider(companion = "  ", background = "").generateResponse(
            "Ping",
            "Sys",
            GenerationPurpose.BACKGROUND
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"model\":\"grok-4.3\""))
    }

    @Test
    fun unauthorizedAndBillingAndRateLimitStayUserFacing() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"bad $apiKey"}}"""))
        val unauthorized = provider().generateResponse("Hi", "Sys", GenerationPurpose.COMPANION)
        val unauthorizedMessage = unauthorized.exceptionOrNull()!!.message!!
        assertTrue(unauthorizedMessage.contains("rejected the API key"))
        assertFalse(unauthorizedMessage.contains(apiKey))

        server.enqueue(MockResponse().setResponseCode(402).setBody("""{"error":{"message":"out of credits"}}"""))
        val billing = provider().generateResponse("Hi", "Sys", GenerationPurpose.COMPANION)
        assertTrue(billing.exceptionOrNull()!!.message!!.contains("credits"))

        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"slow down"}"""))
        val limited = provider().generateResponse("Hi", "Sys", GenerationPurpose.COMPANION)
        assertTrue(limited.exceptionOrNull()!!.message!!.contains("rate limit"))
    }

    @Test
    fun missingKeyDoesNotOpenAConnection() = runBlocking {
        val result = provider(key = "  ").generateResponse("Hi", "Sys", GenerationPurpose.COMPANION)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Add an xAI API key"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun connectionFailureIsANetworkMessage() = runBlocking {
        val endpoint = server.url("/v1/chat/completions").toString()
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        server.shutdown()
        val result = XaiCloudProvider(
            apiKey = apiKey,
            companionModel = "grok-4.6",
            backgroundModel = "grok-4.3",
            timeoutSeconds = 10,
            endpoint = endpoint,
            httpClient = client
        ).generateResponse("Hi", "Sys", GenerationPurpose.COMPANION)
        val message = result.exceptionOrNull()!!.message!!
        assertTrue(message.contains("Can't reach") || message.contains("Network error"))
        assertFalse(message.contains(apiKey))
    }

    private fun provider(
        key: String = apiKey,
        companion: String = "grok-4.6",
        background: String = "grok-4.3"
    ): XaiCloudProvider {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        return XaiCloudProvider(
            apiKey = key,
            companionModel = companion,
            backgroundModel = background,
            timeoutSeconds = 10,
            endpoint = server.url("/v1/chat/completions").toString(),
            httpClient = client
        )
    }

    private fun completion(text: String) = """
        {"choices":[{"message":{"role":"assistant","content":"$text"}}]}
    """.trimIndent()
}
