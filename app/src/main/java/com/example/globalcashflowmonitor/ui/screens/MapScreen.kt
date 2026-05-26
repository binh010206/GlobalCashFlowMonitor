package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlin.math.PI
import kotlin.math.sin

// --- MAPBOX IMPORTS ---
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo

data class CountryData(val id: String, val name: String, val lat: Double, val lng: Double, val flagUrl: String)
data class FlowData(val id: String, val sourceId: String, val targetId: String, val amount: Double, val type: String)
data class SocketPayload(val countries: List<CountryData>, val flows: List<FlowData>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    isLoggedIn: Boolean = false,
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var realTimeData by remember { mutableStateOf<List<CountryData>>(emptyList()) }
    var activeFlows by remember { mutableStateOf<List<FlowData>>(emptyList()) }
    var isConnected by remember { mutableStateOf(false) }

    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var isMapStyleLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val socket: Socket = IO.socket("https://globalcashflowbackend.onrender.com")
            socket.on("connect") { isConnected = true }
            socket.on("disconnect") { isConnected = false }
            socket.on("cashflow_update") { args: Array<Any> ->
                if (args.isNotEmpty()) {
                    try {
                        val payload = Gson().fromJson(args[0].toString(), SocketPayload::class.java)
                        realTimeData = payload.countries
                        activeFlows = payload.flows
                    } catch (e: Exception) { Log.e("MapScreen", "JSON Err: ${e.message}") }
                }
            }
            socket.connect()
        } catch (e: Exception) { e.printStackTrace() }
    }

    LaunchedEffect(realTimeData, pointAnnotationManager, isMapStyleLoaded) {
        if (isMapStyleLoaded && realTimeData.isNotEmpty() && pointAnnotationManager != null && pointAnnotationManager!!.annotations.isEmpty()) {
            val options = mutableListOf<PointAnnotationOptions>()
            for (country in realTimeData) {
                val bitmap = getLocalCircularFlag(context, country.id)
                options.add(
                    PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(country.lng, country.lat))
                        .withIconImage(bitmap)
                        .withIconSize(0.2)
                )
            }
            pointAnnotationManager?.create(options)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    scalebar.updateSettings { enabled = false }
                    compass.updateSettings { enabled = false }
                    logo.updateSettings { enabled = false }
                    attribution.updateSettings { enabled = false }

                    val mapboxMap = getMapboxMap()
                    pointAnnotationManager = annotations.createPointAnnotationManager()

                    mapboxMap.loadStyleUri(Style.DARK) { style ->
                        setupFlowLayers(style)
                        isMapStyleLoaded = true
                    }

                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(108.2022, 16.0544))
                            .zoom(2.0)
                            .pitch(45.0)
                            .build()
                    )

                    mapboxMap.addOnCameraChangeListener {
                        val currentZoom = mapboxMap.cameraState.zoom
                        val dynamicSize = (currentZoom / 10.0).coerceIn(0.12, 0.5)
                        pointAnnotationManager?.let { manager ->
                            manager.annotations.forEach { it.iconSize = dynamicSize }
                            manager.update(manager.annotations)
                        }
                    }
                }
            },
            update = { mapView -> updateFlowDataOnMap(mapView, activeFlows, realTimeData) },
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )

        // ==========================================
        // KHU VỰC TOP: CHỈ HIỂN THỊ TỐI ĐA 2 THÔNG BÁO KHÔNG BỊ CẮT CHỮ
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            GlassSearchBar(isConnected, isLoggedIn, onNavigateToLogin)
            Spacer(modifier = Modifier.height(16.dp))

            if (activeFlows.isNotEmpty()) {
                Text("LIVE FLOWS (TỶ USD)", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))

                // Chiều cao khống chế chuẩn 145.dp để vừa khít 2 cái
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 145.dp)
                ) {
                    // Sử dụng .take(2) để ép hệ thống chỉ lấy tối đa 2 luồng tiền mới nhất
                    items(activeFlows.take(2), key = { it.id }) { flow -> MiniLiveFlowCard(flow) }
                }
            }
        }

        // ==========================================
        // KHU VỰC RIGHT: NÂNG CAO LÊN ĐỂ KHÔNG CHẠM THANH DƯỚI
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .offset(y = (10).dp) // Nhích nhẹ lên 30dp cực kỳ thoáng đãng
                .zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureButton(Icons.Rounded.AutoGraph, Color(0xFFFF9100), "3D Heatmap")
            FeatureButton(Icons.Rounded.SmartToy, Color(0xFF00B0FF), "Gemini AI")
            FeatureButton(Icons.Rounded.PeopleAlt, Color(0xFFE040FB), "Sync Room")
            FeatureButton(Icons.Rounded.NotificationsActive, Color(0xFFFFD600), "Alerts")
        }
    }
}

