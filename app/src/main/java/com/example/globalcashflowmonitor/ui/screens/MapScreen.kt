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
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo

data class CountryData(val id: String, val name: String, val lat: Double, val lng: Double, val flagUrl: String? = null)
data class FlowData(val id: String, val sourceId: String, val targetId: String, val amount: Double, val type: String)
data class SocketPayload(val countries: List<CountryData>, val flows: List<FlowData>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    isLoggedIn: Boolean = false,
    isDarkMode: Boolean = true,
    targetCountryId: String? = null,
    onZoomCompleted: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var realTimeData by remember { mutableStateOf<List<CountryData>>(emptyList()) }
    var activeFlows by remember { mutableStateOf<List<FlowData>>(emptyList()) }
    var isConnected by remember { mutableStateOf(false) }

    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var isMapStyleLoaded by remember { mutableStateOf(false) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }

    // 🌟 BIẾN TRẠNG THÁI CHO CHATBOT (Lúc nãy mày lỡ tay xóa mất khúc này)
    var showAiBot by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("Xin chào! Tôi là Trợ lý Vĩ mô Gemini. Hãy ra lệnh cho tôi phân tích hoặc di chuyển bản đồ đến quốc gia bạn muốn.") }

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

    // Khối lệnh này sẽ kích hoạt camera bay (flyTo) khi nhận được targetCountryId từ phòng Chat
    LaunchedEffect(targetCountryId, isMapStyleLoaded, realTimeData) {
        if (targetCountryId != null && isMapStyleLoaded && realTimeData.isNotEmpty()) {
            val targetCountry = realTimeData.find { it.id == targetCountryId.uppercase() }
            if (targetCountry != null) {
                // Hiển thị thông báo
                Toast.makeText(context, "Đã định vị: ${targetCountry.name}", Toast.LENGTH_SHORT).show()

                // Gọi Mapbox bay đến tọa độ đó
                mapboxMapRef?.flyTo(
                    com.mapbox.maps.CameraOptions.Builder()
                        .center(com.mapbox.geojson.Point.fromLngLat(targetCountry.lng, targetCountry.lat))
                        .zoom(4.5)
                        .pitch(45.0)
                        .bearing(0.0)
                        .build(),
                    com.mapbox.maps.plugin.animation.MapAnimationOptions.mapAnimationOptions {
                        duration(2500) // Thời gian bay là 2.5 giây
                    }
                )

                // Tùy chọn:Bật luôn popup hiển thị thông tin hoặc mở AI Chatbot ở đây
                // showAiBot = true
                // chatInput = "Phân tích số liệu của ${targetCountry.name}"

                // Reset lại biến để không bị bay lại vào lần sau
                onZoomCompleted()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    scalebar.updateSettings { enabled = false }
                    compass.updateSettings { enabled = false }
                    logo.updateSettings { enabled = false }
                    attribution.updateSettings { enabled = false }

                    val mapboxMap = getMapboxMap()
                    mapboxMapRef = mapboxMap

                    pointAnnotationManager = annotations.createPointAnnotationManager()

                    val mapStyle = if (isDarkMode) Style.DARK else Style.LIGHT

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
                }
            },
            update = { mapView ->
                val currentStyle = if (isDarkMode) Style.DARK else Style.LIGHT
                mapView.getMapboxMap().getStyle { style ->
                    if (style.styleURI != currentStyle) {
                        mapView.getMapboxMap().loadStyleUri(currentStyle) { newStyle ->
                            setupFlowLayers(newStyle)
                        }
                    }
                }
                updateFlowDataOnMap(mapView, activeFlows, realTimeData) },
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )

        // KHU VỰC TOP
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 145.dp)
                ) {
                    items(activeFlows.take(2), key = { it.id }) { flow -> MiniLiveFlowCard(flow) }
                }
            }
        }

        // KHU VỰC RIGHT (ĐÃ GẮN SỰ KIỆN MỞ CHATBOT)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .offset(y = (10).dp)
                .zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureButton(Icons.Rounded.AutoGraph, Color(0xFFFF9100), "3D Heatmap")
            // 🌟 SỬA LẠI CHỖ NÀY: Bấm vào mở BottomSheet
            FeatureButton(Icons.Rounded.SmartToy, Color(0xFF00B0FF), "Gemini AI", onClick = { showAiBot = true })
            FeatureButton(Icons.Rounded.PeopleAlt, Color(0xFFE040FB), "Sync Room")
            FeatureButton(Icons.Rounded.NotificationsActive, Color(0xFFFFD600), "Alerts")
        }

        // =========================================
        // GIAO DIỆN CHATBOT BOTTOM SHEET (Lúc nãy mày xóa mất)
        // =========================================
        if (showAiBot) {
            ModalBottomSheet(
                onDismissRequest = { showAiBot = false },
                containerColor = Color(0xFF121224),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.zIndex(10f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SmartToy, null, tint = Color(0xFF00B0FF), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gemini Macro AI Analyst", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Hộp thoại AI trả lời
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(aiResponse, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopStart))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Ô nhập tin nhắn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("FDI vào Việt Nam thế nào?", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x33000000),
                                unfocusedContainerColor = Color(0x33000000),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        )

                        Spacer(Modifier.width(8.dp))

                        // 🌟 ĐÂY LÀ NÚT GỬI CHUẨN GỌI API & BAY CAMERA
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    val userQuery = chatInput
                                    aiResponse = "Gemini AI đang phân tích dữ liệu vĩ mô dòng tiền, vui lòng đợi..."
                                    chatInput = ""

                                    val request = com.example.globalcashflowmonitor.network.ChatRequest(userQuery)
                                    com.example.globalcashflowmonitor.network.RetrofitClient.instance.sendAiMessage(request)
                                        .enqueue(object : retrofit2.Callback<com.example.globalcashflowmonitor.network.ChatResponse> {
                                            override fun onResponse(
                                                call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>,
                                                response: retrofit2.Response<com.example.globalcashflowmonitor.network.ChatResponse>
                                            ) {
                                                if (response.isSuccessful && response.body()?.success == true) {
                                                    val aiData = response.body()?.data
                                                    if (aiData != null) {
                                                        aiResponse = aiData.reply
                                                        if (aiData.action == "ZOOM_TO" && aiData.targetId.isNotBlank()) {
                                                            val targetCountry = realTimeData.find { it.id == aiData.targetId.uppercase() }
                                                            if (targetCountry != null) {
                                                                Toast.makeText(context, "AI đang dẫn đường đến: ${targetCountry.name}", Toast.LENGTH_SHORT).show()
                                                                mapboxMapRef?.flyTo(
                                                                    com.mapbox.maps.CameraOptions.Builder()
                                                                        .center(com.mapbox.geojson.Point.fromLngLat(targetCountry.lng, targetCountry.lat))
                                                                        .zoom(4.5).pitch(45.0).bearing(0.0).build(),
                                                                    com.mapbox.maps.plugin.animation.MapAnimationOptions.mapAnimationOptions {
                                                                        duration(2500)
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    aiResponse = "Hệ thống AI phản hồi thất bại."
                                                }
                                            }

                                            override fun onFailure(call: retrofit2.Call<com.example.globalcashflowmonitor.network.ChatResponse>, t: Throwable) {
                                                aiResponse = "Lỗi kết nối: ${t.message}"
                                            }
                                        })
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF00B0FF))
                        ) {
                            Icon(Icons.Rounded.Send, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

fun setupFlowLayers(style: Style) {
    val flowTypes = listOf(
        Pair("FDI", "#00B0FF"),
        Pair("TRADE", "#FF9100"),
        Pair("REMITTANCE", "#E040FB")
    )
    for (type in flowTypes) {
        style.addSource(geoJsonSource("flow-source-${type.first}") { featureCollection(FeatureCollection.fromFeatures(emptyList())) })
        style.addLayer(lineLayer("flow-layer-${type.first}", "flow-source-${type.first}") { lineColor(type.second); lineWidth(2.5); lineOpacity(0.5) })
        style.addLayer(symbolLayer("flow-text-${type.first}", "flow-source-${type.first}") {
            symbolPlacement(com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement.LINE)
            textField(com.mapbox.maps.extension.style.expressions.generated.Expression.get("arcLabel"))
            textColor(type.second); textSize(11.0); textHaloColor("#0A0A14"); textHaloWidth(2.0); textKeepUpright(true); symbolSpacing(150.0)
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
                val startPoint = Point.fromLngLat(sourceCountry.lng, sourceCountry.lat)
                val endPoint = Point.fromLngLat(targetCountry.lng, targetCountry.lat)
                val curvedPoints = createArc(startPoint, endPoint)
                val feature = Feature.fromGeometry(LineString.fromLngLats(curvedPoints))
                val macroIcon = when(flow.type) { "FDI" -> "💼" "REMITTANCE" -> "💸" else -> "📦" }
                feature.addStringProperty("arcLabel", "▶  $macroIcon +${flow.amount} tỷ")
                when (flow.type) { "FDI" -> featuresFDI.add(feature) "REMITTANCE" -> featuresRemit.add(feature) else -> featuresTrade.add(feature) }
            }
        }
        style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source-FDI")?.featureCollection(FeatureCollection.fromFeatures(featuresFDI))
        style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source-TRADE")?.featureCollection(FeatureCollection.fromFeatures(featuresTrade))
        style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source-REMITTANCE")?.featureCollection(FeatureCollection.fromFeatures(featuresRemit))
    }
}

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
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) { inSampleSize *= 2 }
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

// 🌟 ĐÃ SỬA LẠI HÀM NÀY ĐỂ NHẬN SỰ KIỆN CLICK MỞ CHATBOT
@Composable
fun FeatureButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = {
                if (onClick != null) onClick()
                else Toast.makeText(context, "Đang mở: $label", Toast.LENGTH_SHORT).show()
            },
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x66000000))
            .border(0.5.dp, flowColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
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