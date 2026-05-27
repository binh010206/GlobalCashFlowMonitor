package com.example.globalcashflowmonitor.ui.screens

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MessageModel(
    val id: String,
    val senderName: String,
    val content: String,
    val isMe: Boolean,
    val type: String = "TEXT", // "TEXT", "SYSTEM", "DATA_CARD"
    val countryId: String? = null // Dùng cho DATA_CARD
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    roomName: String,
    isDarkMode: Boolean,
    onBackClick: () -> Unit,
    onNavigateToMap: (String) -> Unit // Hàm Callback để Deep-link bay ra bản đồ
) {
    val coroutineScope = rememberCoroutineScope()

    // Màu động Sáng/Tối
    val bgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val topBarColor = if (isDarkMode) Color(0xFF161622) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val borderColor = if (isDarkMode) Color(0xFF2A2A35) else Color(0xFFE5E7EB)

    // Dữ liệu giả lập (Mock Data)
    var messages by remember(roomId) {
        mutableStateOf(
            when (roomId) {
                "room_1" -> listOf(
                    MessageModel("1", "Hệ thống", "Chào mừng đến với Thảo luận Vĩ mô Châu Á!", false, "SYSTEM"),
                    MessageModel("2", "Elon Musk", "Dòng vốn FDI vào khu vực Đông Nam Á đang tăng trưởng rất tốt quý này.", false, "TEXT"),
                    MessageModel("3", "Bạn", "Đúng vậy, đặc biệt là Việt Nam.", true, "TEXT"),
                    MessageModel("4", "Bình", "Đây là số liệu tóm tắt trên bản đồ nè, anh em bấm thẻ xem thử:", false, "TEXT"),
                    MessageModel("5", "Bình", "Việt Nam - FDI: 36.6 Tỷ USD", false, "DATA_CARD", "VN")
                )
                "room_2" -> listOf(
                    MessageModel("1", "Hệ thống", "Chào mừng đến với Nhóm Project 5!", false, "SYSTEM"),
                    MessageModel("2", "Mark Zuckerberg", "Mọi người ơi, nhớ check file Excel số liệu thống kê nhé.", false, "TEXT"),
                    MessageModel("3", "Bình", "Ok Mark Zuckerberg, tí tớ làm nốt API auth rồi kéo về ráp giao diện.", false, "TEXT"),
                    MessageModel("4", "Mark Zuckerberg", "Nhớ nộp báo cáo tiến độ đúng hạn nha anh em!", false, "TEXT")
                )
                "room_3" -> listOf(
                    MessageModel("1", "Hệ thống", "Kênh thông báo biến động Lạm phát tự động.", false, "SYSTEM"),
                    MessageModel("2", "Bot", "⚠️ Cảnh báo: Lạm phát Mỹ tháng này đạt 3.4%, vượt dự báo của FED.", false, "TEXT"),
                    MessageModel("3", "Bạn", "/gdp US", true, "TEXT"),
                    MessageModel("4", "Hệ thống", "🤖 Bot: GDP của Hoa Kỳ hiện tại là 27,360 Tỷ USD.", false, "SYSTEM")
                )
                else -> listOf(
                    MessageModel("1", "Hệ thống", "Phòng chat mới đã được khởi tạo thành công!", false, "SYSTEM")
                )
            }
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarColor)
                .border(1.dp, borderColor)
                .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF00B8D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Public, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(roomName, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Đang trực tuyến", color = Color(0xFF00E676), fontSize = 11.sp)
                }
            }
            // Nút tìm kiếm & thêm người (Tính năng 1)
            IconButton(onClick = { /* Thêm người */ }) {
                Icon(Icons.Rounded.GroupAdd, contentDescription = "Thêm người", tint = Color(0xFF00B8D4))
            }
        }

        // --- KHU VỰC HIỂN THỊ TIN NHẮN ---
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(messages) { msg ->
                when (msg.type) {
                    "SYSTEM" -> SystemMessage(msg.content, isDarkMode)
                    "DATA_CARD" -> DataCardMessage(msg, isDarkMode) { countryId ->
                        // Khi click vào thẻ, gọi hàm điều hướng bay sang Map
                        onNavigateToMap(countryId)
                    }
                    else -> ChatBubble(msg, isDarkMode) // "TEXT"
                }
            }

            // Hiệu ứng AI đang gõ chữ
            if (isAiTyping) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Rounded.SmartToy, null, tint = Color(0xFF00B8D4), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trợ lý AI đang phân tích...", color = Color.Gray, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }

        // --- KHU VỰC NHẬP TIN NHẮN ---
        val inputBg = if (isDarkMode) Color(0xFF1E1E28) else Color.White
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarColor)
                .border(1.dp, borderColor)
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Gõ tin nhắn, /lệnh hoặc @AI...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = Color(0xFF00B8D4),
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val newMsg = MessageModel(System.currentTimeMillis().toString(), "Bạn", inputText, true)
                        messages = messages + newMsg
                        val tempInput = inputText
                        inputText = ""

                        // --- MÔ PHỎNG LOGIC BACKEND SẼ LÀM ---
                        coroutineScope.launch {
                            // 1. Nếu dùng lệnh /gdp
                            if (tempInput.startsWith("/gdp")) {
                                delay(500)
                                messages = messages + MessageModel("sys_1", "Hệ thống", "🤖 Bot: GDP của Việt Nam hiện tại là 430 Tỷ USD.", false, "SYSTEM")
                            }
                            // 2. Nếu tag @AI
                            else if (tempInput.contains("@AI")) {
                                isAiTyping = true
                                delay(2000) // Giả lập chờ AI gọi API mất 2 giây
                                isAiTyping = false
                                messages = messages + MessageModel("ai_1", "Trợ lý AI", "Theo dữ liệu tôi phân tích được, dòng tiền đang có xu hướng dịch chuyển mạnh về châu Á do lãi suất FED.", false, "TEXT")
                            }
                        }
                    }
                },
                containerColor = Color(0xFF00B8D4),
                modifier = Modifier.size(48.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Gửi", tint = Color.White)
            }
        }
    }
}

