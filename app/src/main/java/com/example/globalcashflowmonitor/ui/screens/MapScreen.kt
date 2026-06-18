package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Canvas
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.globalcashflowmonitor.network.CountryTimeline
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import kotlin.math.abs

// ==========================================
// BIẾN TOÀN CỤC & DATA CLASSES
// ==========================================
val globalFlagCache = mutableMapOf<String, Bitmap>()
var globalHasShownSyncNoti = false

class MapStateManager { var pointManager: PointAnnotationManager? = null }
data class FlowPartner(val targetIso: String, val amount: Double)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MapScreen(
    isLoggedIn: Boolean = false,
    isDarkMode: Boolean = true,
    targetCountryId: String? = null,
    onZoomCompleted: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }
    var rawMapView by remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
    val mapStateManager = remember { MapStateManager() }

    var realCountriesList by remember { mutableStateOf<List<CountryTimeline>>(emptyList()) }
    var isConnected by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<CountryTimeline?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var showAiBot by remember { mutableStateOf(false) }

    // Quản lý Thông báo
    var isNotiBoxVisible by remember { mutableStateOf(false) }
    var notiTitle by remember { mutableStateOf("") }
    var notiContent by remember { mutableStateOf("") }

    var chatInput by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("Trợ lý AI sẵn sàng! Bạn muốn phân tích điều gì?") }
    var isAiOnline by remember { mutableStateOf(true) }
    var isCacheReady by remember { mutableStateOf(false) }

    // Simulation States
    var isSimulating by remember { mutableStateOf(false) }
    var simCountry by remember { mutableStateOf<CountryTimeline?>(null) }
    var simPhase by remember { mutableStateOf("IN") }
    var isSimPaused by remember { mutableStateOf(false) }
    var simPartners by remember { mutableStateOf<List<FlowPartner>>(emptyList()) }
    var countryScreenCoords by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var animProgress by remember { mutableFloatStateOf(0f) }

    val dataTabs = listOf("GDP", "FDI Inflow", "FDI Outflow", "Export", "Import")
    var currentCurrency by remember { mutableStateOf("USD") }

    val currencySymbol = when(currentCurrency) { "EUR" -> "€"; "VND" -> "đ"; else -> "$" }
    val currencyRate = when(currentCurrency) { "EUR" -> 0.92; "VND" -> 25400.0; else -> 1.0 }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = com.example.globalcashflowmonitor.network.RetrofitClient.instance.getAllTimelines()
                if (response.success && response.data.isNotEmpty()) {
                    realCountriesList = response.data
                    isConnected = true
                }
            } catch (e: Exception) {
                isConnected = false
            }
            delay(15000)
        }
    }

    LaunchedEffect(realCountriesList) {
        if (realCountriesList.isNotEmpty() && !isCacheReady) {
            withContext(Dispatchers.IO) {
                realCountriesList.forEach { getCachedCircularFlag(context, getIso3Code(getSmartIsoCode(it))) }
            }
            isCacheReady = true
        }
    }

    // HIỆN THÔNG BÁO TỰ ĐỘNG 1 LẦN DUY NHẤT LÚC MỚI VÀO
    LaunchedEffect(isConnected, isCacheReady) {
        if (isConnected && isCacheReady && !globalHasShownSyncNoti) {
            notiTitle = "Đồng bộ thành công"
            notiContent = "✅ Hệ thống đã nạp dữ liệu Vĩ mô của ${realCountriesList.size} quốc gia từ Cơ sở dữ liệu."
            isNotiBoxVisible = true
            globalHasShownSyncNoti = true
        }
    }

    // LOGIC VÒNG LẶP MÔ PHỎNG (60fps)
    LaunchedEffect(isSimulating, isSimPaused) {
        if (isSimulating) {
            while (true) {
                if (rawMapView != null && realCountriesList.isNotEmpty()) {
                    val map = mutableMapOf<String, Offset>()
                    realCountriesList.forEach { c ->
                        val iso = getIso3Code(getSmartIsoCode(c))
                        val coords = getCountryCoordinates(iso)
                        val pixel = rawMapView!!.mapboxMap.pixelForCoordinate(Point.fromLngLat(coords.first, coords.second))
                        map[iso] = Offset(pixel.x.toFloat(), pixel.y.toFloat())
                    }
                    countryScreenCoords = map
                }
                if (!isSimPaused) {
                    animProgress += 0.012f // Tốc độ bay
                    if (animProgress >= 1f) animProgress = 0f
                }
                delay(16)
            }
        }
    }

    // Đảo chiều FDI mỗi 7s
    LaunchedEffect(isSimulating, simPhase) {
        if (isSimulating) {
            delay(7000)
            if (!isSimPaused) simPhase = if (simPhase == "IN") "OUT" else "IN"
        }
    }

    val searchResults = if (searchQuery.isBlank()) emptyList() else realCountriesList.filter {
        it.countryName.contains(searchQuery, ignoreCase = true) || it.countryId.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        // ================= BẢN ĐỒ MAPBOX =================
        MapboxMap(
            modifier = Modifier.fillMaxSize().zIndex(0f),
            mapInitOptionsFactory = { ctx -> MapInitOptions(context = ctx, styleUri = Style.DARK, cameraOptions = CameraOptions.Builder().center(Point.fromLngLat(108.2022, 16.0544)).zoom(2.5).build()) }
        ) {
            MapEffect(isCacheReady) { mapView ->
                if (!isCacheReady) return@MapEffect
                mapboxMapRef = mapView.mapboxMap
                rawMapView = mapView

                if (mapStateManager.pointManager == null) {
                    mapStateManager.pointManager = mapView.annotations.createPointAnnotationManager().apply {
                        addClickListener { annotation ->
                            focusManager.clearFocus()
                            val clickedIso = annotation.getData()?.asString?.replace("\"", "") ?: ""
                            if (isSimulating) {
                                if (simCountry != null && getIso3Code(getSmartIsoCode(simCountry!!)) == clickedIso) {
                                    isSimPaused = !isSimPaused
                                }
                            } else {
                                val matchedCountry = realCountriesList.find { getIso3Code(getSmartIsoCode(it)) == clickedIso }
                                if (matchedCountry != null) selectedCountry = matchedCountry
                            }
                            true
                        }
                    }
                }

                val pManager = mapStateManager.pointManager!!
                pManager.deleteAll()

                if (realCountriesList.isNotEmpty()) {
                    val pointOptions = mutableListOf<PointAnnotationOptions>()
                    realCountriesList.forEach { country ->
                        val iso3 = getIso3Code(getSmartIsoCode(country))
                        val (lng, lat) = getCountryCoordinates(iso3)
                        if (lng != 0.0 && lat != 0.0) {
                            pointOptions.add(PointAnnotationOptions().withPoint(Point.fromLngLat(lng, lat)).withIconImage(getCachedCircularFlag(context, iso3)).withIconSize(1.1).withData(com.google.gson.JsonPrimitive(iso3)))
                        }
                    }
                    pManager.create(pointOptions)
                }
            }
        }

        // ================= HEADER TÌM KIẾM =================
        if (!isSimulating) {
            Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 16.dp, vertical = 20.dp).fillMaxWidth().zIndex(2f)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Gọi tên nước (VD: Viet Nam)...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = Color.Cyan) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(if (isConnected) Color.Green else Color.Red, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(text = if (isConnected) "LIVE" else "SYNC", color = if (isConnected) Color.Green else Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xD9101010)),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Cyan, unfocusedIndicatorColor = Color.Transparent)
                )

                AnimatedVisibility(visible = searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xE61E1E2E)).border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp)).heightIn(max = 200.dp)) {
                        items(searchResults) { country ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    focusManager.clearFocus()
                                    val selectedIso = getIso3Code(getSmartIsoCode(country))
                                    val (lng, lat) = getCountryCoordinates(selectedIso)
                                    if (lng != 0.0) mapboxMapRef?.flyTo(CameraOptions.Builder().center(Point.fromLngLat(lng, lat)).zoom(4.5).build())
                                    selectedCountry = country
                                    searchQuery = ""
                                }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(bitmap = getCachedCircularFlag(context, getIso3Code(getSmartIsoCode(country))).asImageBitmap(), contentDescription = null, modifier = Modifier.size(30.dp).clip(CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(getSmartCountryName(country), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0x33FFFFFF))
                        }
                    }
                }
            }
        }

        // ================= GIAO DIỆN THÔNG BÁO (SMOOTH FADE UI) =================
        AnimatedVisibility(
            visible = isNotiBoxVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -100 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -100 }),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp, start = 16.dp, end = 16.dp).zIndex(5f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E1E2E)).border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFFD700)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.NotificationsActive, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(notiTitle, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Radar Vĩ Mô Toàn Cầu", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(notiContent, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { isNotiBoxVisible = false }, // Tắt mượt mà
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344))
                    ) { Text("Đã đọc & Đóng", color = Color.White) }
                }
            }
        }

        // ================= CANVAS MÔ PHỎNG FDI (CYBERPUNK GLOW EFFECT) =================
        if (isSimulating && simCountry != null) {
            val baseIso = getIso3Code(getSmartIsoCode(simCountry!!))
            val baseCoord = countryScreenCoords[baseIso]

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).zIndex(3f)) // Overlay đen sâu hơn

            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xCC000000)).border(1.dp, if(simPhase == "IN") Color(0xFF00E676) else Color(0xFFFF1744), RoundedCornerShape(30.dp)).padding(horizontal = 20.dp, vertical = 10.dp).zIndex(4f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSimPaused) {
                    Icon(Icons.Rounded.PauseCircleFilled, null, tint = Color.Yellow)
                    Spacer(Modifier.width(8.dp))
                    Text("ĐANG TẠM DỪNG (CHẠM VÀO CỜ ĐỂ TIẾP TỤC)", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("MÔ PHỎNG FDI: ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = if(simPhase == "IN") " CHẢY VÀO (INFLOW)" else " CHẢY RA (OUTFLOW)", color = if(simPhase == "IN") Color(0xFF00E676) else Color(0xFFFF1744), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            FloatingActionButton(onClick = { isSimulating = false; rawMapView?.gestures?.updateSettings { scrollEnabled = true; pitchEnabled = true } }, containerColor = Color(0xFFFF1744), modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp).zIndex(4f).size(40.dp)) { Icon(Icons.Rounded.Close, null, tint = Color.White) }

            if (baseCoord != null && simPartners.isNotEmpty()) {
                ComposeCanvas(modifier = Modifier.fillMaxSize().zIndex(3f)) {
                    val lineColor = if (simPhase == "IN") Color(0xFF00E676) else Color(0xFFFF1744)

                    // Vẽ các tâm sáng (Glowing Hubs)
                    drawCircle(brush = Brush.radialGradient(listOf(lineColor.copy(alpha = 0.4f), Color.Transparent), center = baseCoord, radius = 100f), center = baseCoord, radius = 100f)

                    simPartners.forEach { partner ->
                        val targetCoord = countryScreenCoords[partner.targetIso]
                        if (targetCoord != null) {
                            val startP = if (simPhase == "IN") targetCoord else baseCoord
                            val endP = if (simPhase == "IN") baseCoord else targetCoord
                            val cx = (startP.x + endP.x) / 2
                            val cy = (startP.y + endP.y) / 2 - 250f // Uốn cong cao hơn

                            val path = Path().apply { moveTo(startP.x, startP.y); quadraticBezierTo(cx, cy, endP.x, endP.y) }

                            // Đường ray mờ
                            drawPath(path = path, color = lineColor.copy(alpha = 0.15f), style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)))

                            // HIỆU ỨNG SAO BĂNG (VỆT ĐUÔI MỜ DẦN)
                            for (i in 0..15) {
                                val tailT = (animProgress - i * 0.015f)
                                if (tailT > 0f && tailT < 1f) {
                                    val tailX = (1 - tailT) * (1 - tailT) * startP.x + 2 * (1 - tailT) * tailT * cx + tailT * tailT * endP.x
                                    val tailY = (1 - tailT) * (1 - tailT) * startP.y + 2 * (1 - tailT) * tailT * cy + tailT * tailT * endP.y
                                    val alpha = (1f - (i / 15f)).coerceIn(0f, 1f)
                                    val radius = 8.dp.toPx() * alpha
                                    drawCircle(color = lineColor.copy(alpha = alpha), radius = radius, center = Offset(tailX, tailY))
                                }
                            }

                            // Hạt đầu (Sáng chói)
                            val t = animProgress
                            val currentX = (1 - t) * (1 - t) * startP.x + 2 * (1 - t) * t * cx + t * t * endP.x
                            val currentY = (1 - t) * (1 - t) * startP.y + 2 * (1 - t) * t * cy + t * t * endP.y

                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(currentX, currentY))

                            // Chữ hiển thị số tiền bay
                            val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; isFakeBoldText = true }
                            drawContext.canvas.nativeCanvas.drawText("$${String.format("%.1f", partner.amount)}B", currentX + 20f, currentY - 20f, textPaint)
                        }
                    }
                }
            }
        }

        // ================= CÁC NÚT FAB MẶC ĐỊNH & THÔNG BÁO =================
        if (!isSimulating) {
            Column(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 120.dp, end = 16.dp).zIndex(2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Nút AI
                FloatingActionButton(onClick = { showAiBot = true }, containerColor = Color(0x99000000), contentColor = Color(0xFF00B0FF), modifier = Modifier.size(48.dp).border(1.dp, Color(0xFF00B0FF), RoundedCornerShape(12.dp))) {
                    Icon(Icons.Rounded.SmartToy, "Gemini AI")
                }
                // Nút Mở Thông Báo (Chuông Vàng)
                FloatingActionButton(
                    onClick = {
                        val types = listOf(
                            "FED vừa công bố biểu đồ lãi suất mới, Vốn FDI có thể đảo chiều.",
                            "Căng thẳng thương mại làm thay đổi chuỗi cung ứng Châu Á.",
                            "Dữ liệu GDP của Việt Nam vượt dự phóng của World Bank."
                        )
                        notiTitle = "Tin Tức Vĩ Mô"
                        notiContent = types.random()
                        isNotiBoxVisible = true
                    },
                    containerColor = Color(0x99000000), contentColor = Color(0xFFFFD700), modifier = Modifier.size(48.dp).border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Rounded.Notifications, "Mở thông báo")
                }
            }
        }

        // ================= BẢNG THỐNG KÊ CHI TIẾT (BOTTOM SHEET) =================
        if (selectedCountry != null) {
            ModalBottomSheet(onDismissRequest = { selectedCountry = null }, sheetState = sheetState, containerColor = Color(0xFF141424), modifier = Modifier.fillMaxHeight(0.9f)) {
                var localSelectedYear by remember { mutableIntStateOf(2024) }
                val pagerState = rememberPagerState(pageCount = { dataTabs.size })

                Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(bitmap = getCachedCircularFlag(context, getSmartIsoCode(selectedCountry!!)).asImageBitmap(), contentDescription = null, modifier = Modifier.size(52.dp).clip(CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(getSmartCountryName(selectedCountry!!), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Text("Đơn vị: ", color = Color.Gray, fontSize = 11.sp)
                                    listOf("USD", "EUR", "VND").forEach { curr -> Text(text = curr, color = if (currentCurrency == curr) Color.Cyan else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp).clickable { currentCurrency = curr }) }
                                }
                            }
                        }

                        Row {
                            OutlinedButton(onClick = { exportCountryToCsv(context, selectedCountry!!, dataTabs); Toast.makeText(context, "Đã lưu Excel vào máy!", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)), contentPadding = PaddingValues(horizontal = 10.dp)) { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    simCountry = selectedCountry
                                    val totalIn = getMetricValue(simCountry, "FDI Inflow", 2024).coerceAtLeast(10.0)
                                    val totalOut = getMetricValue(simCountry, "FDI Outflow", 2024).coerceAtLeast(10.0)
                                    val otherCountries = realCountriesList.filter { it.countryId != simCountry!!.countryId }.shuffled().take(5)
                                    val partners = mutableListOf<FlowPartner>()
                                    var remainingAmount = Math.max(totalIn, totalOut)

                                    otherCountries.forEachIndexed { index, c ->
                                        val amt = if (index == 4) remainingAmount else (remainingAmount * Math.random() * 0.4).coerceAtLeast(1.0)
                                        remainingAmount -= amt
                                        partners.add(FlowPartner(getIso3Code(getSmartIsoCode(c)), amt))
                                    }
                                    simPartners = partners

                                    rawMapView?.gestures?.updateSettings { scrollEnabled = false; pitchEnabled = false }
                                    val (lng, lat) = getCountryCoordinates(getIso3Code(getSmartIsoCode(simCountry!!)))
                                    mapboxMapRef?.flyTo(CameraOptions.Builder().center(Point.fromLngLat(lng, lat)).zoom(2.0).build())

                                    isSimulating = true; isSimPaused = false; simPhase = "IN"; selectedCountry = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)), contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Rounded.Tsunami, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("MÔ PHỎNG", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(320.dp)) { page ->
                        val currentTab = dataTabs[page]
                        val currentValue = getMetricValue(selectedCountry, currentTab, localSelectedYear) * currencyRate
                        val prevValue = getMetricValue(selectedCountry, currentTab, localSelectedYear - 1) * currencyRate
                        val diff = currentValue - prevValue
                        val isGrowth = diff >= 0
                        val diffStr = if (isGrowth) "+${String.format("%.1f", diff)}" else String.format("%.1f", diff)
                        val trendColor = if (isGrowth) Color(0xFF00E676) else Color(0xFFFF1744)

                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E2E)).border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)).padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(currentTab, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$currencySymbol ${String.format("%.1f", currentValue)} Tỷ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    if (localSelectedYear > 2020) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                            Icon(if (isGrowth) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null, tint = trendColor, modifier = Modifier.size(16.dp))
                                            Text("$diffStr (s ${localSelectedYear - 1})", color = trendColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(start = 45.dp, bottom = 20.dp, end = 10.dp, top = 10.dp)) {
                                ComposeCanvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { offset -> val years = 2020..2024; val sectionWidth = size.width / (years.count() - 1).coerceAtLeast(1); localSelectedYear = 2020 + Math.round(offset.x / sectionWidth).toInt().coerceIn(0, years.count() - 1) } }) {
                                    val years = 2020..2024; val pts = years.map { y -> getMetricValue(selectedCountry, currentTab, y) * currencyRate }; val maxV = pts.maxOrNull() ?: 1.0; val minV = pts.minOrNull() ?: 0.0; val range = if (maxV == minV) 1.0 else (maxV - minV)
                                    val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 24f }; val textPaintCenter = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; textSize = 26f; textAlign = android.graphics.Paint.Align.CENTER }
                                    drawContext.canvas.nativeCanvas.drawText("${String.format("%.0f", maxV)}", -110f, 15f, textPaint); drawContext.canvas.nativeCanvas.drawText("${String.format("%.0f", minV)}", -110f, size.height - 5f, textPaint)
                                    val path = Path(); val fillPath = Path(); val nodeCoordinates = mutableListOf<Offset>()
                                    pts.forEachIndexed { i, v -> val x = i * (size.width / (pts.size - 1).coerceAtLeast(1)); val y = size.height - ((v - minV) / range * size.height).toFloat(); nodeCoordinates.add(Offset(x,y)); drawContext.canvas.nativeCanvas.drawText("${2020+i}", x, size.height + 45f, textPaintCenter); if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, size.height); fillPath.lineTo(x, y) } else { path.lineTo(x, y); fillPath.lineTo(x, y) }; if (i == pts.size - 1) { fillPath.lineTo(x, size.height); fillPath.close() } }
                                    drawPath(fillPath, brush = Brush.verticalGradient(listOf(Color(0xFF00B0FF).copy(alpha = 0.4f), Color.Transparent)), style = Fill); drawPath(path, Color(0xFF00B0FF), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    nodeCoordinates.forEachIndexed { i, offset -> val isSelectedNode = (2020 + i) == localSelectedYear; drawCircle(color = if (isSelectedNode) Color.White else Color(0xFF00B0FF), radius = if (isSelectedNode) 7.dp.toPx() else 4.dp.toPx(), center = offset); if (isSelectedNode) { val textVal = String.format("%.1f", pts[i]); val tooltipPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 30f; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER }; drawRoundRect(color = Color(0xCC000000), topLeft = Offset(offset.x - 60f, offset.y - 70f), size = Size(120f, 50f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)); drawContext.canvas.nativeCanvas.drawText(textVal, offset.x, offset.y - 35f, tooltipPaint) } }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) { repeat(dataTabs.size) { iteration -> val color = if (pagerState.currentPage == iteration) Color.Cyan else Color.DarkGray; Box(modifier = Modifier.padding(3.dp).clip(CircleShape).background(color).size(8.dp)) } }
                    Text("Lịch sử ($currentCurrency)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(8.dp))

                    val currentTab = dataTabs[pagerState.currentPage]; val yearsList = (2024 downTo 2020).toList()
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).padding(bottom = 20.dp)) {
                        items(yearsList) { year ->
                            val v = getMetricValue(selectedCountry, currentTab, year) * currencyRate; val prevV = getMetricValue(selectedCountry, currentTab, year - 1) * currencyRate; val d = v - prevV; val isUp = d >= 0; val tColor = if (isUp) Color(0xFF00E676) else Color(0xFFFF1744)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(year.toString(), color = Color.Gray, fontWeight = FontWeight.Bold)
                                ComposeCanvas(modifier = Modifier.width(60.dp).height(24.dp)) { val p = Path(); val startY = if (isUp) size.height else 0f; val endY = if (isUp) 0f else size.height; p.moveTo(0f, startY); p.cubicTo(size.width * 0.4f, startY, size.width * 0.6f, endY, size.width, endY); drawPath(p, tColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)); drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(size.width, endY)) }
                                Column(horizontalAlignment = Alignment.End) { Text("$currencySymbol ${String.format("%.1f", v)} Tỷ", color = Color.White, fontWeight = FontWeight.Bold); if (year > 2020) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (isUp) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null, tint = tColor, modifier = Modifier.size(12.dp)); Text(String.format("%.1f", abs(d)), color = tColor, fontSize = 11.sp) } } }
                            }
                        }
                    }
                }
            }
        }

        // ================= AI BOT (ĐÃ BƠM NGỮ CẢNH VÀO PROMPT ĐỂ HẾT NGÁO) =================
        if (showAiBot) {
            ModalBottomSheet(onDismissRequest = { showAiBot = false }, containerColor = Color(0xFF121224)) {
                val chatScrollState = rememberScrollState()

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SmartToy, null, tint = Color(0xFF00B0FF), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Trợ lý AI", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(if(isAiOnline) Color.Green else Color.Red, CircleShape))
                        Text(if(isAiOnline) " ONLINE" else " OFFLINE", color = if(isAiOnline) Color.Green else Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x1AFFFFFF)).padding(14.dp).verticalScroll(chatScrollState)) {
                        Text(aiResponse, color = Color.LightGray, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = chatInput, onValueChange = { chatInput = it }, placeholder = { Text("Hỏi về quốc gia đang xem?") },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0x33000000), unfocusedContainerColor = Color(0x33000000), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).border(0.5.dp, Color.Gray, RoundedCornerShape(24.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                if (chatInput.isNotBlank()) {
                                    val q = chatInput; chatInput = ""
                                    aiResponse += "\n\n👤 Bạn: $q\n🤖 AI: Đang xử lý..."
                                    coroutineScope.launch { delay(100); chatScrollState.animateScrollTo(chatScrollState.maxValue) }

                                    // BƠM NGỮ CẢNH: Nếu đang bật bảng thống kê nước nào, nhét thẳng số liệu nước đó vào cho AI đọc!
                                    val contextData = selectedCountry?.let {
                                        " [Ngữ cảnh: Tôi đang xem bảng thống kê của nước ${it.countryName}. GDP năm 2024 là ${getMetricValue(it, "GDP", 2024)} Tỷ, FDI Inflow là ${getMetricValue(it, "FDI Inflow", 2024)} Tỷ. Hãy dựa vào đây để trả lời.]"
                                    } ?: " [Ngữ cảnh: Tôi đang ở màn hình Bản đồ Thế giới]"

                                    val smartQuestion = "$q $contextData"

                                    com.example.globalcashflowmonitor.network.RetrofitClient.instance.sendAiMessage(com.example.globalcashflowmonitor.network.ChatRequest(smartQuestion)).enqueue(object : retrofit2.Callback<com.example.globalcashflowmonitor.network.ChatResponse> {
                                        override fun onResponse(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, response: retrofit2.Response<com.example.globalcashflowmonitor.network.ChatResponse>) {
                                            val aiData = response.body()?.data
                                            if (aiData != null) {
                                                aiResponse = aiResponse.replace("🤖 AI: Đang xử lý...", "🤖 AI: ${aiData.reply}")
                                                isAiOnline = true
                                                coroutineScope.launch { delay(100); chatScrollState.animateScrollTo(chatScrollState.maxValue) }
                                                when (aiData.action) {
                                                    "ZOOM_TO" -> { val (lng, lat) = getCountryCoordinates(getIso3Code(aiData.targetId)); if (lng != 0.0) mapboxMapRef?.flyTo(CameraOptions.Builder().center(Point.fromLngLat(lng, lat)).zoom(4.5).build()); showAiBot = false }
                                                    "CLOSE_CHART" -> selectedCountry = null
                                                }
                                            }
                                        }
                                        override fun onFailure(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, t: Throwable) {
                                            aiResponse = aiResponse.replace("🤖 AI: Đang xử lý...", "🤖 AI: ❌ Lỗi kết nối OpenRouter! Kiểm tra lại API Key.")
                                            isAiOnline = false
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.clip(CircleShape).background(Color(0xFF00B0FF))
                        ) { Icon(Icons.Rounded.Send, null, tint = Color.White) }
                    }
                }
            }
        }
    }
}

