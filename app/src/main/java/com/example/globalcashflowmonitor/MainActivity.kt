package com.example.globalcashflowmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.globalcashflowmonitor.ui.theme.GlobalCashFlowMonitorTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GlobalCashFlowMonitorTheme(darkTheme = true) {

                // Danh sách các mục dòng tiền
                val dataTabs = listOf("Top 50", "FDI Inflow", "GDP Growth", "GDA", "O&M")

                // SỬ DỤNG mutableStateListOf ĐỂ CHỌN NHIỀU MỤC CÙNG LÚC
                val selectedTabs = remember { mutableStateListOf<String>("Top 50") }

                var selectedBottomTab by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF121212)) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Rounded.Public, "Trang chủ") },
                                label = { Text("Trang chủ") },
                                selected = selectedBottomTab == 0,
                                onClick = { selectedBottomTab = 0 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Rounded.BarChart, "Thống kê") },
                                label = { Text("Thống kê") },
                                selected = selectedBottomTab == 1,
                                onClick = { selectedBottomTab = 1 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Rounded.AutoAwesome, "AI") },
                                label = { Text("AI Assist") },
                                selected = selectedBottomTab == 2,
                                onClick = { selectedBottomTab = 2 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Rounded.Settings, "Cài đặt") },
                                label = { Text("Cài đặt") },
                                selected = selectedBottomTab == 3,
                                onClick = { selectedBottomTab = 3 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Cyan, unselectedIconColor = Color.Gray)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                        if (selectedBottomTab == 0) {
                            MapboxMap(
                                modifier = Modifier.fillMaxSize(),
                                mapInitOptionsFactory = { context ->
                                    MapInitOptions(
                                        context = context,
                                        styleUri = Style.DARK,
                                        // HIỆN THẲNG VỀ ĐÀ NẴNG
                                        cameraOptions = CameraOptions.Builder()
                                            .center(Point.fromLngLat(108.2022, 16.0544))
                                            .zoom(10.0) // Độ cao vừa đủ nhìn cả thành phố và vùng lân cận
                                            .pitch(45.0) // Nghiêng góc 45 độ để nhìn rõ cột 3D sau này
                                            .build()
                                    )
                                }
                            )
                        } else {
                            CenterTextPlaceholder("Màn hình đang phát triển: $selectedBottomTab")
                        }

                        // HEADER VỚI THANH TÌM KIẾM VÀ CHIPS
                        if (selectedBottomTab == 0) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 65.dp)) { // Hạ xuống 65dp để né la bàn

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(30.dp),
                                    shadowElevation = 10.dp,
                                    color = Color(0xFF2C2C2C)
                                ) {
                                    OutlinedTextField(
                                        value = "",
                                        onValueChange = {},
                                        placeholder = { Text("Tìm kiếm quốc gia, khu vực...", color = Color.Gray) },
                                        leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = Color.Cyan) }, // Icon Cyan đồng bộ
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(30.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Cyan,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color.Cyan
                                        ),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // THANH CHỌN ĐA MỤC DÒNG TIỀN
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(dataTabs) { tabName ->
                                        FilterChip(
                                            selected = selectedTabs.contains(tabName),
                                            onClick = {
                                                // LOGIC CHỌN NHIỀU MỤC
                                                if (selectedTabs.contains(tabName)) {
                                                    if (selectedTabs.size > 1) selectedTabs.remove(tabName)
                                                } else {
                                                    selectedTabs.add(tabName)
                                                }
                                            },
                                            label = { Text(tabName) },
                                            leadingIcon = if (selectedTabs.contains(tabName)) {
                                                { Icon(Icons.Rounded.Check, "Checked", modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color.Cyan,
                                                selectedLabelColor = Color.Black,
                                                selectedLeadingIconColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CenterTextPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(text = text, color = Color.White)
    }
}