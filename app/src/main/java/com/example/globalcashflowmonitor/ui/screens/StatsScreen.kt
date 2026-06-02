package com.example.globalcashflowmonitor.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.globalcashflowmonitor.network.CountryTimeline
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(countries: List<CountryTimeline>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedYear by remember { mutableIntStateOf(2024) }
    val years = (2020..2024).toList().reversed()
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    val metricsTabs = listOf("GDP", "FDI Inflow", "FDI Outflow", "Export", "Import")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentMetric = metricsTabs[selectedTabIndex]

    // AI States
    var showAiBot by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("Trợ lý AI đang phân tích dữ liệu...") }
    var isAiOnline by remember { mutableStateOf(true) }

    // Xử lý dữ liệu
    val sortedCountries = countries.sortedByDescending { getMetricValue(it, currentMetric, selectedYear) }
    val top1Country = sortedCountries.firstOrNull()
    val maxVal = getMetricValue(top1Country, currentMetric, selectedYear).coerceAtLeast(1.0)

    // Tính tổng toàn cầu
    val totalGlobalValue = countries.sumOf { getMetricValue(it, currentMetric, selectedYear) }
    val prevTotalGlobalValue = countries.sumOf { getMetricValue(it, currentMetric, selectedYear - 1) }
    val globalGrowth = if (prevTotalGlobalValue > 0) ((totalGlobalValue - prevTotalGlobalValue) / prevTotalGlobalValue) * 100 else 0.0

    Scaffold(
        containerColor = Color(0xFF0F0F1A),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAiBot = true
                    val top5Names = sortedCountries.take(5).joinToString(", ") { it.countryName }
                    val prompt = "Đóng vai chuyên gia kinh tế World Bank. Phân tích ngắn gọn (dưới 80 chữ) tại sao 5 nước: $top5Names lại dẫn đầu thế giới về $currentMetric trong năm $selectedYear."

                    aiResponse = "🤖 AI: Đang phân tích Bảng xếp hạng $currentMetric năm $selectedYear...\n\n"

                    com.example.globalcashflowmonitor.network.RetrofitClient.instance.sendAiMessage(com.example.globalcashflowmonitor.network.ChatRequest(prompt))
                        .enqueue(object : retrofit2.Callback<com.example.globalcashflowmonitor.network.ChatResponse> {
                            override fun onResponse(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, response: retrofit2.Response<com.example.globalcashflowmonitor.network.ChatResponse>) {
                                val aiData = response.body()?.data
                                if (aiData != null) {
                                    aiResponse += aiData.reply
                                    isAiOnline = true
                                }
                            }
                            override fun onFailure(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, t: Throwable) {
                                aiResponse += "❌ Lỗi mạng hoặc API Key hết hạn."
                                isAiOnline = false
                            }
                        })
                },
                containerColor = Color(0xFF00B0FF),
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.AutoAwesome, "Phân tích AI")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // HEADER & YEAR FILTER
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Macro Leaderboard", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Bảng xếp hạng Vĩ mô Toàn cầu", fontSize = 12.sp, color = Color.Gray)
                }

                Box {
                    OutlinedButton(
                        onClick = { yearDropdownExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Cyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Cyan)
                    ) {
                        Text("Năm $selectedYear", fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = yearDropdownExpanded,
                        onDismissRequest = { yearDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E2E))
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString(), color = Color.White) },
                                onClick = { selectedYear = year; yearDropdownExpanded = false }
                            )
                        }
                    }
                }
            }

            // TABS METRICS
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color.Cyan,
                edgePadding = 16.dp,
                divider = {}
            ) {
                metricsTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = Color.Cyan,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // MAIN LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // GLOBAL OVERVIEW CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x3300B0FF), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("TỔNG QUAN TOÀN CẦU ($selectedYear)", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${String.format("%,.0f", totalGlobalValue)} Tỷ USD", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)

                            if (selectedYear > 2020) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    val isUp = globalGrowth >= 0
                                    Icon(if (isUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, null, tint = if(isUp) Color(0xFF00E676) else Color(0xFFFF1744), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${if (isUp) "+" else ""}${String.format("%.1f", globalGrowth)}% so với ${selectedYear-1}", color = if(isUp) Color(0xFF00E676) else Color(0xFFFF1744), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            // RADAR CHART CHO TOP 1
                            if (top1Country != null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Color(0x33FFFFFF))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("HỒ SƠ QUỐC GIA TOP 1: ${top1Country.countryName}", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    RadarChart(country = top1Country, year = selectedYear)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("BẢNG XẾP HẠNG TOP 10", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }

                // LEADERBOARD ROWS
                itemsIndexed(sortedCountries.take(10)) { index, country ->
                    val value = getMetricValue(country, currentMetric, selectedYear)
                    val targetProgress = (value / maxVal).toFloat().coerceIn(0f, 1f)
                    val progress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(1500))

                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700) // Vàng
                        1 -> Color(0xFFC0C0C0) // Bạc
                        2 -> Color(0xFFCD7F32) // Đồng
                        else -> Color(0xFF333344)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF161624)).padding(12.dp)
                    ) {
                        // Số thứ tự / Huy chương
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(rankColor), contentAlignment = Alignment.Center) {
                            if (index < 3) Icon(Icons.Rounded.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            else Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        // Cờ
                        val bitmap = getCachedCircularFlag(context, getIso3Code(getSmartIsoCode(country)))
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))

                        Spacer(Modifier.width(12.dp))

                        // Tên + Progress Bar
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(getSmartCountryName(country), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${String.format("%,.1f", value)} Tỷ", color = Color.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A3A))) {
                                Box(modifier = Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(
                                    Brush.horizontalGradient(listOf(Color(0xFF00B0FF), Color(0xFF00E676)))
                                ))
                            }
                        }
                    }
                }
            }
        }
    }

    // BOTTOM SHEET AI
    if (showAiBot) {
        ModalBottomSheet(onDismissRequest = { showAiBot = false }, containerColor = Color(0xFF121224)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SmartToy, null, tint = Color(0xFF00B0FF), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI Chuyên Gia Vĩ Mô", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(if(isAiOnline) Color.Green else Color.Red, CircleShape))
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1AFFFFFF)).padding(16.dp)) {
                    Text(aiResponse, color = Color.LightGray, fontSize = 14.sp, lineHeight = 22.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ==========================================
// COMPOSABLE: BIỂU ĐỒ RADAR (HÌNH NHỆN)
// ==========================================
@Composable
fun RadarChart(country: CountryTimeline, year: Int) {
    val metrics = listOf("GDP", "FDI Inflow", "FDI Outflow", "Export", "Import")
    // Lấy giá trị thật
    val rawValues = metrics.map { getMetricValue(country, it, year) }
    // Chuẩn hóa dữ liệu để vẽ (vì GDP rất to, FDI nhỏ, cần logarit hoặc chia tỷ lệ để vẽ đẹp)
    // Ở đây ta dùng log10 để thu hẹp khoảng cách hiển thị trên radar
    val logValues = rawValues.map { if (it > 1) Math.log10(it) else 0.0 }
    val maxLogVal = logValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    val animatedValues = logValues.map {
        animateFloatAsState(targetValue = (it / maxLogVal).toFloat(), animationSpec = tween(1500)).value
    }

    Canvas(modifier = Modifier.size(180.dp)) {
        val radius = size.width / 2f
        val center = Offset(radius, radius)
        val anglePerMetric = (2 * PI) / metrics.size

        // Vẽ 3 vòng mạng nhện nền
        for (step in 1..3) {
            val stepRadius = radius * (step / 3f)
            val path = Path()
            for (i in metrics.indices) {
                val angle = i * anglePerMetric - PI / 2
                val x = center.x + stepRadius * cos(angle).toFloat()
                val y = center.y + stepRadius * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = Color(0xFF333344), style = Stroke(1.5f))
        }

        // Vẽ các trục chéo và tên chỉ số
        for (i in metrics.indices) {
            val angle = i * anglePerMetric - PI / 2
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            drawLine(color = Color(0xFF333344), start = center, end = Offset(x, y), strokeWidth = 1.5f)

            // Vẽ Text nhãn
            val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; textSize = 22f; textAlign = android.graphics.Paint.Align.CENTER }
            val textX = center.x + (radius + 25f) * cos(angle).toFloat()
            val textY = center.y + (radius + 25f) * sin(angle).toFloat()
            drawContext.canvas.nativeCanvas.drawText(metrics[i], textX, textY, textPaint)
        }

        // Vẽ vùng dữ liệu màu xanh
        val dataPath = Path()
        for (i in animatedValues.indices) {
            val displayRadius = radius * animatedValues[i]
            val angle = i * anglePerMetric - PI / 2
            val x = center.x + displayRadius * cos(angle).toFloat()
            val y = center.y + displayRadius * sin(angle).toFloat()

            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            // Chấm tròn ở đỉnh
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
        }
        dataPath.close()

        drawPath(dataPath, color = Color(0x6600B0FF), style = Fill)
        drawPath(dataPath, color = Color(0xFF00B0FF), style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}