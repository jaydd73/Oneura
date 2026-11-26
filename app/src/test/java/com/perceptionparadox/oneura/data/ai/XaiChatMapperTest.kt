package com.perceptionparadox.oneura.data.ai

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

class XaiChatMapperTest {

    @Test
    fun requestUsesOpenAiChatShapeAndNeverCarriesAKey() {
        val json = XaiChatMapper.buildRequestJson(
            model = XaiModels.DEFAULT_COMPANION,
            systemPrompt = "Be Oneura.",
            userPrompt = "Hello"
        )
        val root = JsonParser.parseString(json).asJsonObject
        assertEquals("grok-4.6", root.get("model").asString)
        assertFalse(root.get("stream").asBoolean)
        assertFalse(json.contains("api_key"))
        assertFalse(json.contains("Authorization"))
        assertFalse(json.contains("Bearer"))

        val messages = root.getAsJsonArray("messages")
        assertEquals(2, messages.size())
        assertEquals("system", messages[0].asJsonObject.get("role").asString)
        assertEquals("Be Oneura.", messages[0].asJsonObject.get("content").asString)
        assertEquals("user", messages[1].asJsonObject.get("role").asString)
        assertEquals("Hello", messages[1].asJsonObject.get("content").asString)
        assertFalse(root.has("stop"))
        assertFalse(root.has("presence_penalty"))
        assertFalse(root.has("frequency_penalty"))
    }

    @Test
    fun companionAndBackgroundModelsResolveToCurrentIds() {
        assertEquals(
            "grok-4.6",
            XaiModels.resolveModel(GenerationPurpose.COMPANION, "", "")
        )
        assertEquals(
            "grok-4.3",
            XaiModels.resolveModel(GenerationPurpose.BACKGROUND, "", "  ")
        )
        assertEquals(
            "grok-4.7",
            XaiModels.resolveModel(GenerationPurpose.COMPANION, " grok-4.7 ", "grok-4.3")
        )
        assertEquals(
            "custom-fast",
            XaiModels.resolveModel(GenerationPurpose.BACKGROUND, "grok-4.6", "custom-fast")
        )
        assertTrue(XaiModels.isDiscouraged("grok-4-1-fast"))
        assertTrue(XaiModels.isDiscouraged("grok-latest"))
        assertFalse(XaiModels.isDiscouraged("grok-4.6"))
        assertFalse(XaiModels.isDiscouraged("grok-4.3"))
    }

    @Test
    fun parsesStringContent() {
        val body = """
            {
              "id": "abc",
              "object": "chat.completion",
              "model": "grok-4.6",
              "choices": [
                {
                  "index": 0,
                  "message": {"role": "assistant", "content": "Hello from Grok"},
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()
        assertEquals("Hello from Grok", XaiChatMapper.parseAssistantText(body))
    }

    @Test
    fun parsesContentPartsArray() {
        val body = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": [
                      {"type": "text", "text": "Hello "},
                      {"type": "text", "text": "there"}
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        assertEquals("Hello there", XaiChatMapper.parseAssistantText(body))
    }

    @Test
    fun emptyContentWithRefusalIsAnError() {
        val body = """
            {"choices":[{"message":{"role":"assistant","content":null,"refusal":"no"}}]}
        """.trimIndent()
        val error = runCatching { XaiChatMapper.parseAssistantText(body) }.exceptionOrNull()
        assertTrue(error is XaiCallException)
        assertTrue(error!!.message!!.contains("refused"))
    }

    @Test
    fun missingChoicesIsAnError() {
        val error = runCatching { XaiChatMapper.parseAssistantText("{}") }.exceptionOrNull()
        assertTrue(error is XaiCallException)
        assertTrue(error!!.message!!.contains("no choices"))
    }

    @Test
    fun httpStatusMessagesCoverAuthBillingAndRateLimit() {
        val unauthorized = XaiChatMapper.messageForHttp(401, """{"error":{"message":"invalid key"}}""")
        assertTrue(unauthorized.contains("rejected the API key"))
        assertTrue(unauthorized.contains("invalid key"))

        val billing = XaiChatMapper.messageForHttp(402, null)
        assertTrue(billing.contains("credits"))
        assertFalse(billing.contains("null"))

        val limited = XaiChatMapper.messageForHttp(429, """{"error":"slow down"}""")
        assertTrue(limited.contains("rate limit"))
        assertTrue(limited.contains("slow down"))

        val missingModel = XaiChatMapper.messageForHttp(404, "")
        assertTrue(missingModel.contains("grok-4.3"))
    }

    @Test
    fun httpErrorRedactsKeyMaterial() {
        val secret = "xai-supersecretvalue"
        val body = """{"error":{"message":"rejected $secret"}}"""
        val message = XaiChatMapper.messageForHttp(401, body, secret)
        assertFalse(message.contains(secret))
        assertFalse(message.contains("supersecret"))
        assertTrue(message.contains("[redacted]"))
        assertTrue(message.contains("rejected the API key"))
    }

    @Test
    fun transportErrorsAreSpecific() {
        assertTrue(
            XaiChatMapper.messageForTransport(SocketTimeoutException("timed out")).contains("timed out")
        )
        assertTrue(
            XaiChatMapper.messageForTransport(UnknownHostException("api.x.ai")).contains("Can't reach")
        )
        assertTrue(
            XaiChatMapper.messageForTransport(ConnectException("refused")).contains("Can't reach")
        )
        assertTrue(
            XaiChatMapper.messageForTransport(IOException("reset")).contains("Network error")
        )
    }
}
