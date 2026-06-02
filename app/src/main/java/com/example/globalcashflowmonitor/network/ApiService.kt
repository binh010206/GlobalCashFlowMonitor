package com.example.globalcashflowmonitor.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Data class cho AI
data class ChatRequest(val message: String)
data class AiCommand(val reply: String, val action: String, val targetId: String)
data class ChatResponse(val success: Boolean, val data: AiCommand)

// ==========================================
// CẤU TRÚC ĐÃ ĐƯỢC SỬA LẠI ĐỂ KHỚP 100% VỚI MONGODB
// ==========================================
data class HistoryData(
    val year: Int,
    val value: Double
)

data class MetricItem(
    val name: String,
    val unit: String?,
    val history: List<HistoryData>
)

data class CountryTimeline(
    val _id: String?,
    val countryId: String,
    val countryName: String,
    val metrics: List<MetricItem> // Mảng này chứa GDP, FDI...
)

data class TimelineResponse(
    val success: Boolean,
    val data: List<CountryTimeline>
)

// ==========================================
interface ApiService {
    @POST("api/chat")
    fun sendAiMessage(@Body request: ChatRequest): Call<ChatResponse>

    @GET("api/countrytimelines")
    suspend fun getAllTimelines(): TimelineResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://globalcashflowbackend.onrender.com/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}