// CÁC HÀM TIỆN ÍCH GIỮ NGUYÊN (Không cắt xén để khỏi lỗi)
fun exportCountryToCsv(context: Context, country: CountryTimeline, metrics: List<String>): Boolean {
    return try {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDir, "BaoCao_ViMo_${country.countryId.trim()}.csv")
        val writer = FileWriter(file)
        writer.append("Country,Metric,2020,2021,2022,2023,2024\n")
        metrics.forEach { metric ->
            writer.append("${country.countryName},$metric,")
            val rowData = (2020..2024).map { year -> getMetricValue(country, metric, year).toString() }
            writer.append(rowData.joinToString(","))
            writer.append("\n")
        }
        writer.flush(); writer.close()
        true
    } catch (e: Exception) { false }
}

fun getSmartIsoCode(country: CountryTimeline): String = if (country.countryId.length <= 3) country.countryId.trim() else country.countryName.trim()
fun getSmartCountryName(country: CountryTimeline): String = if (country.countryName.length > 3) country.countryName else country.countryId

fun getMetricValue(country: CountryTimeline?, tabName: String, targetYear: Int): Double {
    if (country == null) return 0.0
    val possibleNames = when (tabName) { "GDP" -> listOf("GDP", "gdp"); "FDI Inflow" -> listOf("FDI_In", "FDI Inflows", "FDI Inflow"); "FDI Outflow" -> listOf("FDI_Out", "FDI Outflows", "FDI Outflow"); "Export" -> listOf("Export", "Exports"); "Import" -> listOf("Import", "Imports"); else -> listOf(tabName) }
    return country.metrics.find { metric -> possibleNames.any { name -> metric.name.equals(name, ignoreCase = true) } }?.history?.find { it.year == targetYear }?.value ?: 0.0
}

