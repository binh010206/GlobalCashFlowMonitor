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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MOCK DATA CLASSES ---
data class ChatRoom(val id: String, val name: String, val lastMessage: String, val time: String, val unread: Int = 0)
data class UserItem(val id: String, val name: String, val role: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    isDarkMode: Boolean,
    onNavigateToRoom: (String, String) -> Unit // Truyền ID và Tên phòng sang ChatScreen
) {

    val bgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF161622) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val borderColor = if (isDarkMode) Color(0xFF2A2A35) else Color(0xFFE5E7EB)

    // --- STATE ---
    // Giả lập danh sách phòng
    var chatRooms by remember {
        mutableStateOf(
            listOf(
                ChatRoom("room_1", "Thảo luận Vĩ mô Châu Á", "Bạn: Dòng vốn FDI đang tăng...", "10:42", 2),
                ChatRoom("room_2", "Nhóm Project 5", "Bình: Nhớ nộp báo cáo sớm nha!", "Hôm qua", 0),
                ChatRoom("room_3", "Cảnh báo Lạm phát", "Hệ thống: Lạm phát Mỹ vượt ngưỡng...", "T2", 5)
            )
        )
    }

    var showCreateGroupSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cộng Đồng & Thảo Luận", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.Search, contentDescription = "Tìm kiếm", tint = textColor)
            }
            HorizontalDivider(color = borderColor)

            // --- DANH SÁCH NHÓM ---
            if (chatRooms.isEmpty()) {
                // UI KHI CHƯA CÓ NHÓM
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có cuộc trò chuyện nào", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bấm vào nút dấu + để tìm bạn bè và tạo nhóm mới nhé.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                // UI KHI ĐÃ CÓ NHÓM
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatRooms) { room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToRoom(room.id, room.name) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Nhóm
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF00B8D4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(room.name.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(room.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(room.lastMessage, color = if (room.unread > 0) textColor else Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            // Thời gian & Badge chưa đọc
                            Column(horizontalAlignment = Alignment.End) {
                                Text(room.time, color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (room.unread > 0) {
                                    Box(
                                        modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFFF1744)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(room.unread.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = borderColor, modifier = Modifier.padding(start = 82.dp)) // Line mờ mờ dưới mỗi item
                    }
                }
            }
        }

        // --- NÚT FLOATING TẠO NHÓM MỚI ---
        FloatingActionButton(
            onClick = { showCreateGroupSheet = true },
            containerColor = Color(0xFF00E676),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp) // Né cái Bottom Bar hệ thống
        ) {
            Icon(Icons.Rounded.AddComment, contentDescription = "Tạo nhóm", tint = Color.Black)
        }

        // --- BOTTOM SHEET: TẠO NHÓM ---
        if (showCreateGroupSheet) {
            CreateGroupBottomSheet(
                isDarkMode = isDarkMode,
                onDismiss = { showCreateGroupSheet = false },
                onCreate = { groupName, selectedUsers ->
                    // Logic ảo khi bấm nút Tạo
                    val newRoom = ChatRoom("room_${System.currentTimeMillis()}", groupName.ifBlank { "Nhóm mới" }, "Bạn vừa tạo nhóm", "Vừa xong", 0)
                    chatRooms = listOf(newRoom) + chatRooms
                    showCreateGroupSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Set<String>) -> Unit
) {
    val sheetBg = if (isDarkMode) Color(0xFF161622) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val inputBg = if (isDarkMode) Color(0xFF1E1E28) else Color(0xFFF3F4F6)

    // Dữ liệu giả lập bạn bè
    val mockUsers = listOf(
        UserItem("u1", "Trần Quang Bình", "Developer"),
        UserItem("u2", "Phan Thanh Khang", "Data Analyst"),
        UserItem("u3", "Hà Điển Trung Hiếu", "Designer"),
        UserItem("u4", "Lê Nhuận Phát", "Backend"),
        UserItem("u5", "Gemini AI", "Bot")
    )

    var searchQuery by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Tạo nhóm thảo luận", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Ô đặt tên nhóm
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Tên nhóm (Không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg, unfocusedContainerColor = inputBg,
                    focusedTextColor = textColor, unfocusedTextColor = textColor
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ô tìm kiếm thành viên
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm tên thành viên...") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg, unfocusedContainerColor = inputBg,
                    focusedTextColor = textColor, unfocusedTextColor = textColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Danh sách thành viên
            Text("Gợi ý (${selectedUsers.size} đã chọn)", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                items(mockUsers.filter { it.name.contains(searchQuery, ignoreCase = true) }) { user ->
                    val isSelected = selectedUsers.contains(user.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUsers = if (isSelected) selectedUsers - user.id else selectedUsers + user.id
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) Color(0xFF00B8D4) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(user.role, color = Color.Gray, fontSize = 12.sp)
                        }

                        // Checkbox custom
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF00B8D4) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút tạo
            Button(
                onClick = { onCreate(groupName, selectedUsers) },
                enabled = selectedUsers.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4), disabledContainerColor = Color.DarkGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("TẠO PHÒNG CHAT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}