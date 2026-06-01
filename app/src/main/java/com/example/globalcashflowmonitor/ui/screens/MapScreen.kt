package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.globalcashflowmonitor.network.CountryData
import com.example.globalcashflowmonitor.network.RetrofitClient
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.animation.flyTo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    isLoggedIn: Boolean,
    isDarkMode: Boolean,
    targetCountryId: String?,
    onZoomCompleted: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State lưu Dữ liệu Vĩ mô kéo từ Backend
    var allCountriesData by remember { mutableStateOf<List<CountryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State điều khiển Thanh trượt Heatmap (Mô phỏng theo năm)
    var selectedYear by remember { mutableStateOf(2024) }
    var selectedMetric by remember { mutableStateOf("GDP") }

    // State điều khiển BottomSheet hiển thị số liệu khi bấm vào cờ
    var selectedCountryId by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // References đến Mapbox Components
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var mapboxView by remember { mutableStateOf<MapView?>(null) }

    // 1. KÉO API LẤY TRỌN GÓI DATA 36 NƯỚC KHI MỞ APP
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getAllData()
            if (response.success) {
                allCountriesData = response.data
            } else {
                Toast.makeText(context, "Lỗi phản hồi từ server", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MapScreen", "Lỗi kéo API: ${e.message}")
            Toast.makeText(context, "Không thể kết nối Server Render", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // Xử lý sự kiện Zoom từ MainActivity gửi qua (Nếu có)
    LaunchedEffect(targetCountryId, mapboxView) {
        if (!targetCountryId.isNullOrEmpty() && mapboxView != null) {
            val coords = getCountryCoordinates(targetCountryId)
            if (coords.longitude() != 0.0) {
                mapboxView?.getMapboxMap()?.flyTo(
                    CameraOptions.Builder().center(coords).zoom(4.5).build()
                )
                onZoomCompleted()
            }
        }
    }

    // 2. THUẬT TOÁN BẢN ĐỒ NHIỆT: TỰ ĐỘNG PHÌNH TO/ĐỔI MÀU THEO SỐ LIỆU THẬT
    LaunchedEffect(allCountriesData, selectedYear, selectedMetric, pointAnnotationManager) {
        if (allCountriesData.isNotEmpty() && pointAnnotationManager != null) {
            // Xóa sạch vết cũ trước khi vẽ năm mới
            pointAnnotationManager!!.deleteAll()

            // Tìm giá trị lớn nhất (Max Value) của năm đó để làm mốc tính tỷ lệ phần trăm
            var maxValue = 0f
            allCountriesData.forEach { country ->
                val metric = country.metrics.find { it.name.equals(selectedMetric, ignoreCase = true) }
                val node = metric?.history?.find { it.year == selectedYear }
                if (node != null && node.value > maxValue) {
                    maxValue = node.value
                }
            }

            // Quét 36 nước để đóng dấu Chấm nhiệt lên Bản đồ
            allCountriesData.forEach { country ->
                val metric = country.metrics.find { it.name.equals(selectedMetric, ignoreCase = true) }
                val value = metric?.history?.find { it.year == selectedYear }?.value ?: 0f

                val coordinates = getCountryCoordinates(country.countryId)
                if (coordinates.longitude() != 0.0) {

                    // Tính toán tỷ lệ màu sắc
                    val ratio = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
                    val color = when {
                        ratio > 0.7f -> Color(0xFFFF1744)  // Đỏ rực (Dòng tiền cực khủng)
                        ratio > 0.3f -> Color(0xFFFF9100)  // Cam (Trung bình khá)
                        ratio > 0.05f -> Color(0xFF00E676) // Xanh lá (Bình thường)
                        else -> Color(0xFF757575)          // Xám (Dữ liệu quá thấp)
                    }
                    val radius = 22f + (ratio * 55f) // Bán kính phình to bám sát số liệu

                    val bitmap = createHeatmapCircle(radius, color.copy(alpha = 0.65f).toArgb())

                    val pointOptions = PointAnnotationOptions()
                        .withPoint(coordinates)
                        .withIconImage(bitmap)
                        .withTextField(country.countryId) // Định danh mã nước (VN, USA)
                        .withTextColor(android.graphics.Color.WHITE) // Fix lỗi WHITE thuần Android
                        .withTextSize(11.0)
                        .withTextOffset(listOf(0.0, 0.0))

                    pointAnnotationManager!!.create(pointOptions)
                }
            }
        }
    }

    // 3. BỐ CỤC GIAO DIỆN CHÍNH (BOX ĐÈ LÊN NHAU)
    Box(modifier = Modifier.fillMaxSize()) {

        // --- LỚP NỀN: BẢN ĐỒ 3D MAPBOX ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapboxMap().loadStyleUri(if (isDarkMode) Style.DARK else Style.LIGHT)
                    val annotationApi = this.annotations
                    val manager = annotationApi.createPointAnnotationManager()

                    // FIX LỖI KINH ĐIỂN: Thêm ClickListener đúng chuẩn SDK v11 Lambda
                    manager.addClickListener { annotation ->
                        val countryCode = annotation.textField
                        if (!countryCode.isNullOrEmpty()) {
                            selectedCountryId = countryCode
                            showBottomSheet = true

                            getMapboxMap().flyTo(
                                CameraOptions.Builder()
                                    .center(annotation.point)
                                    .zoom(4.0)
                                    .build()
                            )
                        }
                        true // Trả về true để xác nhận đã nuốt sự kiện click thành công
                    }

                    pointAnnotationManager = manager
                    mapboxView = this
                }
            }
        )

        // --- LỚP TRÊN: THANH SEARCH / TRỢ LÝ AI (GIỮ NGUYÊN FORM DESIGN CỦA MÀY) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 45.dp, start = 16.dp, end = 16.dp)
                .background(if (isDarkMode) Color(0xDD101010) else Color(0xDDFFFFFF), RoundedCornerShape(24.dp))
                .clickable { /* Sau này mở Chatbot AI ở đây */ }
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = "AI Search", tint = if (isDarkMode) Color(0xFFFFC107) else Color.DarkGray)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Hỏi Trợ lý AI về Dòng tiền toàn cầu...",
                color = if (isDarkMode) Color.LightGray else Color.Gray,
                fontSize = 14.sp
            )
        }

        // --- LỚP TRÊN: BẢNG ĐIỀU KHIỂN THỜI GIAN VÀ CHỈ SỐ HEATMAP ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp) // Đẩy cao lên để không bị che khuất bởi BottomBar của MainActivity
        ) {
            HeatmapController(
                selectedYear = selectedYear,
                onYearChange = { selectedYear = it },
                selectedMetric = selectedMetric,
                onMetricChange = { selectedMetric = it },
                isDarkMode = isDarkMode
            )
        }

        // Vòng xoay khi đang tải dữ liệu lần đầu
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFFFC107)
            )
        }
    }

    // 4. BOTTOM SHEET: HIỂN THỊ KHI BẤM CHỌN 1 QUỐC GIA
    if (showBottomSheet && selectedCountryId != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = if (isDarkMode) Color(0xFF101010) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Text(
                    text = "PHÂN TÍCH QUỐC GIA: $selectedCountryId",
                    color = if (isDarkMode) Color(0xFFFFC107) else Color(0xFF1976D2),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))

                // NƠI ĐẮP FILE STATSSCREEN.KT CỦA MÀY VÀO
                Text(
                    text = "Dữ liệu thực tế của mã nước [$selectedCountryId] đã được nạp sẵn.\nMày chỉ việc gọi Component đồ thị từ file StatsScreen.kt vào đây để hoàn chỉnh chức năng xem chi tiết!",
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// ==========================================================
// SUB-COMPONENT: THANH ĐIỀU KHIỂN MÔ PHỎNG DÒNG TIỀN THEO NĂM
// ==========================================================
@Composable
fun HeatmapController(
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    selectedMetric: String,
    onMetricChange: (String) -> Unit,
    isDarkMode: Boolean
) {
    val metrics = listOf("GDP", "FDI Inflows", "FDI Outflows", "Exports", "Imports")

    Card(
        modifier = Modifier.fillMaxWidth(0.92f),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xDD151515) else Color(0xDDFFFFFF)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hàng chọn bộ lọc các loại dòng tiền (Cuộn ngang)
            ScrollableTabRow(
                selectedTabIndex = metrics.indexOf(selectedMetric),
                containerColor = Color.Transparent,
                indicator = {}, divider = {}
            ) {
                metrics.forEach { metric ->
                    val isSelected = metric == selectedMetric
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .background(
                                color = if (isSelected) Color(0xFFFFC107) else (if (isDarkMode) Color(0xFF252525) else Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onMetricChange(metric) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = metric,
                            color = if (isSelected) Color.Black else (if (isDarkMode) Color.White else Color.DarkGray),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thanh kéo dòng thời gian (Slider 2020 -> 2024)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("2020", color = if (isDarkMode) Color.LightGray else Color.DarkGray, fontSize = 12.sp)
                Slider(
                    value = selectedYear.toFloat(),
                    onValueChange = { onYearChange(it.toInt()) },
                    valueRange = 2020f..2024f,
                    steps = 3,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFC107),
                        activeTrackColor = Color(0xFFFFC107),
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                    )
                )
                Text("2024", color = if (isDarkMode) Color.LightGray else Color.DarkGray, fontSize = 12.sp)
            }

            Text(
                text = "NĂM MÔ PHỎNG: $selectedYear",
                color = Color(0xFFFFC107),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ==========================================================
// HÀM HELPER: VẼ BITMAP HÌNH TRÒN ĐỂ ĐÓNG DẤU NHIỆT LÊN MAP
// ==========================================================
fun createHeatmapCircle(radius: Float, colorInt: Int): Bitmap {
    val size = (radius * 2).toInt().coerceAtLeast(15)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(radius, radius, radius, paint)
    return bitmap
}

// ==========================================================
// TỪ ĐIỂN TOẠ ĐỘ: ĐỔI MÃ CỦA 36 NƯỚC SANG TOẠ ĐỘ ĐỊA LÝ KHỚP CSV
// ==========================================================
fun getCountryCoordinates(countryId: String): Point {
    val coords = mapOf(
        "VN" to Point.fromLngLat(108.2022, 16.0544),
        "USA" to Point.fromLngLat(-95.7129, 37.0902),
        "CHN" to Point.fromLngLat(104.1954, 35.8617),
        "JPN" to Point.fromLngLat(138.2529, 36.2048),
        "DEU" to Point.fromLngLat(10.4515, 51.1657),
        "GBR" to Point.fromLngLat(-3.4360, 55.3781),
        "FRA" to Point.fromLngLat(2.2137, 46.2276),
        "IND" to Point.fromLngLat(78.9629, 20.5937),
        "KOR" to Point.fromLngLat(127.7669, 35.9078),
        "RUS" to Point.fromLngLat(105.3188, 61.5240),
        "AUS" to Point.fromLngLat(133.7751, -25.2744),
        "BRA" to Point.fromLngLat(-51.9253, -14.2350),
        "CAN" to Point.fromLngLat(-106.3468, 56.1304),
        "MEX" to Point.fromLngLat(-102.5528, 23.6345),
        "SGP" to Point.fromLngLat(103.8198, 1.3521),
        "ARG" to Point.fromLngLat(-63.6167, -38.4161),
        "IDN" to Point.fromLngLat(113.9213, -0.7893),
        "THA" to Point.fromLngLat(100.9925, 15.8700),
        "MYS" to Point.fromLngLat(101.9758, 4.2105),
        "PHL" to Point.fromLngLat(121.7740, 12.8797)
    )
    return coords[countryId.uppercase()] ?: Point.fromLngLat(0.0, 0.0)
}