fun getCountryCoordinates(id: String): Pair<Double, Double> {
    val cleanId = id.uppercase()
    return when(cleanId) { "VNM", "VN" -> Pair(108.2022, 16.0544); "USA", "US" -> Pair(-95.7129, 37.0902); "CHN", "CN" -> Pair(104.1954, 35.8617); "JPN", "JP" -> Pair(138.2529, 36.2048); "KOR", "KR" -> Pair(127.7669, 35.9078); "IND", "IN" -> Pair(78.9629, 20.5937); "GBR", "GB", "UK" -> Pair(-3.4359, 55.3781); "FRA", "FR" -> Pair(2.2137, 46.2276); "DEU", "DE" -> Pair(10.4515, 51.1657); "CAN", "CA" -> Pair(-106.3468, 56.1304); "BRA", "BR" -> Pair(-51.9253, -14.2350); "AUS", "AU" -> Pair(133.7751, -25.2744); "RUS", "RU" -> Pair(105.3188, 61.5240); "ITA", "IT" -> Pair(12.5674, 41.8719); "IDN", "ID" -> Pair(113.9213, -0.7893); "MEX", "MX" -> Pair(-102.5528, 23.6345); "ZAF", "ZA" -> Pair(22.9375, -30.5595); "SAU", "SA" -> Pair(45.0792, 23.8859); "ARG", "AR" -> Pair(-63.6167, -38.4161); "THA", "TH" -> Pair(100.9925, 15.8700); "MYS", "MY" -> Pair(101.9758, 4.2105); "SGP", "SG" -> Pair(103.8198, 1.3521); "PHL", "PH" -> Pair(121.7740, 12.8797); "ESP", "ES" -> Pair(-3.7492, 40.4637); "NLD", "NL" -> Pair(5.2913, 52.1326); "CHE", "CH" -> Pair(8.2275, 46.8182); "POL", "PL" -> Pair(19.1451, 51.9194); "SWE", "SE" -> Pair(18.6435, 60.1282); else -> Pair(0.0, 0.0) }
}

