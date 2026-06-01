package com.example.globalcashflowmonitor.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// ==========================================
// 1. CẤU TRÚC DỮ LIỆU CHO CHATBOT AI (CŨ)
// ==========================================
data class ChatRequest(val message: String)
data class AiCommand(val reply: String, val action: String, val targetId: String)
data class ChatResponse(val success: Boolean, val data: AiCommand)

// ==========================================
// 2. CẤU TRÚC DỮ LIỆU CHO BẢN ĐỒ NHIỆT (MỚI)
// ==========================================
data class SingleResponse(val success: Boolean, val data: CountryData)
data class AllDataResponse(val success: Boolean, val data: List<CountryData>) // Hứng 36 nước

data class CountryData(val countryId: String, val countryName: String, val metrics: List<MetricTimeline>)
data class MetricTimeline(val name: String, val unit: String, val history: List<YearlyNode>)
data class YearlyNode(val year: Int, val value: Float)

// ==========================================
// 3. ĐỊNH NGHĨA CÁC ĐƯỜNG DẪN API
// ==========================================
interface ApiService {
    // Gọi con AI
    @POST("api/chat")
    fun sendAiMessage(@Body request: ChatRequest): Call<ChatResponse>

    // Lấy data 1 nước (Khi bấm vào cờ 1 nước cụ thể)
    @GET("api/analytics/{countryCode}")
    suspend fun getCountryData(@Path("countryCode") countryCode: String): SingleResponse

    // Lấy data TOÀN CẦU (Dùng để vẽ Bản đồ nhiệt)
    @GET("api/analytics")
    suspend fun getAllData(): AllDataResponse
}

// ==========================================
// 4. KHỞI TẠO ĐƯỜNG ỐNG RETROFIT
// ==========================================
object RetrofitClient {
    // Dùng đúng link Render của mày
    private const val BASE_URL = "https://globalcashflowbackend.onrender.com/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}