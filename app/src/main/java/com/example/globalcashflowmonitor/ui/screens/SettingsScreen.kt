package com.example.globalcashflowmonitor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onLogout: () -> Unit
) {
    // Các biến trạng thái (State) để lưu lựa chọn của người dùng
    var isDarkMode by remember { mutableStateOf(true) }
    var isColorBlindMode by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("USD ($)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 40.dp) // Đẩy xuống một chút để né thanh trạng thái của điện thoại
    ) {
        Text("Cài đặt hệ thống", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        // --- PHẦN 1: GIAO DIỆN & HIỂN THỊ ---
        Text("GIAO DIỆN & HIỂN THỊ", color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        SettingSwitchRow(
            icon = Icons.Rounded.DarkMode,
            title = "Chế độ nền tối (Dark Mode)",
            checked = isDarkMode,
            onCheckedChange = { isDarkMode = it }
        )
        SettingSwitchRow(
            icon = Icons.Rounded.Visibility,
            title = "Chế độ mù màu (Tương phản cao)",
            checked = isColorBlindMode,
            onCheckedChange = { isColorBlindMode = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- PHẦN 2: DỮ LIỆU & TIỀN TỆ ---
        Text("DỮ LIỆU KINH TẾ", color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Dòng hiển thị Đơn vị tiền tệ (Mốt mình làm cái Popup bấm vào để chọn VND/EUR sau)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AttachMoney, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Đơn vị tiền tệ chính", color = Color.White, fontSize = 16.sp)
            }
            Text(selectedCurrency, color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- PHẦN 3: TÀI KHOẢN
        Text("TÀI KHOẢN", color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onLogout() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744).copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = Color(0xFFFF1744))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Đăng xuất tài khoản", color = Color(0xFFFF1744), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Thông báo số lượt AI dành cho Guest
        Text(
            text = "Phiên đăng nhập đang hoạt động ổn định.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

// Hàm dùng chung để vẽ mấy cái công tắc (Switch) cho lẹ, đỡ lặp code
@Composable
fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Cyan,
                checkedTrackColor = Color(0xFF005555), // Màu xanh ngọc đậm lúc bật
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2C2C2C)
            )
        )
    }
}