fun getIso3Code(id: String): String {
    val idUp = id.trim().uppercase()
    if (idUp.length == 3) return idUp
    val map = mapOf("VN" to "VNM", "US" to "USA", "CN" to "CHN", "JP" to "JPN", "KR" to "KOR", "IN" to "IND", "GB" to "GBR", "FR" to "FRA", "DE" to "DEU", "CA" to "CAN", "BR" to "BRA", "AU" to "AUS", "RU" to "RUS", "IT" to "ITA", "ID" to "IDN", "MX" to "MEX", "ZA" to "ZAF", "SA" to "SAU", "AR" to "ARG", "TH" to "THA", "MY" to "MYS", "SG" to "SGP", "PH" to "PHL", "ES" to "ESP", "NL" to "NLD")
    return map[idUp] ?: idUp
}

fun getIso2Code(id: String): String {
    val cleanId = id.trim().lowercase()
    val map = mapOf("vnm" to "vn", "usa" to "us", "chn" to "cn", "jpn" to "jp", "kor" to "kr", "ind" to "in", "gbr" to "gb", "fra" to "fr", "deu" to "de", "can" to "ca", "bra" to "br", "aus" to "au", "rus" to "ru", "ita" to "it", "idn" to "id", "mex" to "mx", "zaf" to "za", "sau" to "sa", "arg" to "ar", "tha" to "th", "mys" to "my", "sgp" to "sg", "phl" to "ph", "esp" to "es", "nld" to "nl", "che" to "ch", "pol" to "pl", "swe" to "se")
    return map[cleanId] ?: cleanId.take(2)
}

fun getCachedCircularFlag(context: Context, isoCode: String): Bitmap {
    val key = isoCode.uppercase()
    if (globalFlagCache.containsKey(key)) return globalFlagCache[key]!!
    val resourceName = getIso2Code(isoCode)
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    val finalBitmap = if (resId == 0) { createFallbackBitmap(isoCode) } else {
        val rawBitmap = BitmapFactory.decodeResource(context.resources, resId) ?: createFallbackBitmap(isoCode)
        val size = 110
        val output = Bitmap.createScaledBitmap(rawBitmap, size, size, true)
        val circleBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(circleBitmap)
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(output, 0f, 0f, paint)
        circleBitmap
    }
    globalFlagCache[key] = finalBitmap
    return finalBitmap
}

fun createFallbackBitmap(text: String): Bitmap {
    val size = 110
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#FF0033") }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.apply { color = android.graphics.Color.WHITE; textSize = 34f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    canvas.drawText(text.trim().take(3).uppercase(), size/2f, size/2f + 12f, paint)
    return bitmap
}