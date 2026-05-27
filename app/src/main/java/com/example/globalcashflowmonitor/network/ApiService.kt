package com.example.globalcashflowmonitor.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. Định nghĩa cấu trúc dữ liệu nhận về từ API Chat của Node.js
data class ChatRequest(val message: String)
data class AiCommand(val reply: String, val action: String, val targetId: String)
data class ChatResponse(val success: Boolean, val data: AiCommand)

// 2. Định nghĩa các Endpoint
interface ApiService {
    @POST("api/chat")
    fun sendAiMessage(@Body request: ChatRequest): Call<ChatResponse>
}

// 3. Khởi tạo Object Retrofit Singleton (Dùng link Render của mày)
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