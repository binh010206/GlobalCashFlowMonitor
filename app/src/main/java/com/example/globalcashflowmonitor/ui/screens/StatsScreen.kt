package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.globalcashflowmonitor.data.CountryData
import com.example.globalcashflowmonitor.data.MockData
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket

// --- MAPBOX IMPORTS ---
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.extension.style.terrain.generated.terrain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// ====================================================
// MODELS & DATA
// ====================================================
data class StatsSocketPayload(val countries: List<CountryData>, val flows: List<Any>? = null)
data class Historic3DData(val year: Int, val metrics: List<Float>)
data class Country3DModel(val id: String, val name: String, val centerLat: Double, val centerLng: Double, val history: List<Historic3DData>)

val mock3DCountries = listOf(
    Country3DModel("VN", "Việt Nam", 14.0583, 108.2772, listOf(
        Historic3DData(2020, listOf(343f, 28f, 19f, 95f, 130f, 17f)),
        Historic3DData(2021, listOf(366f, 31f, 4f, 109f, 132f, 18f)),
        Historic3DData(2022, listOf(409f, 22f, 11f, 85f, 134f, 19f)),
        Historic3DData(2023, listOf(430f, 36f, 28f, 88f, 135f, 16f)),
        Historic3DData(2024, listOf(460f, 39f, 32f, 92f, 138f, 18f))
    )),
    Country3DModel("US", "Hoa Kỳ", 38.9072, -77.0369, listOf(
        Historic3DData(2020, listOf(21060f, 150f, -650f, 130f, 27000f, 6f)),
        Historic3DData(2021, listOf(23315f, 380f, -845f, 240f, 29000f, 7f)),
        Historic3DData(2022, listOf(25462f, 285f, -948f, 235f, 31000f, 7f)),
        Historic3DData(2023, listOf(27360f, 388f, -1060f, 242f, 34000f, 7f)),
        Historic3DData(2024, listOf(28500f, 410f, -1100f, 250f, 35500f, 8f))
    )),
    Country3DModel("CN", "Trung Quốc", 39.9042, 116.4074, listOf(
        Historic3DData(2020, listOf(14687f, 144f, 535f, 3216f, 10000f, 45f)),
        Historic3DData(2021, listOf(17734f, 180f, 676f, 3250f, 12000f, 53f)),
        Historic3DData(2022, listOf(17963f, 189f, 877f, 3127f, 13500f, 51f)),
        Historic3DData(2023, listOf(17700f, 163f, 823f, 3225f, 14000f, 50f)),
        Historic3DData(2024, listOf(18200f, 175f, 850f, 3300f, 14500f, 52f))
    ))
)

val metricColors = listOf("#00E676", "#00B0FF", "#FFD600", "#E040FB", "#FF1744", "#FF9100")
val metricNames = listOf("GDP", "FDI", "Trade Bal.", "Reserves", "Ext. Debt", "Remittance")

