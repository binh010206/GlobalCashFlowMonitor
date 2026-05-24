package com.example.globalcashflowmonitor

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
import com.example.globalcashflowmonitor.ui.screens.MapScreen
import com.example.globalcashflowmonitor.ui.screens.SettingsScreen
import com.example.globalcashflowmonitor.ui.screens.AiScreen
import com.example.globalcashflowmonitor.ui.screens.StatsScreen





class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlobalCashFlowMonitorTheme(darkTheme = true) {

                // Trạng thái lưu xem đang ở màn hình nào (0 = Map, 1 = Stats, 2 = AI)
                var currentScreen by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        BottomNavBar(selectedTab = currentScreen, onTabSelected = { currentScreen = it })
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                        // ĐIỀU PHỐI MÀN HÌNH CHÍNH
                        when (currentScreen) {

                            0 -> MapScreen()
                            // Số 1: Hiện màn hình Thống kê
                            1 -> StatsScreen()

                            // Số 2: Hiện màn hình AI
                            2 -> AiScreen()
                            3 -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}