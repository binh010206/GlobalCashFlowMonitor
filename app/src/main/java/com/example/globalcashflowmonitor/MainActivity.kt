package com.example.globalcashflowmonitor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.globalcashflowmonitor.ui.theme.GlobalCashFlowMonitorTheme
import com.example.globalcashflowmonitor.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

                MainAppFlow()
        }
    }
}

@Composable
fun MainAppFlow() {

    val context = LocalContext.current
    val sharedPref =
        remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

    // Đọc trạng thái Dark Mode
    var isDarkMode by remember { mutableStateOf(sharedPref.getBoolean("DARK_MODE", true)) }

    // 🌟 TRẠNG THÁI HỆ THỐNG
    var isLoggedIn by remember { mutableStateOf(sharedPref.getBoolean("IS_LOGGED_IN", false)) }

    // Trạng thái màn hình Auth (Đăng nhập/Đăng ký)
    var showAuthScreen by remember { mutableStateOf(false) }
    var authScreenType by remember { mutableStateOf("LOGIN") } // "LOGIN" hoặc "REGISTER"

    // 0=Map, 1=AI, 2=Stats(Center), 3=Chat, 4=Settings
    var currentScreen by remember { mutableStateOf(0) }

    // HÀM BẢO VỆ CHUYỂN TAB (FREE vs PREMIUM)
    fun navigateToTab(tabIndex: Int) {
        val premiumTabs = listOf(1, 3, 4) // AI, Chat, Settings bắt buộc Đăng nhập
        if (tabIndex in premiumTabs && !isLoggedIn) {
            authScreenType = "LOGIN"
            showAuthScreen = true
        } else {
            currentScreen = tabIndex
        }
    }

    GlobalCashFlowMonitorTheme(darkTheme = isDarkMode) {

        // Cập nhật màu nền tổng của Box ngoài cùng để nó thay đổi Sáng/Tối
        val rootBgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)

        Box(modifier = Modifier.fillMaxSize().background(rootBgColor)) {

            // ==================================================
            // 1. LỚP MÀN HÌNH NỘI DUNG (CÁC TAB)
            // ==================================================
            Scaffold(
                modifier = Modifier.zIndex(1f),
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    when (currentScreen) {
                        // TAB FREE: Map và Stats
                        0 -> MapScreen(
                            isLoggedIn = isLoggedIn,
                            onNavigateToLogin = { authScreenType = "LOGIN"; showAuthScreen = true }
                        )

                        2 -> StatsScreen(
                            isLoggedIn = isLoggedIn,
                            onNavigateToLogin = { authScreenType = "LOGIN"; showAuthScreen = true }
                        )

                        // TAB PREMIUM: Đã khóa bằng hàm navigateToTab ở trên
                        1 -> PlaceholderScreen("Màn hình AI Assist (Đang ráp)")
                        3 -> PlaceholderScreen("Phòng Chat Vĩ Mô (Đang ráp)")
                        4 -> SettingsScreen(
                            onLogout = {
                                isLoggedIn = false
                            },
                            isDarkMode = isDarkMode,
                            onThemeChange = { newTheme ->
                                isDarkMode = newTheme
                                sharedPref.edit().putBoolean("DARK_MODE", newTheme).apply()
                            }
                        )
                    }
                }
            }

            // ==================================================
            // 2. BOTTOM MENU "TÍCH CHỈ" (GIỮ NGUYÊN DESIGN CỦA MÀY)
            // ==================================================
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .zIndex(3f)
            ) {
                Surface(
                    modifier = Modifier
                        .width(360.dp)
                        .height(70.dp)
                        .shadow(12.dp, RoundedCornerShape(35.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(35.dp)),
                    color = Color(0xCC101010),
                    shape = RoundedCornerShape(35.dp),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cụm Trái
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedNavItem(
                                Icons.Rounded.Public,
                                "Bản đồ",
                                currentScreen == 0
                            ) { navigateToTab(0) }
                            AnimatedNavItem(
                                Icons.Rounded.SmartToy,
                                "AI Assist",
                                currentScreen == 1
                            ) { navigateToTab(1) }
                        }

                        Spacer(modifier = Modifier.width(80.dp)) // Chỗ trống cho FAB

                        // Cụm Phải
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedNavItem(
                                Icons.Rounded.Forum,
                                "Cộng đồng",
                                currentScreen == 3
                            ) { navigateToTab(3) }
                            AnimatedNavItem(
                                Icons.Rounded.Settings,
                                "Cài đặt",
                                currentScreen == 4
                            ) { navigateToTab(4) }
                        }
                    }
                }

                // NÚT FAB THỐNG KÊ (CENTER)
                FloatingActionButton(
                    onClick = { navigateToTab(2) },
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(12.dp),
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.Center)
                        .offset(y = (-35).dp)
                        .scale(animateFloatAsState(if (currentScreen == 2) 1.1f else 1.0f).value)
                ) {
                    Icon(Icons.Rounded.Analytics, "Thống kê", modifier = Modifier.size(30.dp))
                }
            }

            // ==================================================
            // 3. MÀN HÌNH ĐĂNG NHẬP / ĐĂNG KÝ (NỔI LÊN TRÊN CÙNG)
            // ==================================================
            if (showAuthScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background) // Che kín toàn bộ màn hình dưới
                        .zIndex(10f)
                ) {
                    if (authScreenType == "LOGIN") {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                                showAuthScreen = false // Đóng auth screen sau khi thành công
                            },
                            onNavigateToRegister = { authScreenType = "REGISTER" },
                            onNavigateToForgot = { /* Xử lý quên mật khẩu sau */ }
                        )
                    } else {
                        RegisterScreen(
                            onRegisterSuccess = {
                                isLoggedIn = true
                                showAuthScreen = false
                            },
                            onNavigateToLogin = { authScreenType = "LOGIN" }
                        )
                    }

                    // Nút Đóng (Dấu X) góc trên bên phải để quay lại làm "Khách"
                    IconButton(
                        onClick = { showAuthScreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================================================
// COMPONENT: Nút bấm có Animation dưới BottomBar
// ==================================================
@Composable
fun AnimatedNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(targetValue = if (isSelected) Color(0xFFFFC107) else Color.Gray, animationSpec = tween(300))
    val scale by animateFloatAsState(targetValue = if (interactionSource.collectIsPressedAsState().value) 0.9f else 1.0f, animationSpec = tween(150))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .scale(scale)
            .padding(8.dp)
    ) {
        Icon(icon, label, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = contentColor, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ==================================================
// MÀN HÌNH TẠM THỜI (PLACEHOLDER)
// ==================================================
@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}