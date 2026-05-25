package com.example.globalcashflowmonitor.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket

// --- TOÁN HỌC & MAPBOX V11 ---
import kotlin.math.PI
import kotlin.math.sin
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

// --- DATA CLASSES ---
data class CountryData(val id: String, val name: String, val lat: Double, val lng: Double, val gdp: Double)
data class FlowData(val id: String, val sourceId: String, val targetId: String, val amount: Double, val type: String)
data class SocketPayload(val countries: List<CountryData>, val flows: List<FlowData>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    isLoggedIn: Boolean = false,
    onNavigateToLogin: () -> Unit = {}
) {
    var realTimeData by remember { mutableStateOf<List<CountryData>>(emptyList()) }
    var activeFlows by remember { mutableStateOf<List<FlowData>>(emptyList()) }
    var isConnected by remember { mutableStateOf(false) }

    // KẾT NỐI SERVER RENDER TRÊN MÂY
    LaunchedEffect(Unit) {
        try {
            val socket: Socket = IO.socket("https://globalcashflowbackend.onrender.com")

            socket.on("connect") { isConnected = true }
            socket.on("disconnect") { isConnected = false }

            socket.on("cashflow_update") { args: Array<Any> ->
                if (args.isNotEmpty()) {
                    val jsonString = args[0].toString()
                    try {
                        val payload = Gson().fromJson(jsonString, SocketPayload::class.java)
                        realTimeData = payload.countries
                        activeFlows = payload.flows
                    } catch (e: Exception) {
                        Log.e("MapScreen", "Lỗi Parse JSON: ${e.message}")
                    }
                }
            }
            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {

        // --- LỚP 1: BẢN ĐỒ MAPBOX 3D CHUYÊN DỤNG ---
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    getMapboxMap().loadStyleUri(Style.DARK) { style ->
                        setupFlowLayers(style)
                    }

                    // NGAY LẬP TỨC ZOOM VỀ KHU VỰC MIỀN TRUNG VIỆT NAM (GÓC 3D)
                    getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(108.2022, 16.0544)) // Tọa độ Đà Nẵng
                            .zoom(4.5)
                            .pitch(45.0) // Nghiêng 45 độ tạo cảm giác chiều sâu không gian
                            .build()
                    )
                }
            },
            update = { mapView ->
                updateFlowDataOnMap(mapView, activeFlows, realTimeData)
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- LỚP 2: UI KÍNH MỜ (TÌM KIẾM & THẺ LUỒNG TIỀN) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 45.dp, start = 16.dp, end = 16.dp, bottom = 100.dp)
        ) {
            GlassSearchBar(isConnected, isLoggedIn, onNavigateToLogin)
            Spacer(modifier = Modifier.height(16.dp))

            if (activeFlows.isNotEmpty()) {
                Text(
                    text = "LIVE FLOWS DETECTED (TỶ USD)",
                    color = Color(0xFF00E676),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeFlows, key = { it.id }) { flow -> LiveFlowCard(flow) }
                }
            }
        }

        // --- LỚP 3: THANH CÔNG CỤ BÊN PHẢI ---
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureButton(Icons.Rounded.AutoGraph, Color(0xFFFF9100), "3D Heatmap")
            FeatureButton(Icons.Rounded.SmartToy, Color(0xFF00B0FF), "Gemini AI Assistant")
            FeatureButton(Icons.Rounded.PeopleAlt, Color(0xFFE040FB), "Sync Room")
            FeatureButton(Icons.Rounded.NotificationsActive, Color(0xFFFFD600), "Smart Alerts")
        }
    }
}

// ==========================================
// THUẬT TOÁN ĐỒ HỌA: TẠO ĐƯỜNG CONG 3D (ARCS)
// ==========================================

fun createArc(start: Point, end: Point): List<Point> {
    val arcPoints = mutableListOf<Point>()
    val numPoints = 50 // Chia nhỏ thành 50 điểm để đường cong cực mượt

    for (i in 0..numPoints) {
        val fraction = i / numPoints.toDouble()
        val lng = start.longitude() + (end.longitude() - start.longitude()) * fraction
        val lat = start.latitude() + (end.latitude() - start.latitude()) * fraction

        // Thuật toán Sin tạo độ vút lên không trung (curveOffset)
        val curveOffset = sin(fraction * PI) * 6.0
        arcPoints.add(Point.fromLngLat(lng, lat + curveOffset))
    }
    return arcPoints
}

fun setupFlowLayers(style: Style) {
    style.addSource(geoJsonSource("flow-source") {
        featureCollection(FeatureCollection.fromFeatures(emptyList()))
    })
    style.addLayer(lineLayer("flow-layer", "flow-source") {
        lineColor("#00E676") // Mặc định xanh phát sáng
        lineWidth(3.5)
        lineOpacity(0.8)
    })
}

fun updateFlowDataOnMap(mapView: MapView, activeFlows: List<FlowData>, countries: List<CountryData>) {
    mapView.getMapboxMap().getStyle { style ->
        val features = mutableListOf<Feature>()
        for (flow in activeFlows) {
            val sourceCountry = countries.find { it.id == flow.sourceId }
            val targetCountry = countries.find { it.id == flow.targetId }

            if (sourceCountry != null && targetCountry != null) {
                val startPoint = Point.fromLngLat(sourceCountry.lng, sourceCountry.lat)
                val endPoint = Point.fromLngLat(targetCountry.lng, targetCountry.lat)

                // GỌI HÀM VẼ ĐƯỜNG CONG THAY VÌ ĐƯỜNG THẲNG
                val curvedPoints = createArc(startPoint, endPoint)
                val lineString = LineString.fromLngLats(curvedPoints)
                features.add(Feature.fromGeometry(lineString))
            }
        }
        val source = style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("flow-source")
        source?.featureCollection(FeatureCollection.fromFeatures(features))
    }
}

// ==========================================
// CÁC COMPONENT GIAO DIỆN (ĐÃ FIX NÚT BẤM)
// ==========================================

@Composable
fun FeatureButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = { Toast.makeText(context, "Đang tải $label...", Toast.LENGTH_SHORT).show() },
            containerColor = Color(0x4D000000),
            contentColor = tint,
            modifier = Modifier.size(48.dp).border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    }
}

@Composable
fun GlassSearchBar(isConnected: Boolean, isLoggedIn: Boolean, onNavigateToLogin: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0x33000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.Gray)
            Spacer(Modifier.width(12.dp))
            Text("Quét không gian kinh tế...", color = Color.Gray, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), RoundedCornerShape(50)))
            Spacer(Modifier.width(6.dp))
            Text(if (isConnected) "SYNC" else "LOST", color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744), fontSize = 12.sp, fontWeight = FontWeight.Black)
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
fun LiveFlowCard(flow: FlowData) {
    val flowColor = if (flow.type == "FDI") Color(0xFF00B0FF) else Color(0xFFFF9100)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x4D000000))
            .border(0.5.dp, flowColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Public, null, tint = flowColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${flow.sourceId} ➔ ${flow.targetId}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Mạch: ${flow.type}", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Text("+${flow.amount} Tỷ $", color = flowColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}