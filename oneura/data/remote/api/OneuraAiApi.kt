package com.perceptionparadox.oneura.data.remote.api

import com.perceptionparadox.oneura.data.remote.model.ChatRequest
import com.perceptionparadox.oneura.data.remote.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OneuraAiApi {

    @POST("api/chat")
    suspend fun generateChatCompletion(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
