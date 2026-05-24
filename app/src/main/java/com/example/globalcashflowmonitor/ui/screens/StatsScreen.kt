package com.example.globalcashflowmonitor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.globalcashflowmonitor.data.CountryData
import com.example.globalcashflowmonitor.data.MockData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    var realTimeData by remember { mutableStateOf(MockData.topCountries) }
    var isConnected by remember { mutableStateOf(false) }

    // BỘ NHỚ LỊCH SỬ ĐỂ VẼ BIỂU ĐỒ ĐƯỜNG (Lưu 15 nhịp đập gần nhất)
    var historyData by remember { mutableStateOf(mapOf<String, List<Double>>()) }

    val tabs = listOf("GDP", "FDI Inflow", "Trade Balance", "Reserves", "External Debt")
    var selectedTab by remember { mutableStateOf("GDP") }

    // Nước đang được chọn để xem biểu đồ chi tiết (Mặc định chọn Mỹ hoặc nước đầu tiên)
    var detailedCountryId by remember { mutableStateOf("US") }

    LaunchedEffect(Unit) {
        try {
            // 🚩 ĐỔI IP Ở ĐÂY
            val socket: Socket = IO.socket("192.168.1.198")

            socket.on("connect") { isConnected = true }
            socket.on("disconnect") { isConnected = false }

            socket.on("cashflow_update") { args: Array<Any> ->
                if (args.isNotEmpty()) {
                    val jsonString = args[0].toString()
                    val listType = object : TypeToken<List<CountryData>>() {}.type
                    val newData: List<CountryData> = Gson().fromJson(jsonString, listType)

                    // Sắp xếp rank
                    realTimeData = newData.sortedByDescending { getDynamicValue(it, selectedTab) }

                    // Cập nhật bộ nhớ lịch sử cho biểu đồ
                    val newHistory = historyData.toMutableMap()
                    newData.forEach { country ->
                        val currentList = newHistory[country.id] ?: emptyList()
                        val value = getDynamicValue(country, selectedTab)
                        newHistory[country.id] = (currentList + value).takeLast(15) // Chỉ giữ 15 nhịp đập gần nhất
                    }
                    historyData = newHistory
                }
            }
            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12)) // Màu nền đen sâu đậm chất phân tích
            .padding(top = 45.dp, start = 16.dp, end = 16.dp)
    ) {
        // --- 1. HEADER (TRẠNG THÁI LIVE) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PHÂN TÍCH VĨ MÔ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isConnected) "DATA SYNCING" else "NO SIGNAL", color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. MENU TÙY CHỌN DÒNG TIỀN ---
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs) { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        realTimeData = realTimeData.sortedByDescending { getDynamicValue(it, selectedTab) }
                    },
                    label = { Text(tab, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2962FF).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF40C4FF),
                        labelColor = Color.Gray,
                        containerColor = Color(0xFF1E1E1E)
                    ),
                    border = null
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. DASHBOARD CHI TIẾT (BIỂU ĐỒ ĐƯỜNG REAL-TIME) ---
        val detailedCountry = realTimeData.find { it.id == detailedCountryId } ?: realTimeData.firstOrNull()
        if (detailedCountry != null) {
            val countryHistory = historyData[detailedCountry.id] ?: listOf(getDynamicValue(detailedCountry, selectedTab))
            DetailedChartPanel(country = detailedCountry, tabName = selectedTab, history = countryHistory)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("BẢNG XẾP HẠNG REAL-TIME", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // --- 4. LEADERBOARD ---
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(realTimeData, key = { it.id }) { country ->
                val history = historyData[country.id] ?: listOf(getDynamicValue(country, selectedTab))
                val currentValue = history.lastOrNull() ?: 0.0
                val previousValue = if (history.size > 1) history[history.size - 2] else currentValue
                val isUp = currentValue >= previousValue
                val diff = Math.abs(currentValue - previousValue)

                CountryStatCard(
                    rank = realTimeData.indexOf(country) + 1,
                    countryName = country.name,
                    value = currentValue,
                    diff = diff,
                    isUp = isUp,
                    isSelected = country.id == detailedCountryId,
                    onClick = { detailedCountryId = country.id }
                )
            }
        }
    }
}

// --- COMPONENT: BIỂU ĐỒ ĐƯỜNG (SPARKLINE CHUYÊN NGHIỆP) ---
@Composable
fun DetailedChartPanel(country: CountryData, tabName: String, history: List<Double>) {
    val currentValue = history.lastOrNull() ?: 0.0
    val firstValue = history.firstOrNull() ?: 0.0
    val isOverallUp = currentValue >= firstValue
    val chartColor = if (isOverallUp) Color(0xFF00E676) else Color(0xFFFF1744)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A24), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(text = country.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Chỉ số $tabName hiện tại", color = Color.Gray, fontSize = 12.sp)
            }
            Text(text = String.format("%.1f Tỷ $", currentValue), color = chartColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // VẼ BIỂU ĐỒ CANVAS (Real-time nhấp nhô)
        if (history.size > 1) {
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val maxVal = history.maxOrNull() ?: 1.0
                val minVal = history.minOrNull() ?: 0.0
                val range = maxVal - minVal
                val paddedRange = if (range == 0.0) 1.0 else range * 1.2 // Thêm padding cho đẹp

                val widthPerPoint = size.width / (14) // 15 điểm thì có 14 khoảng
                val path = Path()

                history.forEachIndexed { index, value ->
                    // Tính tọa độ Y: Đảo ngược vì Y=0 là ở trên cùng Canvas
                    val normalizedY = 1f - ((value - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()
                    val x = index * widthPerPoint
                    val y = normalizedY * size.height

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        // Tính toán vẽ đường cong Bezier cho mượt
                        val prevX = (index - 1) * widthPerPoint
                        val prevNormalizedY = 1f - ((history[index - 1] - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()
                        val prevY = prevNormalizedY * size.height

                        val controlPointX = (x + prevX) / 2
                        path.cubicTo(controlPointX, prevY, controlPointX, y, x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = chartColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Vẽ chấm tròn ở điểm dữ liệu mới nhất
                val lastY = 1f - ((history.last() - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width, lastY * size.height)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("Đang thu thập dữ liệu nhịp đập...", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}

// --- COMPONENT: ITEM BẢNG XẾP HẠNG ---
@Composable
fun CountryStatCard(rank: Int, countryName: String, value: Double, diff: Double, isUp: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val changeColor = if (isUp) Color(0xFF00E676) else Color(0xFFFF1744)
    val bgColor = if (isSelected) Color(0xFF2962FF).copy(alpha = 0.15f) else Color(0xFF1E1E24)
    val borderColor = if (isSelected) Color(0xFF2962FF) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "#$rank", color = if (rank <= 3) Color(0xFFFFD600) else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(35.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = countryName, color = if (isSelected) Color.White else Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = String.format("%.1f", value), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (diff > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (isUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, contentDescription = null, tint = changeColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = String.format("%.2f", diff), color = changeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun getDynamicValue(country: CountryData, tabName: String): Double {
    return when (tabName) {
        "GDP" -> country.gdp
        "FDI Inflow" -> country.fdi
        "Trade Balance" -> country.tradeBalance
        "Reserves" -> country.reserves
        "External Debt" -> country.debt
        else -> 0.0
    }
}