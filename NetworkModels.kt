package com.perceptionparadox.oneura.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<NetworkMessage>,
    @SerializedName("stream") val stream: Boolean = false
)

data class NetworkMessage(
    @SerializedName("role") val role: String, // "system", "user", "assistant"
    @SerializedName("content") val content: String
)

data class ChatResponse(
    @SerializedName("model") val model: String,
    @SerializedName("message") val message: NetworkMessage,
    @SerializedName("done") val done: Boolean
)
