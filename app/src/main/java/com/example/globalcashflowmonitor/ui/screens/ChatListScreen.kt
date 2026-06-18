package com.example.globalcashflowmonitor.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.Locale

data class ChatRoom(val id: String, val name: String, val lastMessage: String, val time: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(isDarkMode: Boolean, onNavigateToRoom: (String, String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF161622) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val borderColor = if (isDarkMode) Color(0xFF2A2A35) else Color(0xFFE5E7EB)

    var chatRooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    fun fetchRooms() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://globalcashflowbackend.onrender.com/api/rooms")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("data")
                    val rooms = mutableListOf<ChatRoom>()
                    val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val dateString = item.getString("createdAt")
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(dateString)
                        val formattedTime = if (date != null) sdf.format(date) else "N/A"
                        rooms.add(ChatRoom(item.getString("_id"), item.getString("name"), "Nhấn để tham gia thảo luận...", formattedTime))
                    }
                    withContext(Dispatchers.Main) { chatRooms = rooms; isLoading = false }
                } else {
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isLoading = false } }
        }
    }

    LaunchedEffect(Unit) { fetchRooms() }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm tên phòng...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().background(cardBg).padding(top = 40.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = Color.Cyan),
                    trailingIcon = { IconButton(onClick = { isSearching = false; searchQuery = "" }) { Icon(Icons.Rounded.Close, "Đóng", tint = textColor) } }
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth().background(cardBg).padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cộng Đồng Vĩ Mô", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isSearching = true }) { Icon(Icons.Rounded.Search, "Tìm kiếm", tint = textColor) }
                }
            }
            HorizontalDivider(color = borderColor)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.Cyan) }
            } else {
                val filteredRooms = chatRooms.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (filteredRooms.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Forum, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (chatRooms.isEmpty()) "Chưa có kênh nào" else "Không tìm thấy phòng", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredRooms) { room ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToRoom(room.id, room.name) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF00B8D4)), contentAlignment = Alignment.Center) {
                                    Text(room.name.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(room.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(room.lastMessage, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(room.time, color = Color.Gray, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = borderColor, modifier = Modifier.padding(start = 82.dp))
                        }
                    }
                }
            }
        }

        FloatingActionButton(onClick = { showCreateGroupSheet = true }, containerColor = Color(0xFF00E676), modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 90.dp, end = 16.dp)) {
            Icon(Icons.Rounded.AddComment, "Tạo nhóm", tint = Color.Black)
        }

        if (showCreateGroupSheet) {
            var newRoomName by remember { mutableStateOf("") }
            var isCreating by remember { mutableStateOf(false) }

            ModalBottomSheet(onDismissRequest = { showCreateGroupSheet = false }, containerColor = cardBg) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                    Text("Tạo Kênh Thảo Luận Mới", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newRoomName, onValueChange = { newRoomName = it }, label = { Text("Tên chủ đề / Kênh") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!isCreating) { // Cho phép tạo dù tên rỗng (Backend sẽ lo)
                                isCreating = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val url = URL("https://globalcashflowbackend.onrender.com/api/rooms")
                                        val conn = url.openConnection() as HttpURLConnection
                                        conn.requestMethod = "POST"
                                        conn.setRequestProperty("Content-Type", "application/json")
                                        conn.connectTimeout = 15000
                                        conn.doOutput = true

                                        val param = JSONObject().apply { put("name", newRoomName.trim()) }
                                        OutputStreamWriter(conn.outputStream).use { it.write(param.toString()); it.flush() }

                                        val resCode = conn.responseCode
                                        val responseStr = if (resCode == 200 || resCode == 201) {
                                            conn.inputStream.bufferedReader().use { it.readText() }
                                        } else {
                                            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                                        }

                                        withContext(Dispatchers.Main) {
                                            isCreating = false
                                            if (resCode == 200 || resCode == 201) {
                                                showCreateGroupSheet = false; fetchRooms()
                                            } else {
                                                // BÓC TÁCH LỖI JSON ĐỂ HIỆN LÊN APP XỊN XÒ NHƯ THẬT
                                                try {
                                                    val errorMsg = JSONObject(responseStr).getString("message")
                                                    Toast.makeText(context, "Lỗi: $errorMsg", Toast.LENGTH_LONG).show()
                                                } catch(e: Exception) {
                                                    Toast.makeText(context, "Lỗi Server không xác định!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isCreating = false
                                            Toast.makeText(context, "Lỗi mạng hoặc Server ngắt kết nối!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4))
                    ) { Text(if (isCreating) "ĐANG KHỞI TẠO..." else "XÁC NHẬN TẠO KÊNH", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}