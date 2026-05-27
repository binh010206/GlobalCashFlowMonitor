package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun Map3DScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedYear by remember { mutableStateOf(2024f) }
    var showAiChat by remember { mutableStateOf(false) }

    // Dùng data Việt Nam làm gốc
    val vnData = mock3DCountries[0]
    val currentMetrics = vnData.history.find { it.year == selectedYear.toInt() }?.metrics ?: vnData.history.last().metrics

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ==========================================
        // 1. MAPBOX 3D - HIỆU ỨNG HÒN ĐẢO NỔI
        // ==========================================
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    val mapboxMap = getMapboxMap()
                    mapboxMap.loadStyleUri(Style.DARK) { style ->

                        // Đọc file vietnam.geojson (Nhớ chắc chắn mày đã bỏ vào thư mục assets)
                        val geoJsonString = loadGeoJsonFromAsset(ctx, "vietnam.geojson") ?: "{}"

                        style.addSource(geoJsonSource("country-source") { data(geoJsonString) })

                        // 🌟 LAYER 1: ĐỔ BÓNG / HÀO QUANG DƯỚI GẦM (Shadow)
                        style.addLayer(lineLayer("country-glow", "country-source") {
                            lineColor(Color(0xFF00E676).hashCode())
                            lineWidth(20.0)
                            lineBlur(15.0)
                            lineOpacity(0.5)
                        })

                        // 🌟 LAYER 2: KHỐI QUỐC GIA LƠ LỬNG (Extrusion) - Cái "Bản đồ Gỗ" của mày đây
                        val countryBaseHeight = 15000.0 // Nâng cả nước VN lên 15km khỏi mặt đất
                        val countryTopHeight = 20000.0  // Bề dày của khối là 5km

                        style.addLayer(fillExtrusionLayer("country-block", "country-source") {
                            fillExtrusionColor(Color(0xFF1E1E28).hashCode()) // Khối màu xám đen sang trọng
                            fillExtrusionBase(countryBaseHeight)
                            fillExtrusionHeight(countryTopHeight)
                            fillExtrusionOpacity(0.95)
                        })

                        // 🌟 LAYER 3: 6 CỘT KINH TẾ CẮM TRÊN BỀ MẶT KHỐI
                        style.addSource(geoJsonSource("pillars-source") { featureCollection(FeatureCollection.fromFeatures(emptyList())) })
                        style.addLayer(fillExtrusionLayer("pillars-layer", "pillars-source") {
                            fillExtrusionHeight(get("height"))
                            fillExtrusionColor(get("color"))
                            fillExtrusionBase(countryTopHeight) // Chân cột bắt đầu từ mặt trên của hòn đảo!
                            fillExtrusionOpacity(0.9)
                        })
                    }
                }
            },
            update = { mapView ->
                val mapboxMap = mapView.getMapboxMap()

                // Góc Camera chuẩn để thấy hòn đảo nổi
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(vnData.centerLng, vnData.centerLat))
                        .zoom(5.5)
                        .pitch(65.0)
                        .bearing((selectedYear - 2020) * 10.0)
                        .build()
                )

                // Tính toán 6 cột cắm trên bề mặt
                mapboxMap.getStyle { style ->
                    val features = mutableListOf<Feature>()
                    val radius = 1.0 // Bán kính tỏa ra từ tâm VN
                    val countryTopHeight = 20000.0

                    currentMetrics.forEachIndexed { i, value ->
                        val angle = (i * 60) * (PI / 180)
                        val pillarLng = vnData.centerLng + radius * cos(angle)
                        val pillarLat = vnData.centerLat + radius * sin(angle)

                        // Độ cao cột cộng thêm độ cao của hòn đảo
                        val displayHeight = countryTopHeight + (value / currentMetrics.maxOrNull()!!) * 250000.0
                        val d = 0.2 // Độ to của cột

                        val points = listOf(
                            Point.fromLngLat(pillarLng - d, pillarLat - d), Point.fromLngLat(pillarLng + d, pillarLat - d),
                            Point.fromLngLat(pillarLng + d, pillarLat + d), Point.fromLngLat(pillarLng - d, pillarLat + d),
                            Point.fromLngLat(pillarLng - d, pillarLat - d)
                        )
                        val polygon = Polygon.fromLngLats(listOf(points))
                        val feature = Feature.fromGeometry(polygon)
                        feature.addNumberProperty("height", displayHeight)
                        feature.addStringProperty("color", metricColors[i])
                        features.add(feature)
                    }

                    val pillarSource = style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("pillars-source")
                    pillarSource?.featureCollection(FeatureCollection.fromFeatures(features))
                }
            },
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )

        // ==========================================
        // 2. NÚT TRỞ VỀ (GÓC TRÁI TRÊN)
        // ==========================================
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 45.dp, start = 16.dp)
                .background(Color(0x800A0A14), CircleShape)
                .border(1.5.dp, Color(0xFF00B8D4), CircleShape)
                .zIndex(2f)
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Trở về", tint = Color.White)
        }

        // ==========================================
        // 3. UI PHỤ (RADAR & TIMELINE) - DỜI SANG TRÁI
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, top = 100.dp)
                .width(200.dp)
                .zIndex(1f)
        ) {
            // 🌟 MẠNG NHỆN ĐÃ CÓ CHỮ CHÚ THÍCH CỰC RÕ
            Column(
                modifier = Modifier
                    .background(Color(0xFF0A0A14).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text("Phân bổ 6 Trụ Cột", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                RadarChartWithLabels(metrics = currentMetrics, colors = metricColors, modifier = Modifier.fillMaxWidth().height(160.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🌟 TIMELINE (NĂM)
            Column(
                modifier = Modifier
                    .background(Color(0xFF0A0A14).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text("Năm: ${selectedYear.toInt()}", color = Color.White, fontWeight = FontWeight.Black)
                Slider(
                    value = selectedYear, onValueChange = { selectedYear = it },
                    valueRange = 2020f..2024f, steps = 3,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00B8D4), activeTrackColor = Color(0xFF00B8D4))
                )
            }
        }

        // ==========================================
        // 4. NÚT MỞ GỌI AI (GÓC PHẢI DƯỚI) & KHUNG CHAT
        // ==========================================
        FloatingActionButton(
            onClick = { showAiChat = !showAiChat },
            containerColor = Color(0xFF00E676),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp)
                .zIndex(3f)
        ) {
            Icon(Icons.Rounded.SmartToy, contentDescription = "Gọi AI", tint = Color.Black)
        }

        // Khung AI Chat hiện lên khi bấm nút
        AnimatedVisibility(
            visible = showAiChat,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .width(300.dp)
                .height(350.dp)
                .zIndex(2f)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF1E1E28).copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("Trợ lý AI Vĩ Mô", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                // Demo text AI
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Phân tích VN năm ${selectedYear.toInt()}:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Với GDP đạt ${currentMetrics[0]} tỷ USD, dòng vốn FDI (${currentMetrics[1]} tỷ USD) đang hỗ trợ mạnh cho cán cân thương mại. Các cột biểu đồ hiển thị mức tăng trưởng ổn định.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                // Chỗ chat
                OutlinedTextField(
                    value = "", onValueChange = {},
                    placeholder = { Text("Hỏi chi tiết cột...", fontSize = 11.sp) },
                    trailingIcon = { Icon(Icons.Rounded.Send, null, tint = Color(0xFF00E676)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// ====================================================
// RADAR CHART ĐÃ CÓ CHỮ CHÚ THÍCH CỤ THỂ
// ====================================================
@Composable
fun RadarChartWithLabels(metrics: List<Float>, colors: List<String>, modifier: Modifier = Modifier) {
    val animatedMetrics = metrics.map { animateFloatAsState(targetValue = it, animationSpec = tween(800)).value }
    val maxVal = animatedMetrics.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val anglePerMetric = (2 * PI) / metrics.size

            // Vẽ lưới
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

            val dataPath = Path()
            val dataPoints = mutableListOf<Offset>()

            for (i in animatedMetrics.indices) {
                val angle = i * anglePerMetric - PI / 2
                // Trục nan hoa
                val axisX = center.x + radius * cos(angle).toFloat()
                val axisY = center.y + radius * sin(angle).toFloat()
                drawLine(color = Color(0xFF333344), start = center, end = Offset(axisX, axisY), strokeWidth = 1f)

                // Vẽ chữ bằng nativeCanvas
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val textRadius = radius + 30f // Nhích chữ ra ngoài lưới
                val textX = center.x + textRadius * cos(angle).toFloat()
                val textY = center.y + textRadius * sin(angle).toFloat() + 8f
                drawContext.canvas.nativeCanvas.drawText(metricNames[i], textX, textY, paint)

                // Tọa độ Data
                val normalizedVal = animatedMetrics[i] / maxVal
                val displayRadius = radius * (normalizedVal.coerceAtLeast(0.1f))
                val dx = center.x + displayRadius * cos(angle).toFloat()
                val dy = center.y + displayRadius * sin(angle).toFloat()
                dataPoints.add(Offset(dx, dy))
                if (i == 0) dataPath.moveTo(dx, dy) else dataPath.lineTo(dx, dy)
            }
            dataPath.close()

            drawPath(dataPath, color = Color(0xFF00B8D4).copy(alpha = 0.4f))
            drawPath(dataPath, color = Color(0xFF00B8D4), style = Stroke(4f, join = StrokeJoin.Round))

            dataPoints.forEachIndexed { i, point ->
                drawCircle(color = Color(android.graphics.Color.parseColor(colors[i])), radius = 10f, center = point)
            }
        }
    }
}