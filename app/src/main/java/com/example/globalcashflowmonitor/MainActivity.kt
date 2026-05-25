package com.example.globalcashflowmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.globalcashflowmonitor.ui.components.BottomNavBar
import com.example.globalcashflowmonitor.ui.theme.GlobalCashFlowMonitorTheme
import com.example.globalcashflowmonitor.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlobalCashFlowMonitorTheme(darkTheme = true) {

                // 1. Quản lý trạng thái
                var isLoggedIn by remember { mutableStateOf(false) }
                var showAuthScreen by remember { mutableStateOf(false) } // Bật/tắt tường Đăng nhập
                var authScreen by remember { mutableStateOf("LOGIN") } // Chuyển đổi giữa Login/Register/Forgot
                var currentScreen by remember { mutableStateOf(0) } // Tabs: 0=Map, 1=Stats, 2=AI, 3=Settings

                // 2. Cấu trúc Xếp chồng (Box)
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {

                    // ==========================================
                    // LỚP DƯỚI CÙNG: APP CHÍNH (LUÔN LUÔN HIỆN)
                    // ==========================================
                    Scaffold(
                        bottomBar = {
                            BottomNavBar(
                                selectedTab = currentScreen,
                                onTabSelected = { currentScreen = it }
                            )
                        },
                        modifier = Modifier.zIndex(1f) // Giữ ở lớp dưới
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            when (currentScreen) {
                                0 -> MapScreen(
                                    isLoggedIn = isLoggedIn,
                                    onNavigateToLogin = {
                                        authScreen = "LOGIN"
                                        showAuthScreen = true // Bấm vào Avatar -> Kéo tường Đăng nhập lên
                                    }
                                )
                                1 -> StatsScreen()
                                2 -> AiScreen()
                                3 -> SettingsScreen(
                                    onLogout = {
                                        isLoggedIn = false
                                        currentScreen = 0
                                    }
                                )
                            }
                        }
                    }

                    // ==========================================
                    // LỚP TRÊN CÙNG: OVERLAY ĐĂNG NHẬP (TRƯỢT LÊN)
                    // ==========================================
                    AnimatedVisibility(
                        visible = showAuthScreen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), // Trượt từ dưới đáy lên
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), // Trượt xuống lại
                        modifier = Modifier.zIndex(2f) // Nổi lên trên cùng, che lấp toàn bộ App
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0A0A14)) // Màu nền che đi app ở dưới
                        ) {
                            when (authScreen) {
                                "LOGIN" -> {
                                    LoginScreen(
                                        onLoginSuccess = {
                                            isLoggedIn = true
                                            showAuthScreen = false // Thành công -> Vuốt màn hình này xuống
                                        },
                                        onNavigateToRegister = { authScreen = "REGISTER" },
                                        onNavigateToForgot = { authScreen = "FORGOT" }
                                    )
                                }
                                "REGISTER" -> {
                                    RegisterScreen(
                                        onRegisterSuccess = { authScreen = "LOGIN" },
                                        onNavigateToLogin = { authScreen = "LOGIN" }
                                    )
                                }
                                "FORGOT" -> {
                                    ForgotPasswordScreen(
                                        onNavigateBack = { authScreen = "LOGIN" }
                                    )
                                }
                            }

                            // Nút "X" góc trên bên phải để TẮT màn hình Đăng nhập nếu họ đổi ý
                            IconButton(
                                onClick = { showAuthScreen = false },
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                            ) {
                                Text("✕", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}