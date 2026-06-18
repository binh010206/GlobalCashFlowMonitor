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
import com.example.globalcashflowmonitor.network.CountryTimeline
import kotlinx.coroutines.delay

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
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

    // Đọc trạng thái
    var isDarkMode by remember { mutableStateOf(sharedPref.getBoolean("DARK_MODE", true)) }
    var isLoggedIn by remember { mutableStateOf(sharedPref.getBoolean("IS_LOGGED_IN", false)) }
    var authScreenType by remember { mutableStateOf("LOGIN") } // Chuyển đổi giữa LOGIN, REGISTER, FORGOT

    // 0=Map, 1=AI, 2=Stats, 3=Chat, 4=Settings/Auth
    var currentScreen by remember { mutableStateOf(0) }
    var targetCountryIdToZoom by remember { mutableStateOf<String?>(null) }

    // ====== QUẢN LÝ ĐIỀU HƯỚNG PHÒNG CHAT ======
    var selectedChatRoomId by remember { mutableStateOf<String?>(null) }
    var selectedChatRoomName by remember { mutableStateOf("") }

    // ====== LẤY DỮ LIỆU ĐỂ TRUYỀN VÀO THỐNG KÊ ======
    var globalCountriesList by remember { mutableStateOf<List<CountryTimeline>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = com.example.globalcashflowmonitor.network.RetrofitClient.instance.getAllTimelines()
                if (response.success && response.data.isNotEmpty()) {
                    globalCountriesList = response.data
                }
            } catch (e: Exception) { }
            delay(15000)
        }
    }

    // Bảo vệ chuyển Tab
    fun navigateToTab(tabIndex: Int) {
        if (tabIndex != 3) {
            selectedChatRoomId = null
        }
        currentScreen = tabIndex
    }

    GlobalCashFlowMonitorTheme(darkTheme = isDarkMode) {
        val rootBgColor = if (isDarkMode) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)

        Box(modifier = Modifier.fillMaxSize().background(rootBgColor)) {

            Scaffold(
                modifier = Modifier.zIndex(1f),
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                    when (currentScreen) {
                        0 -> MapScreen(
                            isLoggedIn = isLoggedIn,
                            isDarkMode = isDarkMode,
                            targetCountryId = targetCountryIdToZoom,
                            onZoomCompleted = { targetCountryIdToZoom = null },
                            onNavigateToLogin = { authScreenType = "LOGIN"; navigateToTab(4) }
                        )

                        1 -> AiScreen(isLoggedIn = isLoggedIn)

                        2 -> StatsScreen(countries = globalCountriesList)

                        3 -> {
                            if (selectedChatRoomId == null) {
                                ChatListScreen(
                                    isDarkMode = isDarkMode,
                                    onNavigateToRoom = { roomId, roomName ->
                                        selectedChatRoomId = roomId
                                        selectedChatRoomName = roomName
                                    }
                                )
                            } else {
                                ChatScreen(
                                    roomId = selectedChatRoomId!!,
                                    roomName = selectedChatRoomName,
                                    isDarkMode = isDarkMode,
                                    onBackClick = { selectedChatRoomId = null },
                                    onNavigateToMap = { countryId ->
                                        targetCountryIdToZoom = countryId
                                        currentScreen = 0
                                    }
                                )
                            }
                        }

                        // TRỌN BỘ MÀN HÌNH ĐĂNG NHẬP VÀ CÀI ĐẶT
                        4 -> {
                            if (isLoggedIn) {
                                SettingsScreen(
                                    onLogout = {
                                        sharedPref.edit().putBoolean("IS_LOGGED_IN", false).apply()
                                        isLoggedIn = false
                                    },
                                    onNavigateToForgot = {
                                        // Bấm Quên Mật Khẩu ở Settings -> Đăng xuất và đá qua màn Khôi phục
                                        sharedPref.edit().putBoolean("IS_LOGGED_IN", false).apply()
                                        isLoggedIn = false
                                        authScreenType = "FORGOT"
                                    }
                                )
                            } else {
                                when (authScreenType) {
                                    "LOGIN" -> LoginScreen(
                                        onLoginSuccess = {
                                            isLoggedIn = true
                                            currentScreen = 0 // TỰ VĂNG RA BẢN ĐỒ KHI ĐĂNG NHẬP
                                        },
                                        onNavigateToRegister = { authScreenType = "REGISTER" },
                                        onNavigateToForgot = { authScreenType = "FORGOT" }
                                    )
                                    "REGISTER" -> RegisterScreen(
                                        onRegisterSuccess = { authScreenType = "LOGIN" },
                                        onNavigateToLogin = { authScreenType = "LOGIN" }
                                    )
                                    "FORGOT" -> ForgotPasswordScreen(
                                        onNavigateBack = { authScreenType = "LOGIN" }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ======= THANH ĐIỀU HƯỚNG DƯỚI CÙNG =======
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
                    shape = RoundedCornerShape(35.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(1f)) {
                            AnimatedNavItem(Icons.Rounded.Public, "Bản đồ", currentScreen == 0) { navigateToTab(0) }
                            AnimatedNavItem(Icons.Rounded.SmartToy, "Trợ lý AI", currentScreen == 1) { navigateToTab(1) }
                        }
                        Spacer(modifier = Modifier.width(80.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(1f)) {
                            AnimatedNavItem(Icons.Rounded.Forum, "Cộng đồng", currentScreen == 3) { navigateToTab(3) }
                            AnimatedNavItem(Icons.Rounded.Settings, "Cài đặt", currentScreen == 4) { navigateToTab(4) }
                        }
                    }
                }

                // NÚT FAB CENTER
                Column(
                    modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingActionButton(
                        onClick = { navigateToTab(2) },
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(12.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .scale(animateFloatAsState(if (currentScreen == 2) 1.1f else 1.0f).value)
                    ) {
                        Icon(Icons.Rounded.Analytics, "Thống kê", modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Thống kê",
                        color = if (currentScreen == 2) Color(0xFFFFC107) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (currentScreen == 2) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ==================================================
// COMPONENT: Nút bấm Animation dưới BottomBar
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

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
        Text(title, color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}