// ==========================================
// THUẬT TOÁN ĐỒ HỌA: TẠO MŨI TÊN & CHỮ TRÊN DÂY CUNG
// ==========================================
fun setupFlowLayers(style: Style) {
    val flowTypes = listOf(
        Pair("FDI", "#00B0FF"),
        Pair("TRADE", "#FF9100"),
        Pair("REMITTANCE", "#E040FB")
    )

    for (type in flowTypes) {
        style.addSource(geoJsonSource("flow-source-${type.first}") {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        })

        // Layer 1: Vẽ nét dây cung Neon chìm ở dưới
        style.addLayer(lineLayer("flow-layer-${type.first}", "flow-source-${type.first}") {
            lineColor(type.second)
            lineWidth(2.5)
            lineOpacity(0.5)
        })

        // Layer 2: Vẽ Chữ số tiền + Mũi tên hướng đích chạy uốn lượn dọc theo đường cong
        style.addLayer(symbolLayer("flow-text-${type.first}", "flow-source-${type.first}") {
            symbolPlacement(com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement.LINE)
            textField(com.mapbox.maps.extension.style.expressions.generated.Expression.get("arcLabel"))
            textColor(type.second)
            textSize(11.0)
            textHaloColor("#0A0A14")
            textHaloWidth(2.0)
            textKeepUpright(true) // Chữ không bao giờ bị chổng ngược đầu khi xoay map
            symbolSpacing(150.0) // Khoảng cách lặp lại của chữ trên dây cung
        })
    }
}

fun updateFlowDataOnMap(mapView: MapView, activeFlows: List<FlowData>, countries: List<CountryData>) {
    mapView.getMapboxMap().getStyle { style ->
        val featuresFDI = mutableListOf<Feature>()
        val featuresTrade = mutableListOf<Feature>()
        val featuresRemit = mutableListOf<Feature>()

        for (flow in activeFlows) {
            val sourceCountry = countries.find { it.id == flow.sourceId }
            val targetCountry = countries.find { it.id == flow.targetId }
            if (sourceCountry != null && targetCountry != null) {
                // Hướng vẽ luôn đi từ Source -> Target để mũi tên hướng đúng về nước đích
                val startPoint = Point.fromLngLat(sourceCountry.lng, sourceCountry.lat)
                val endPoint = Point.fromLngLat(targetCountry.lng, targetCountry.lat)

                val curvedPoints = createArc(startPoint, endPoint)
                val feature = Feature.fromGeometry(LineString.fromLngLats(curvedPoints))

                // Phân loại Icon vĩ mô tương ứng
                val macroIcon = when(flow.type) {
                    "FDI" -> "💼"       // Vốn FDI đầu tư nước ngoài
                    "REMITTANCE" -> "💸" // Kiều hối dòng vốn
                    else -> "📦"         // Thương mại xuất nhập khẩu (TRADE)
                }

                // Tạo chuỗi nhãn hiển thị kèm mũi tên hướng đích uốn lượn dọc dây cung: "▶ 💼 +5.2 Tỷ $"
                feature.addStringProperty("arcLabel", "▶  $macroIcon +${flow.amount} tỷ")

                when (flow.type) {
                    "FDI" -> featuresFDI.add(feature)
                    "REMITTANCE" -> featuresRemit.add(feature)
                    else -> featuresTrade.add(feature)
                }
            }
        }
        style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source-FDI")?.featureCollection(FeatureCollection.fromFeatures(featuresFDI))
        style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source-TRADE")?.featureCollection(FeatureCollection.fromFeatures(featuresTrade))
        style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source-REMITTANCE")?.featureCollection(FeatureCollection.fromFeatures(featuresRemit))
    }
}

