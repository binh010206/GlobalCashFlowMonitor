package com.example.globalcashflowmonitor.ui.screens

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
import kotlinx.coroutines.delay

// Lịch sử chat giả lập
data class ChatSession(val id: String, val title: String, val date: String)
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen() {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf(
        ChatMessage("Xin chào! Tôi là Trợ lý AI Tài chính Vĩ mô. Bạn muốn phân tích dòng tiền của quốc gia nào hôm nay?", isUser = false)
    )}
    var inputText by remember { mutableStateOf("") }
    var aiUsageLeft by remember { mutableStateOf(5) }

    // STATE: Lưu thời gian bắt đầu khóa và Text hiển thị đồng hồ đếm ngược
    var lockoutStartTime by remember { mutableStateOf(0L) }
    var timeRemainingText by remember { mutableStateOf("") }

    // STATE CHO LỊCH SỬ CHAT
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    // Dữ liệu lịch sử giả lập (Mock Data)
    val mockHistory = listOf(
        ChatSession("1", "Phân tích FDI khu vực Đông Nam Á", "Hôm nay"),
        ChatSession("2", "Dự báo lạm phát Mỹ năm 2026", "Hôm qua"),
        ChatSession("3", "So sánh cán cân thương mại VN-TQ", "2 ngày trước"),
        ChatSession("4", "Tác động của dự trữ ngoại hối Nhật Bản", "Tuần trước")
    )


    // THUẬT TOÁN ĐẾM NGƯỢC
    LaunchedEffect(aiUsageLeft, lockoutStartTime) {
        if (aiUsageLeft == 0 && lockoutStartTime > 0) {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val timePassed = currentTime - lockoutStartTime

                // ---------------------------------------------------------
                // CHỖ NÀY ĐỂ CHỈNH THỜI GIAN KHÓA:
                // Hiện tại đang set 15 giây để mày test cho lẹ (15L * 1000)
                // LÀM THẬT 5 TIẾNG THÌ SỬA THÀNH: 5L * 60 * 60 * 1000
                // ---------------------------------------------------------
                val cooldownTime = 15L * 1000

                if (timePassed >= cooldownTime) {
                    aiUsageLeft = 5
                    lockoutStartTime = 0L
                    timeRemainingText = ""
                    break
                } else {
                    // Tính toán quy đổi ra Giờ:Phút:Giây
                    val timeLeft = cooldownTime - timePassed
                    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(timeLeft)
                    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
                    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
                    timeRemainingText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
                delay(1000) // Đợi 1 giây rồi update lại vòng lặp
            }
        }
    }

    // Kho câu trả lời ảo (Mock Data)
    val fakeReplies = listOf(
        "Theo dữ liệu của Ngân hàng Thế giới, dòng tiền này đang chịu ảnh hưởng từ lạm phát.",
        "Tín hiệu khả quan! FDI vào khu vực này đang tăng trưởng ổn định trong quý vừa qua.",
        "Tôi đang phân tích... Biểu đồ cho thấy một sự sụt giảm nhẹ về cán cân thương mại.",
        "Câu hỏi rất hay. Điều này phụ thuộc nhiều vào chính sách lãi suất của FED sắp tới.",
        "Hệ thống ghi nhận dòng vốn có sự dịch chuyển sang các nước đang phát triển tại Đông Nam Á."
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 30.dp)) {
        // HEADER BẢNG CHAT
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "AI Advisor",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // HIỆN ĐỒNG HỒ ĐẾM NGƯỢC
                    text = if (aiUsageLeft > 0) "Chế độ: Khách (Guest)" else "MỞ KHÓA SAU: $timeRemainingText",
                    color = if (aiUsageLeft > 0) Color.LightGray else Color.Red,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = "Lịch sử", tint = Color.Cyan)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (aiUsageLeft > 0) Color(0xFF005555) else Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (aiUsageLeft > 0) "Còn lại: $aiUsageLeft/5" else "Đang khóa",
                        color = if (aiUsageLeft > 0) Color.Cyan else Color.Red,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }
        }
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 16.dp))

        // VÙNG HIỂN THỊ TIN NHẮN
        LazyColumn(modifier = Modifier.weight(1.0f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { msg ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        color = if (msg.isUser) Color.Cyan else Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 0.dp,
                            bottomEnd = if (msg.isUser) 0.dp else 16.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(text = msg.text, color = if (msg.isUser) Color.Black else Color.White, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ô GÕ CHAT
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                enabled = aiUsageLeft > 0,
                placeholder = {
                    Text(if (aiUsageLeft > 0) "Hỏi về dòng tiền..." else "Chờ $timeRemainingText để hỏi tiếp", color = Color.Gray)
                },
                modifier = Modifier.weight(1.0f), shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E), disabledContainerColor = Color(0xFF1A1A1A),
                    focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.trim().isNotEmpty() && aiUsageLeft > 0) {
                        messages.add(ChatMessage(inputText, isUser = true))
                        inputText = ""
                        aiUsageLeft -= 1

                        // KÍCH HOẠT ĐẾM NGƯỢC KHI VỀ SỐ 0
                        if (aiUsageLeft == 0) {
                            lockoutStartTime = System.currentTimeMillis()
                        }

                        // Lấy ngẫu nhiên 1 câu trả lời cho đỡ chán
                        messages.add(ChatMessage(fakeReplies.random(), isUser = false))
                    }
                },
                enabled = aiUsageLeft > 0 && inputText.trim().isNotEmpty(),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Cyan, disabledContainerColor = Color.DarkGray),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Gửi", tint = Color.Black)
            }
        }
    }

    // BOTTOM SHEET HIỂN THỊ LỊCH SỬ CHAT
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
                Text("Lịch sử tư vấn AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(mockHistory) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                                .clickable {
                                    // TODO: Load lại tin nhắn của session này khi click
                                    showHistorySheet = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(text = session.date, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}