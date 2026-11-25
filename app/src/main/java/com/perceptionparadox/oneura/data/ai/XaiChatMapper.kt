package com.perceptionparadox.oneura.data.ai

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * OpenAI-compatible chat-completions mapping for https://api.x.ai/v1/chat/completions.
 * Pure JVM so request and response handling can be unit tested without Android.
 * The API key is never placed in the body and is stripped if an error echoes one.
 */
object XaiChatMapper {
    private val secretPattern = Regex("""(?i)(?:bearer\s+|sk-|xai-)[A-Za-z0-9_\-]{6,}""")

    fun buildRequestJson(model: String, systemPrompt: String, userPrompt: String): String {
        val messages = JsonArray()
        messages.add(message("system", systemPrompt))
        messages.add(message("user", userPrompt))
        val root = JsonObject()
        root.addProperty("model", model)
        root.add("messages", messages)
        root.addProperty("stream", false)
        return root.toString()
    }

    fun parseAssistantText(body: String): String {
        val root = try {
            val parsed = JsonParser.parseString(body)
            if (!parsed.isJsonObject) throw XaiCallException("xAI returned a response that was not a JSON object.")
            parsed.asJsonObject
        } catch (e: XaiCallException) {
            throw e
        } catch (e: Exception) {
            throw XaiCallException("xAI returned a response that was not valid JSON.")
        }

        val choicesElement = root.get("choices")
        if (choicesElement == null || !choicesElement.isJsonArray || choicesElement.asJsonArray.size() == 0) {
            throw XaiCallException("xAI returned no choices.")
        }
        val first = choicesElement.asJsonArray[0]
        if (!first.isJsonObject) throw XaiCallException("xAI returned an unreadable choice.")
        val message = first.asJsonObject.get("message")
        if (message == null || !message.isJsonObject) {
            throw XaiCallException("xAI returned a choice with no message.")
        }
        val messageObject = message.asJsonObject
        val text = readContent(messageObject.get("content")).trim()
        if (text.isNotEmpty()) return text

        val refusal = messageObject.get("refusal")
            ?.takeUnless { it.isJsonNull }
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
        if (!refusal.isNullOrBlank()) {
            throw XaiCallException("xAI refused the request: ${redact(refusal)}")
        }
        throw XaiCallException("xAI returned an empty reply.")
    }

    fun messageForHttp(code: Int, errorBody: String?, secret: String? = null): String {
        val detail = extractErrorDetail(errorBody, secret)
        val base = when (code) {
            401 -> "xAI rejected the API key. Check the key in Settings. You can create one at console.x.ai."
            402 -> "xAI declined the request because the account needs credits or a paid plan. Check billing at console.x.ai."
            403 -> "xAI refused access for this key or team. Check the key's permissions in the xAI console."
            404 -> "xAI could not find that model. Check the model names in Settings. Current defaults are grok-4.6 and grok-4.3."
            429 -> "xAI rate limit reached. Wait a moment, then try again."
            in 500..599 -> "xAI had a server error ($code). Try again shortly."
            else -> "xAI request failed ($code)."
        }
        return if (detail.isNullOrBlank()) base else "$base $detail"
    }

    fun messageForTransport(error: IOException): String = when (error) {
        is SocketTimeoutException -> "xAI timed out. Check your connection, or raise the timeout in Settings."
        is UnknownHostException, is ConnectException -> "Can't reach api.x.ai. Check your network connection."
        else -> "Network error talking to xAI. Check your connection and try again."
    }

    fun redact(text: String, secret: String? = null): String {
        var cleaned = text
        val key = secret?.trim().orEmpty()
        if (key.length >= 4) cleaned = cleaned.replace(key, "[redacted]")
        return secretPattern.replace(cleaned, "[redacted]")
    }

    private fun extractErrorDetail(errorBody: String?, secret: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        val raw = try {
            val parsed = JsonParser.parseString(errorBody)
            if (!parsed.isJsonObject) errorBody
            else {
                val root = parsed.asJsonObject
                val err = root.get("error")
                when {
                    err == null || err.isJsonNull ->
                        root.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                    err.isJsonPrimitive -> err.asString
                    err.isJsonObject -> {
                        val obj = err.asJsonObject
                        obj.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: obj.get("code")?.takeIf { it.isJsonPrimitive }?.asString
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            errorBody
        }
        if (raw.isNullOrBlank()) return null
        val redacted = redact(raw, secret).replace('\n', ' ').trim()
        if (redacted.isEmpty()) return null
        return if (redacted.length <= 180) redacted else redacted.take(179).trimEnd() + "…"
    }

    private fun readContent(content: JsonElement?): String {
        if (content == null || content.isJsonNull) return ""
        if (content.isJsonPrimitive) return content.asString
        if (!content.isJsonArray) return ""
        return content.asJsonArray.joinToString("") { element ->
            when {
                element.isJsonNull -> ""
                element.isJsonPrimitive -> element.asString
                element.isJsonObject ->
                    element.asJsonObject.get("text")?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                else -> ""
            }
        }
    }

    private fun message(role: String, content: String): JsonObject {
        val obj = JsonObject()
        obj.addProperty("role", role)
        obj.addProperty("content", content)
        return obj
    }
}

class XaiCallException(message: String) : Exception(message)
