package com.example.globalcashflowmonitor.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(containerColor = Color(0xFF121212)) {
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.Public, "Trang chủ") },
            label = { Text("Bản đồ") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.BarChart, "Thống kê") },
            label = { Text("Thống kê") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.AutoAwesome, "AI") },
            label = { Text("AI Assist") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
        )
        // NÚT CÀI ĐẶT ĐÂY RỒI NÈ!!!
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.Settings, "Cài đặt") },
            label = { Text("Cài đặt") },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
        )
    }
}