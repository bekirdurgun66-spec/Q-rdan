package com.example.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiApiService {
    @POST("generateContent")
    suspend fun generateContent(
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}