// ==========================================
// CÁC COMPONENTS (BONG BÓNG CHAT CHIA THEO LOẠI)
// ==========================================

@Composable
fun SystemMessage(text: String, isDarkMode: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val bg = if (isDarkMode) Color(0xFF333344) else Color(0xFFE5E7EB)
        Text(
            text = text,
            color = if (isDarkMode) Color.LightGray else Color.DarkGray,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.background(bg, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ChatBubble(msg: MessageModel, isDarkMode: Boolean) {
    val bubbleColor = if (msg.isMe) Color(0xFF00B8D4) else if (isDarkMode) Color(0xFF2C2C3A) else Color.White
    val textColor = if (msg.isMe) Color.White else if (isDarkMode) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start
    ) {
        if (!msg.isMe) {
            Text(msg.senderName, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        }
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (msg.isMe) 16.dp else 0.dp,
                bottomEnd = if (msg.isMe) 0.dp else 16.dp
            ),
            shadowElevation = if (isDarkMode) 0.dp else 2.dp
        ) {
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

        // Giao diện Thẻ Dữ Liệu
        Column(
            modifier = Modifier
                .width(260.dp)
                .background(bg, RoundedCornerShape(16.dp))
                .border(1.dp, border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { msg.countryId?.let { onClick(it) } } // Bấm vào để bay ra Map
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Public, null, tint = border)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DỮ LIỆU ĐƯỢC CHIA SẺ", color = border, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg.content, color = textCol, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(12.dp))
            // Nút ảo giả lập Deep-link
            Row(
                modifier = Modifier.fillMaxWidth().background(border.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AdsClick, null, tint = border, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chạm để xem trên Bản đồ", color = border, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}