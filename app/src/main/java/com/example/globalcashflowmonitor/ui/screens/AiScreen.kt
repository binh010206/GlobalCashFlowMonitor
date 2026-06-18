package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(isLoggedIn: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isHistoryLoaded by remember { mutableStateOf(false) }

    // Quản lý Guest (Khách)
    var aiUsageLeft by remember { mutableStateOf(sharedPref.getInt("AI_USAGE_LEFT", 5)) }
    var lockoutStartTime by remember { mutableStateOf(sharedPref.getLong("LOCKOUT_START_TIME", 0L)) }
    var timeRemainingText by remember { mutableStateOf("") }

    // TẢI LỊCH SỬ TỪ MONGODB KHI MỞ MÀN HÌNH
    LaunchedEffect(Unit) {
        if (!isHistoryLoaded) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://globalcashflowbackend.onrender.com/api/chat/history")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 10000
                    if (conn.responseCode == 200) {
                        val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONObject(responseStr).getJSONArray("data")
                        val historyList = mutableListOf<ChatMessage>()
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            historyList.add(ChatMessage(item.getString("content"), item.getString("role") == "user"))
                        }
                        withContext(Dispatchers.Main) {
                            messages.clear()
                            if (historyList.isEmpty()) {
                                messages.add(ChatMessage("Xin chào! Tôi là Trợ lý AI. " + if (isLoggedIn) "Bạn đã đăng nhập, hãy sử dụng thả ga nhé!" else "Khách chưa đăng nhập được dùng 5 lần.", false))
                            } else {
                                messages.addAll(historyList)
                            }
                            isHistoryLoaded = true
                        }
                    }
                } catch (e: Exception) { }
            }
        }
    }

    // ĐẾM NGƯỢC 3 PHÚT
    LaunchedEffect(aiUsageLeft, lockoutStartTime, isLoggedIn) {
        if (!isLoggedIn && aiUsageLeft <= 0 && lockoutStartTime > 0) {
            while (true) {
                val timePassed = System.currentTimeMillis() - lockoutStartTime
                val cooldownTime = 3L * 60 * 1000 // 3 phút
                if (timePassed >= cooldownTime) {
                    aiUsageLeft = 5; lockoutStartTime = 0L; timeRemainingText = ""
                    sharedPref.edit().putInt("AI_USAGE_LEFT", 5).putLong("LOCKOUT_START_TIME", 0L).apply()
                    break
                } else {
                    val timeLeft = cooldownTime - timePassed
                    timeRemainingText = String.format("%02d:%02d", java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60)
                }
                delay(1000)
            }
        }
    }

    val canChat = isLoggedIn || aiUsageLeft > 0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 30.dp, bottom = 90.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Trợ lý AI", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = if (isLoggedIn) "Chế độ: VIP (Không giới hạn)" else if (canChat) "Chế độ: Khách (Guest)" else "MỞ KHÓA SAU: $timeRemainingText", color = if (canChat) Color.LightGray else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (!isLoggedIn) {
                Surface(color = if (canChat) Color(0xFF005555) else Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Text(text = if (canChat) "Còn lại: $aiUsageLeft/5" else "Đang khóa", color = if (canChat) Color.Cyan else Color.Red, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 16.dp))

        LazyColumn(modifier = Modifier.weight(1.0f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { msg ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(color = if (msg.isUser) Color.Cyan else Color(0xFF2C2C2C), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (msg.isUser) 16.dp else 0.dp, bottomEnd = if (msg.isUser) 0.dp else 16.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                        Text(text = msg.text, color = if (msg.isUser) Color.Black else Color.White, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it }, enabled = canChat,
                placeholder = { Text(if (canChat) "Hỏi AI..." else "Chờ $timeRemainingText", color = Color.Gray) },
                modifier = Modifier.weight(1.0f), shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E), disabledContainerColor = Color(0xFF1A1A1A), focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.trim().isNotEmpty() && canChat) {
                        val q = inputText; messages.add(ChatMessage(q, true)); inputText = ""; messages.add(ChatMessage("Đang phân tích...", false))
                        if (!isLoggedIn) {
                            aiUsageLeft -= 1; if (aiUsageLeft <= 0) lockoutStartTime = System.currentTimeMillis()
                            sharedPref.edit().putInt("AI_USAGE_LEFT", aiUsageLeft).putLong("LOCKOUT_START_TIME", lockoutStartTime).apply()
                        }
                        com.example.globalcashflowmonitor.network.RetrofitClient.instance.sendAiMessage(com.example.globalcashflowmonitor.network.ChatRequest(q)).enqueue(object : retrofit2.Callback<com.example.globalcashflowmonitor.network.ChatResponse> {
                            override fun onResponse(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, response: retrofit2.Response<com.example.globalcashflowmonitor.network.ChatResponse>) {
                                val aiData = response.body()?.data
                                if (aiData != null) { messages.removeAt(messages.size - 1); messages.add(ChatMessage(aiData.reply, false)) }
                            }
                            override fun onFailure(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, t: Throwable) {
                                messages.removeAt(messages.size - 1); messages.add(ChatMessage("Lỗi kết nối máy chủ AI!", false))
                            }
                        })
                    }
                },
                enabled = canChat && inputText.trim().isNotEmpty(), colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Cyan, disabledContainerColor = Color.DarkGray), modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Rounded.Send, "Gửi", tint = Color.Black) }
        }
    }
}