fun loadGeoJsonFromAsset(context: Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ====================================================
// MÀN HÌNH CHÍNH (2D THỐNG KÊ)
// ====================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(isLoggedIn: Boolean, onNavigateToLogin: () -> Unit) {
    var show3DPremiumView by remember { mutableStateOf(false) }

    if (show3DPremiumView) {
        Map3DScreen(onClose = { show3DPremiumView = false })
        return
    }

    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }
    val focusManager = LocalFocusManager.current

    var detailedCountryId by remember { mutableStateOf(sharedPref.getString("PINNED_COUNTRY", "VN") ?: "VN") }
    var realTimeData by remember { mutableStateOf(MockData.topCountries) }
    var isConnected by remember { mutableStateOf(false) }
    var historyData by remember { mutableStateOf(mapOf<String, Map<String, List<Double>>>()) }

    val tabs = listOf("GDP", "FDI Inflow", "Trade Balance", "Reserves", "External Debt")
    var selectedTab by remember { mutableStateOf("GDP") }
    var searchQuery by remember { mutableStateOf("") }
    var showBottomLock by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val socket: Socket = IO.socket("https://globalcashflowbackend.onrender.com")
            socket.on("connect") { isConnected = true }
            socket.on("disconnect") { isConnected = false }

            socket.on("cashflow_update") { args: Array<Any> ->
                if (args.isNotEmpty()) {
                    try {
                        val payload = Gson().fromJson(args[0].toString(), StatsSocketPayload::class.java)
                        val newData = payload.countries
                        realTimeData = newData.sortedByDescending { getDynamicValue(it, selectedTab) }

                        val newHistory = historyData.toMutableMap()
                        tabs.forEach { tab ->
                            val tabMap = (newHistory[tab] ?: emptyMap()).toMutableMap()
                            newData.forEach { country ->
                                val currentList = tabMap[country.id] ?: emptyList()
                                val value = getDynamicValue(country, tab)
                                tabMap[country.id] = (currentList + value).takeLast(15)
                            }
                            newHistory[tab] = tabMap
                        }
                        historyData = newHistory
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            socket.connect()
        } catch (e: Exception) { e.printStackTrace() }
    }

    val currentTabHistoryMap = historyData[selectedTab] ?: emptyMap()
    val filteredData = realTimeData.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A14))
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .padding(top = 45.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("THỐNG KÊ THỜI GIAN THỰC", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF1E1E24), RoundedCornerShape(20.dp))
                    .border(1.5.dp, if (isConnected) Color(0xFF00E676).copy(alpha = 0.8f) else Color(0xFFFF1744).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isConnected) "SYNCING" else "NO SIGNAL", color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm quốc gia (VD: Việt Nam, US...)", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color(0xFF2A2A35),
                    focusedContainerColor = Color(0xFF161622), unfocusedContainerColor = Color(0xFF161622),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            realTimeData = realTimeData.sortedByDescending { getDynamicValue(it, tab) }
                            focusManager.clearFocus()
                        },
                        label = { Text(tab, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF2962FF).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF40C4FF), labelColor = Color.Gray, containerColor = Color(0xFF1E1E24)),
                        border = BorderStroke(1.dp, if (selectedTab == tab) Color(0xFF2962FF) else Color(0xFF333344))
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val detailedCountry = realTimeData.find { it.id == detailedCountryId } ?: realTimeData.firstOrNull()
            if (detailedCountry != null) {
                val countryHistory = currentTabHistoryMap[detailedCountry.id] ?: listOf(getDynamicValue(detailedCountry, selectedTab))
                val isPinned = sharedPref.getString("PINNED_COUNTRY", "") == detailedCountry.id

                DetailedChartPanel(
                    country = detailedCountry,
                    tabName = selectedTab,
                    history = countryHistory,
                    isPinned = isPinned,
                    onPinClick = {
                        if (isLoggedIn) {
                            sharedPref.edit().putString("PINNED_COUNTRY", detailedCountry.id).apply()
                            val temp = detailedCountryId
                            detailedCountryId = ""
                            detailedCountryId = temp
                        } else {
                            Toast.makeText(context, "Vui lòng đăng nhập để sử dụng chức năng Ghim!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("BẢNG XẾP HẠNG (CƠ BẢN)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredData, key = { it.id }) { country ->
                    val history = currentTabHistoryMap[country.id] ?: listOf(getDynamicValue(country, selectedTab))
                    val currentValue = history.lastOrNull() ?: 0.0
                    val previousValue = if (history.size > 1) history[history.size - 2] else currentValue
                    CountryStatCard(
                        rank = realTimeData.indexOf(country) + 1, country = country, value = currentValue,
                        diff = abs(currentValue - previousValue), isUp = currentValue >= previousValue,
                        isSelected = country.id == detailedCountryId,
                        onClick = {
                            detailedCountryId = country.id
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)) {
            AnimatedVisibility(
                visible = !showBottomLock,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showBottomLock = true },
                    containerColor = Color(0xFF00B8D4),
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -10) showBottomLock = true
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Khóa 3D", tint = Color.Black)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 80.dp)) {
            AnimatedVisibility(
                visible = showBottomLock,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 20) showBottomLock = false
                        }
                    }
                ) {
                    Button(
                        onClick = { if (isLoggedIn) show3DPremiumView = true else onNavigateToLogin() },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLoggedIn) Color(0xFF00B8D4) else Color(0xFF2A2A35)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if (isLoggedIn) Icons.Rounded.TravelExplore else Icons.Rounded.Lock, contentDescription = null, tint = if (isLoggedIn) Color.Black else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isLoggedIn) "MỞ SA BÀN 3D CHUYÊN SÂU" else "ĐĂNG NHẬP ĐỂ XEM SA BÀN 3D", color = if (isLoggedIn) Color.Black else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val firstLetter = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
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

// ====================================================
// COMPONENT: CHART PANEL & CARD
// ====================================================
@Composable
fun DetailedChartPanel(
    country: CountryData,
    tabName: String,
    history: List<Double>,
    isPinned: Boolean = false,
    onPinClick: () -> Unit = {}
) {
    val currentValue = history.lastOrNull() ?: 0.0
    val previousValue = if (history.size > 1) history[history.size - 2] else currentValue
    val latestIsUp = currentValue >= previousValue
    val diff = abs(currentValue - previousValue)
    val themeColor = if (latestIsUp) Color(0xFF00E676) else Color(0xFFFF1744)

    var showLegend by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF161622), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = getFlagEmoji(country.id), fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = country.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    IconButton(onClick = onPinClick, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Rounded.PushPin, contentDescription = "Ghim", tint = if (isPinned) Color(0xFFFFD600) else Color(0xFF444455))
                    }
                }
                Text(text = "Chỉ số $tabName hiện tại", color = Color.Gray, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format("%.1f Tỷ $", currentValue), color = themeColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                if (history.size > 1 && diff > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = if (latestIsUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, contentDescription = null, tint = themeColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = String.format("%s %.2f", if (latestIsUp) "+" else "-", diff), color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.size > 1) {
            val maxVal = history.maxOrNull() ?: 1.0
            val minVal = history.minOrNull() ?: 0.0
            val range = maxVal - minVal
            val paddedRange = if (range == 0.0) 1.0 else range * 1.2

            val topLabel = minVal + paddedRange - (paddedRange * 0.1)
            val midLabel = minVal + (range / 2)
            val bottomLabel = minVal - (paddedRange * 0.1)

            Row(modifier = Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.fillMaxHeight().width(55.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    Text(text = String.format("%.1f", topLabel), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = String.format("%.1f", midLabel), color = Color(0xFF555566), fontSize = 10.sp)
                    Text(text = String.format("%.1f", bottomLabel), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Canvas(modifier = Modifier.fillMaxHeight().weight(1f)) {
                    val gridPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val gridColor = Color(0xFF333344)
                    drawLine(color = gridColor, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1f, pathEffect = gridPathEffect)
                    drawLine(color = gridColor, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 1f, pathEffect = gridPathEffect)
                    drawLine(color = gridColor, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1f, pathEffect = gridPathEffect)

                    val widthPerPoint = size.width / (history.size - 1).toFloat()
                    val fullPath = Path()

                    history.forEachIndexed { index, value ->
                        val normalizedY = 1f - ((value - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()
                        val x = index * widthPerPoint
                        val y = normalizedY * size.height

                        if (index == 0) {
                            fullPath.moveTo(x, y)
                        } else {
                            val prevX = (index - 1) * widthPerPoint
                            val prevY = (1f - ((history[index - 1] - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()) * size.height
                            val controlPointX = (x + prevX) / 2

                            fullPath.cubicTo(controlPointX, prevY, controlPointX, y, x, y)

                            val segmentPath = Path()
                            segmentPath.moveTo(prevX, prevY)
                            segmentPath.cubicTo(controlPointX, prevY, controlPointX, y, x, y)

                            val isSegmentUp = value >= history[index - 1]
                            val segmentColor = if (isSegmentUp) Color(0xFF00E676) else Color(0xFFFF1744)

                            drawPath(path = segmentPath, color = segmentColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(fullPath)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(themeColor.copy(alpha = 0.35f), Color.Transparent))
                    )

                    history.forEachIndexed { index, value ->
                        val normalizedY = 1f - ((value - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()
                        val x = index * widthPerPoint
                        val y = normalizedY * size.height
                        val markerSize = 3.5.dp.toPx()

                        if (index > 0) {
                            val isPointUp = value >= history[index - 1]
                            if (isPointUp) {
                                drawRect(color = Color(0xFF00E676), topLeft = Offset(x - markerSize, y - markerSize), size = Size(markerSize * 2, markerSize * 2))
                            } else {
                                val trianglePath = Path().apply {
                                    moveTo(x - markerSize, y - markerSize)
                                    lineTo(x + markerSize, y - markerSize)
                                    lineTo(x, y + markerSize)
                                    close()
                                }
                                drawPath(path = trianglePath, color = Color(0xFFFF1744))
                            }
                        } else {
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
                        }
                    }

                    val lastY = (1f - ((currentValue - minVal + (paddedRange * 0.1)) / paddedRange).toFloat()) * size.height
                    drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = Offset(size.width, lastY))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("Đang phân tích tín hiệu...", color = Color.DarkGray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0xFF2A2A35), thickness = 1.dp)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showLegend = !showLegend }.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chú thích đồ thị mô phỏng", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Icon(imageVector = if (showLegend) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
            }

            AnimatedVisibility(visible = showLegend) {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E28), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF00E676)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vuông Xanh: Chu kỳ Tăng trưởng (Uptrend)", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▼", color = Color(0xFFFF1744), fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tam giác Đỏ: Chu kỳ Suy giảm (Downtrend)", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("*Độ nhiễu thị trường (Market Noise): 0.05%", color = Color.DarkGray, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
fun CountryStatCard(rank: Int, country: CountryData, value: Double, diff: Double, isUp: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val changeColor = if (isUp) Color(0xFF00E676) else Color(0xFFFF1744)
    val bgColor = if (isSelected) Color(0xFF2962FF).copy(alpha = 0.15f) else Color(0xFF161622)
    val borderColor = if (isSelected) Color(0xFF2962FF) else Color(0xFF2A2A35)

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(bgColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "#$rank", color = if (rank <= 3) Color(0xFFFFD600) else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(35.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = getFlagEmoji(country.id), fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = country.name, color = if (isSelected) Color.White else Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

// ====================================================
// SA BÀN 3D PREMIUM
// ====================================================

@Composable
fun RadarChart(metrics: List<Float>, colors: List<String>, modifier: Modifier = Modifier) {
    val animatedMetrics = metrics.map { animateFloatAsState(targetValue = it, animationSpec = tween(800)).value }
    val maxVal = animatedMetrics.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Canvas(modifier = modifier.padding(10.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val anglePerMetric = (2 * PI) / metrics.size

        for (step in 1..3) {
            val r = radius * (step / 3f)
            val path = Path()
            for (i in metrics.indices) {
                val angle = i * anglePerMetric - PI / 2
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = Color(0xFF333344), style = Stroke(1f))
        }

        for (i in metrics.indices) {
            val angle = i * anglePerMetric - PI / 2
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            drawLine(color = Color(0xFF333344), start = center, end = Offset(x, y), strokeWidth = 1f)
        }

        val dataPath = Path()
        val dataPoints = mutableListOf<Offset>()

        for (i in animatedMetrics.indices) {
            val normalizedVal = animatedMetrics[i] / maxVal
            val displayRadius = radius * (normalizedVal.coerceAtLeast(0.15f))

            val angle = i * anglePerMetric - PI / 2
            val x = center.x + displayRadius * cos(angle).toFloat()
            val y = center.y + displayRadius * sin(angle).toFloat()

            dataPoints.add(Offset(x, y))
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(dataPath, color = Color(0xFF00B8D4).copy(alpha = 0.4f))
        drawPath(dataPath, color = Color(0xFF00B8D4), style = Stroke(4f, join = StrokeJoin.Round))

        dataPoints.forEachIndexed { i, point ->
            drawCircle(color = Color(android.graphics.Color.parseColor(colors[i])), radius = 8f, center = point)
        }
    }
}