// ==========================================
// CÁC HÀM XỬ LÝ ẢNH OFFLINE TRÁNH TRÀN RAM
// ==========================================
fun getLocalCircularFlag(context: Context, countryId: String): Bitmap {
    val resourceName = countryId.lowercase()
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    if (resId == 0) return getPlaceholderBitmap()

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, options)
    options.inSampleSize = calculateInSampleSize(options, 100, 100)
    options.inJustDecodeBounds = false

    val rawBitmap = BitmapFactory.decodeResource(context.resources, resId, options) ?: return getPlaceholderBitmap()
    return getCircularBitmap(rawBitmap)
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun getPlaceholderBitmap(): Bitmap {
    val size = 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.DKGRAY }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    return bitmap
}

fun getCircularBitmap(bitmap: Bitmap): Bitmap {
    val size = Math.min(bitmap.width, bitmap.height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply { isAntiAlias = true }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val rect = Rect(bitmap.width / 2 - size / 2, bitmap.height / 2 - size / 2, bitmap.width / 2 + size / 2, bitmap.height / 2 + size / 2)
    val destRect = Rect(0, 0, size, size)
    canvas.drawBitmap(bitmap, rect, destRect, paint)
    return output
}

fun createArc(start: Point, end: Point): List<Point> {
    val arcPoints = mutableListOf<Point>()
    val numPoints = 50
    for (i in 0..numPoints) {
        val fraction = i / numPoints.toDouble()
        val lng = start.longitude() + (end.longitude() - start.longitude()) * fraction
        val lat = start.latitude() + (end.latitude() - start.latitude()) * fraction
        val curveOffset = sin(fraction * PI) * 6.0
        arcPoints.add(Point.fromLngLat(lng, lat + curveOffset))
    }
    return arcPoints
}

// ==========================================
// COMPONENT UI CON ( giữ nguyên cấu trúc chuẩn )
// ==========================================
@Composable
fun FeatureButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = { Toast.makeText(context, "Đang mở: $label", Toast.LENGTH_SHORT).show() },
            containerColor = Color(0x4D000000),
            contentColor = tint,
            modifier = Modifier.size(48.dp).border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) { Icon(icon, label) }
    }
}

@Composable
fun GlassSearchBar(isConnected: Boolean, isLoggedIn: Boolean, onNavigateToLogin: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0x33000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Search, "Search", tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Radar Kinh tế...", color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), RoundedCornerShape(50)))
            Spacer(Modifier.width(6.dp))
            Text(if (isConnected) "SYNC" else "LOST", color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            if (!isLoggedIn) {
                IconButton(onClick = onNavigateToLogin, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.AccountCircle, "Login", tint = Color.White)
                }
            } else {
                Icon(Icons.Rounded.VerifiedUser, "Logged In", tint = Color(0xFF00E676), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun MiniLiveFlowCard(flow: FlowData) {
    val flowColor = when(flow.type) {
        "FDI" -> Color(0xFF00B0FF)
        "REMITNACE", "REMITTANCE" -> Color(0xFFE040FB)
        else -> Color(0xFFFF9100)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x66000000))
            .border(0.5.dp, flowColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Public, null, tint = flowColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${flow.sourceId} \u2794 ${flow.targetId}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(flow.type, color = flowColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        Text("+${flow.amount} tỷ", color = flowColor, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}