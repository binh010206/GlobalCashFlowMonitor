package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    isDarkMode: Boolean,             // Nhận trạng thái Sáng/Tối từ MainActivity
    onThemeChange: (Boolean) -> Unit // Gửi tín hiệu lật công tắc ra ngoài MainActivity
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

    val userName = sharedPref.getString("USER_NAME", "Khách") ?: "Khách"
    val userEmail = sharedPref.getString("USER_EMAIL", "Chưa đăng nhập") ?: "Chưa đăng nhập"
    var isColorBlindMode by remember { mutableStateOf(sharedPref.getBoolean("COLOR_BLIND", false)) }

    // BỘ MÀU ĐỘNG: Nếu Dark Mode thì dùng màu tối, ngược lại dùng màu sáng
    val bgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val cardBgColor = if (isDarkMode) Color(0xFF161622) else Color.White
    val cardBorderColor = if (isDarkMode) Color(0xFF2A2A35) else Color(0xFFE5E7EB)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val subTextColor = if (isDarkMode) Color.Gray else Color.DarkGray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor) // Dùng màu động
            .padding(24.dp)
            .padding(top = 40.dp)
    ) {
        Text("Cài đặt hệ thống", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // --- THẺ THÔNG TIN NGƯỜI DÙNG ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgColor, RoundedCornerShape(16.dp))
                .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00B8D4)),
                contentAlignment = Alignment.Center
            ) {
                Text(userName.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(userEmail, color = subTextColor, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("GIAO DIỆN & HIỂN THỊ", color = Color(0xFF00B8D4), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        SettingSwitchRow(
            icon = Icons.Rounded.DarkMode,
            title = "Chế độ nền tối (Dark Mode)",
            checked = isDarkMode,
            onCheckedChange = { onThemeChange(it) }, // Bắn sự kiện ra ngoài
            textColor = textColor
        )

        SettingSwitchRow(
            icon = Icons.Rounded.Visibility,
            title = "Chế độ cho người mù màu",
            checked = isColorBlindMode,
            onCheckedChange = {
                isColorBlindMode = it
                sharedPref.edit().putBoolean("COLOR_BLIND", it).apply()
            },
            textColor = textColor
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                sharedPref.edit().putBoolean("IS_LOGGED_IN", false).apply()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744))
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color(0xFFFF1744))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng xuất tài khoản", color = Color(0xFFFF1744), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color // Nhận màu chữ động
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = textColor, fontSize = 16.sp) // Áp dụng màu chữ động
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00B8D4),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}