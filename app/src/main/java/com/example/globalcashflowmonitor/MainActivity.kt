package com.example.globalcashflowmonitor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.globalcashflowmonitor.ui.components.BottomNavBar
import com.example.globalcashflowmonitor.ui.theme.GlobalCashFlowMonitorTheme
import com.example.globalcashflowmonitor.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // 1. KHAI BÁO CÁC BIẾN TRẠNG THÁI Ở ĐÂY (TRONG SCOPE CỦA BỘ NHỚ)
            val context = this
            val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

            var isDarkMode by remember { mutableStateOf(sharedPref.getBoolean("DARK_MODE", true)) }
            var isLoggedIn by remember { mutableStateOf(sharedPref.getBoolean("IS_LOGGED_IN", false)) }
            var currentScreen by remember { mutableStateOf(0) }
            var targetCountryIdToZoom by remember { mutableStateOf<String?>(null) }

            // Các biến phục vụ Auth
            var showAuthScreen by remember { mutableStateOf(false) }
            var authScreenType by remember { mutableStateOf("LOGIN") }

            GlobalCashFlowMonitorTheme(darkTheme = isDarkMode) {
                Scaffold(
                    bottomBar = {
                        BottomNavBar(
                            selectedTab = currentScreen,
                            onTabSelected = { currentScreen = it }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                        // ĐIỀU PHỐI MÀN HÌNH CHÍNH
                        when (currentScreen) {
                            0 -> MapScreen(
                                isLoggedIn = isLoggedIn,
                                isDarkMode = isDarkMode,
                                targetCountryId = targetCountryIdToZoom,
                                onZoomCompleted = { targetCountryIdToZoom = null },
                                onNavigateToLogin = { authScreenType = "LOGIN"; showAuthScreen = true }
                            )
                            1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Màn hình Thống kê", color = if(isDarkMode) Color.White else Color.Black)
                            }
                            2 -> AiScreen()
                            3 -> SettingsScreen(
                                onLogout = { isLoggedIn = false },
                                isDarkMode = isDarkMode,
                                onThemeChange = { newTheme: Boolean ->
                                    isDarkMode = newTheme
                                    sharedPref.edit().putBoolean("DARK_MODE", newTheme).apply()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}