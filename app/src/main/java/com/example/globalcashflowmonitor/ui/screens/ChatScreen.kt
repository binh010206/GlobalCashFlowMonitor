package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class MessageModel(
    val id: String, val senderName: String, val content: String,
    val isMe: Boolean, val type: String = "TEXT", val countryId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(roomId: String, roomName: String, isDarkMode: Boolean, onBackClick: () -> Unit, onNavigateToMap: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

    val myName = sharedPref.getString("USER_NAME", "Khách Ẩn Danh") ?: "Khách Ẩn Danh"
    val myEmail = sharedPref.getString("USER_EMAIL", "guest@unknown.com") ?: "guest@unknown.com"

    val bgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val topBarColor = if (isDarkMode) Color(0xFF161622) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val borderColor = if (isDarkMode) Color(0xFF2A2A35) else Color(0xFFE5E7EB)

    var messages by remember { mutableStateOf<List<MessageModel>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun fetchMessages() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://globalcashflowbackend.onrender.com/api/rooms/$roomId/messages")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("data")
                    val msgs = mutableListOf<MessageModel>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        msgs.add(
                            MessageModel(
                                id = item.getString("_id"), senderName = item.getString("senderName"),
                                content = item.getString("content"), isMe = (item.optString("senderEmail", "") == myEmail),
                                type = item.optString("type", "TEXT"), countryId = item.optString("countryId", null)
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        if (messages.size != msgs.size) {
                            messages = msgs
                            launch { if(messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(roomId) {
        while (true) { fetchMessages(); delay(3000) }
    }

    // CHÚ Ý CHỖ NÀY ĐÃ THÊM PADDING BOTTOM 90.DP ĐỂ KHÔNG BỊ ĐÈ THANH MENU
    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(bottom = 90.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(topBarColor).border(1.dp, borderColor).padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Rounded.ArrowBack, "Quay lại", tint = textColor) }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF00B8D4)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Public, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(roomName, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Phòng công cộng trực tuyến", color = Color(0xFF00E676), fontSize = 11.sp)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)) {
            items(messages) { msg ->
                when (msg.type) {
                    "SYSTEM" -> SystemMessage(msg.content, isDarkMode)
                    "DATA_CARD" -> DataCardMessage(msg, isDarkMode) { countryId -> onNavigateToMap(countryId) }
                    else -> ChatBubble(msg, isDarkMode)
                }
            }
        }

        val inputBg = if (isDarkMode) Color(0xFF1E1E28) else Color.White
        Row(modifier = Modifier.fillMaxWidth().background(topBarColor).border(1.dp, borderColor).padding(12.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                placeholder = { Text("Gửi tin nhắn dưới tên $myName...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = inputBg, unfocusedContainerColor = inputBg, focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = borderColor, focusedTextColor = textColor, unfocusedTextColor = textColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText; inputText = ""
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL("https://globalcashflowbackend.onrender.com/api/rooms/$roomId/messages")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.setRequestProperty("Content-Type", "application/json")
                                conn.doOutput = true
                                val param = JSONObject().apply { put("senderName", myName); put("senderEmail", myEmail); put("content", textToSend); put("type", "TEXT") }
                                OutputStreamWriter(conn.outputStream).use { it.write(param.toString()); it.flush() }
                                if (conn.responseCode == 200) { fetchMessages() }
                            } catch (e: Exception) { }
                        }
                    }
                },
                containerColor = Color(0xFF00B8D4), modifier = Modifier.size(48.dp), elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) { Icon(Icons.Rounded.Send, "Gửi", tint = Color.White) }
        }
    }
}

@Composable
fun SystemMessage(text: String, isDarkMode: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val bg = if (isDarkMode) Color(0xFF333344) else Color(0xFFE5E7EB)
        Text(text = text, color = if (isDarkMode) Color.LightGray else Color.DarkGray, fontSize = 12.sp, fontStyle = FontStyle.Italic, modifier = Modifier.background(bg, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun ChatBubble(msg: MessageModel, isDarkMode: Boolean) {
    val bubbleColor = if (msg.isMe) Color(0xFF00B8D4) else if (isDarkMode) Color(0xFF2C2C3A) else Color.White
    val textColor = if (msg.isMe) Color.White else if (isDarkMode) Color.White else Color.Black
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start) {
        if (!msg.isMe) Text(msg.senderName, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Surface(color = bubbleColor, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (msg.isMe) 16.dp else 0.dp, bottomEnd = if (msg.isMe) 0.dp else 16.dp), shadowElevation = if (isDarkMode) 0.dp else 2.dp) {
            Text(text = msg.content, color = textColor, fontSize = 15.sp, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun DataCardMessage(msg: MessageModel, isDarkMode: Boolean, onClick: (String) -> Unit) {
    val bg = if (isDarkMode) Color(0xFF161622) else Color.White
    val border = if (isDarkMode) Color(0xFF00E676) else Color(0xFF00C853)
    val textCol = if (isDarkMode) Color.White else Color.Black
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(msg.senderName, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Column(modifier = Modifier.width(260.dp).background(bg, RoundedCornerShape(16.dp)).border(1.dp, border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).clickable { msg.countryId?.let { onClick(it) } }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Public, null, tint = border)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DỮ LIỆU BẢN ĐỒ", color = border, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg.content, color = textCol, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}