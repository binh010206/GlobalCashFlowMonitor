package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val text: String, val isUser: Boolean)
data class ChatSession(val id: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(isLoggedIn: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }
    val userEmail = sharedPref.getString("USER_EMAIL", "") ?: ""

    // State quản lý luồng hiện tại
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var currentSessionTitle by remember { mutableStateOf("Cuộc trò chuyện mới") }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val sessionList = remember { mutableStateListOf<ChatSession>() }

    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    // HÀM 1: Lấy danh sách các Tiêu đề gói
    fun fetchSessionList() {
        if (!isLoggedIn) return
        isLoadingHistory = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://globalcashflowbackend.onrender.com/api/ai/sessions?email=$userEmail")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("data")
                    val fetchedSessions = mutableListOf<ChatSession>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        fetchedSessions.add(ChatSession(item.getString("_id"), item.getString("title")))
                    }
                    withContext(Dispatchers.Main) {
                        sessionList.clear()
                        sessionList.addAll(fetchedSessions)
                        isLoadingHistory = false
                    }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main){ isLoadingHistory = false } }
        }
    }

    // HÀM 2: Lấy tin nhắn của 1 gói cụ thể khi người dùng bấm vào tiêu đề
    fun loadMessagesForSession(sessionId: String, title: String) {
        currentSessionId = sessionId
        currentSessionTitle = title
        showHistorySheet = false // Đóng bảng lịch sử
        messages.clear()
        messages.add(ChatMessage("Đang tải dữ liệu...", false))

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://globalcashflowbackend.onrender.com/api/ai/sessions/$sessionId/messages")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("data")
                    val fetchedMsgs = mutableListOf<ChatMessage>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        fetchedMsgs.add(ChatMessage(item.getString("content"), item.getString("role") == "user"))
                    }
                    withContext(Dispatchers.Main) {
                        messages.clear()
                        messages.addAll(fetchedMsgs)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { messages.clear(); messages.add(ChatMessage("Lỗi tải tin nhắn!", false)) }
            }
        }
    }

    // GIAO DIỆN CHÍNH
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 30.dp, bottom = 90.dp)) {

            // --- TOP BAR ---
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentSessionTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (isLoggedIn) "Chế độ: VIP (Đã đồng bộ mây)" else "Chế độ: Khách", color = Color.LightGray, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // NÚT LỊCH SỬ
                    if (isLoggedIn) {
                        IconButton(
                            onClick = { fetchSessionList(); showHistorySheet = true },
                            modifier = Modifier.background(Color(0xFF1E1E2E), CircleShape).size(40.dp)
                        ) { Icon(Icons.Rounded.History, "Lịch sử", tint = Color.Cyan) }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // NÚT TẠO CUỘC TRÒ CHUYỆN MỚI
                    IconButton(
                        onClick = {
                            currentSessionId = null
                            currentSessionTitle = "Cuộc trò chuyện mới"
                            messages.clear()
                            messages.add(ChatMessage("Xin chào! Bạn muốn phân tích hay thảo luận về chủ đề gì?", false))
                        },
                        modifier = Modifier.background(Color(0xFF00B8D4), CircleShape).size(40.dp)
                    ) { Icon(Icons.Rounded.Add, "Tạo mới", tint = Color.Black) }
                }
            }
            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 16.dp))

            // KHUNG HIỂN THỊ TIN NHẮN
            LazyColumn(modifier = Modifier.weight(1.0f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { msg ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                        Surface(color = if (msg.isUser) Color.Cyan else Color(0xFF2C2C2C), shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                            Text(text = msg.text, color = if (msg.isUser) Color.Black else Color.White, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Ô NHẬP TIN NHẮN
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it },
                    placeholder = { Text("Hỏi AI...", color = Color.Gray) },
                    modifier = Modifier.weight(1.0f), shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E))
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.trim().isNotEmpty()) {
                            val q = inputText; messages.add(ChatMessage(q, true)); inputText = ""; messages.add(ChatMessage("Đang phân tích...", false))

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    // BƯỚC 1: Nếu chưa có Session, phải gọi API tạo Session trước
                                    if (currentSessionId == null && isLoggedIn) {
                                        val createUrl = URL("https://globalcashflowbackend.onrender.com/api/ai/sessions")
                                        val createConn = createUrl.openConnection() as HttpURLConnection
                                        createConn.requestMethod = "POST"
                                        createConn.setRequestProperty("Content-Type", "application/json")
                                        createConn.doOutput = true
                                        val param = JSONObject().apply { put("email", userEmail); put("firstMessage", q) }
                                        OutputStreamWriter(createConn.outputStream).use { it.write(param.toString()); it.flush() }

                                        if (createConn.responseCode == 200) {
                                            val res = createConn.inputStream.bufferedReader().use { it.readText() }
                                            val data = JSONObject(res).getJSONObject("data")
                                            currentSessionId = data.getString("_id")
                                            currentSessionTitle = data.getString("title")
                                        }
                                    }

                                    // BƯỚC 2: Gọi API lưu tin nhắn và lấy câu trả lời
                                    val chatUrl = URL("https://globalcashflowbackend.onrender.com/api/ai/chat-in-session")
                                    val chatConn = chatUrl.openConnection() as HttpURLConnection
                                    chatConn.requestMethod = "POST"
                                    chatConn.setRequestProperty("Content-Type", "application/json")
                                    chatConn.doOutput = true

                                    val chatParam = JSONObject().apply {
                                        put("sessionId", currentSessionId ?: "guest_session") // Gửi kèm ID gói
                                        put("message", q)
                                    }
                                    OutputStreamWriter(chatConn.outputStream).use { it.write(chatParam.toString()); it.flush() }

                                    if (chatConn.responseCode == 200) {
                                        val res = chatConn.inputStream.bufferedReader().use { it.readText() }
                                        val aiReply = JSONObject(res).getJSONObject("data").getString("reply")

                                        withContext(Dispatchers.Main) {
                                            messages.removeAt(messages.size - 1)
                                            messages.add(ChatMessage(aiReply, false))
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { messages.removeAt(messages.size - 1); messages.add(ChatMessage("Lỗi mạng!", false)) }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Cyan)
                ) { Icon(Icons.Rounded.Send, "Gửi", tint = Color.Black) }
            }
        }

        // GIAO DIỆN BOTTOM SHEET: DANH SÁCH CÁC GÓI CHAT
        if (showHistorySheet && isLoggedIn) {
            ModalBottomSheet(onDismissRequest = { showHistorySheet = false }, containerColor = Color(0xFF161622)) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                    Text("Gần đây", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    if (isLoadingHistory) {
                        CircularProgressIndicator(color = Color.Cyan, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (sessionList.isEmpty()) {
                        Text("Chưa có lịch sử hội thoại nào.", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                            items(sessionList) { session ->
                                val isSelected = session.id == currentSessionId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF2A2A35) else Color.Transparent)
                                        .clickable { loadMessagesForSession(session.id, session.title) }
                                        .padding(vertical = 14.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.title,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}