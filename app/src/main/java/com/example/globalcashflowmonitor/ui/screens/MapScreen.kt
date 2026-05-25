    package com.example.globalcashflowmonitor.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// --- CÁC THƯ VIỆN BỔ SUNG ĐỂ SỬA LỖI ĐỎ LÒM ---
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
// ----------------------------------------------
import com.example.globalcashflowmonitor.data.CountryData
import com.example.globalcashflowmonitor.data.MockData
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val dataTabs = listOf("GDP", "FDI Inflow", "Trade Balance", "Remittances", "FPI", "Reserves", "External Debt", "Tourism")
    val selectedTabs = remember { mutableStateListOf<String>() }
    var selectedCountry by remember { mutableStateOf<CountryData?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var currentZoom by remember { mutableStateOf(3.0) }
    var isLegendExpanded by remember { mutableStateOf(false) }

    // --- KẾT NỐI REAL-TIME VỚI SERVER NODE.JS BẰNG SOCKET.IO ---
    var realTimeData by remember { mutableStateOf(MockData.topCountries) }

    LaunchedEffect(Unit) {
        try {
            // 🚩 LƯU Ý SỐNG CÒN: Sửa "192.168.1.X" thành IPv4 mạng Wi-Fi của máy tính mày!
            val socket: Socket = IO.socket("172.26.33.128")
            socket.connect()

            // FIX LỖI ÉP KIỂU: Khai báo rõ args là Array<Any>
            socket.on("cashflow_update") { args: Array<Any> ->
                if (args.isNotEmpty()) {
                    val jsonString = args[0].toString()
                    val listType = object : TypeToken<List<CountryData>>() {}.type
                    val newData: List<CountryData> = Gson().fromJson(jsonString, listType)

                    realTimeData = newData
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Bảng màu tháp 3D tương phản cao
    val colorMapTowers = mapOf(
        "GDP" to "#00FFFF", "FDI Inflow" to "#FFBF00", "Trade Balance" to "#FF007F",
        "Remittances" to "#00FF00", "FPI" to "#9D00FF", "Reserves" to "#FFFFFF",
        "External Debt" to "#FF0040", "Tourism" to "#00E676"
    )

    fun getValueForTab(country: CountryData, tabName: String): Double {
        return when (tabName) {
            "GDP" -> country.gdp
            "FDI Inflow" -> country.fdi
            "Trade Balance" -> country.tradeBalance
            "Remittances" -> country.remittances
            "FPI" -> country.fpi
            "Reserves" -> country.reserves
            "External Debt" -> country.debt
            "Tourism" -> country.tourism
            else -> 0.0
        }
    }

    // Thuật toán màu nhiệt chuẩn G8
    fun getG8SequentialColor(value: Double, g8Threshold: Double): String {
        val absVal = Math.abs(value)
        if (absVal >= g8Threshold) return "#D32F2F"
        val ratio = (absVal / g8Threshold).coerceIn(0.0, 1.0)
        return when {
            ratio < 0.20 -> "#FFF59D"
            ratio < 0.50 -> "#FBC02D"
            else -> "#F57C00"
        }
    }

    fun formatCurrency(value: Double): String {
        return if (value.isNaN()) "???" else "$value Tỷ USD"
    }

    Box(modifier = Modifier.fillMaxSize()) {

        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapInitOptionsFactory = { ctx ->
                MapInitOptions(
                    context = ctx, styleUri = Style.DARK,
                    cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(104.1954, 35.8617))
                        .zoom(3.0).pitch(50.0).build()
                )
            }
        ) {
            MapEffect(selectedTabs.toList(), currentZoom, selectedCountry, realTimeData) { mapView ->
                val map = mapView.mapboxMap
                mapView.mapboxMap.addOnCameraChangeListener { currentZoom = mapView.mapboxMap.cameraState.zoom }

                map.getStyle { style ->
                    // 1. Dọn dẹp layer cũ
                    dataTabs.forEach { tab ->
                        if (style.styleLayerExists("layer-$tab")) style.removeStyleLayer("layer-$tab")
                        if (style.styleSourceExists("source-$tab")) style.removeStyleSource("source-$tab")
                    }
                    if (style.styleLayerExists("country-background-layer")) style.removeStyleLayer("country-background-layer")
                    if (style.styleLayerExists("country-border-layer")) style.removeStyleLayer("country-border-layer")

                    val primaryTab = if (selectedTabs.isNotEmpty()) selectedTabs[0] else "GDP"
                    val allValuesDesc = realTimeData.map { Math.abs(getValueForTab(it, primaryTab)) }.sortedDescending()
                    val g8Threshold = if (allValuesDesc.size >= 8) allValuesDesc[7] else (allValuesDesc.firstOrNull() ?: 1.0)

                    val baseZoomOff = 3.0
                    val responsiveSize = 0.25 * (2.0.pow(baseZoomOff - currentZoom))
                    val responsiveGap = 0.50 * (2.0.pow(baseZoomOff - currentZoom))
                    val selectedCountryId = selectedCountry?.id ?: ""

                    // 2. Vẽ tháp 3D
                    selectedTabs.forEachIndexed { index, tabName ->
                        val row = index / 3
                        val col = index % 3
                        val offLng = (col - 1) * responsiveGap
                        val offLat = (1 - row) * responsiveGap

                        val features = realTimeData.map { country ->
                            val polygon = createResponsivePillarBase(country.lat, country.lng, responsiveSize, offLng, offLat)
                            val rawValue = getValueForTab(country, tabName)
                            val heightValue = Math.sqrt(Math.abs(rawValue)) * 50000.0

                            val feature = Feature.fromGeometry(polygon)
                            feature.addNumberProperty("height", heightValue)
                            feature.addStringProperty("country_id_3d", country.id)
                            feature.addStringProperty("country_name_3d", country.name)
                            feature
                        }

                        val sourceId = "source-$tabName"
                        val layerId = "layer-$tabName"
                        style.addSource(geoJsonSource(sourceId) { data(FeatureCollection.fromFeatures(features).toJson()) })

                        style.addLayer(fillExtrusionLayer(layerId, sourceId) {
                            fillExtrusionHeight(Expression.get("height"))
                            fillExtrusionColor(colorMapTowers[tabName] ?: "#808080")
                            fillExtrusionOpacity(0.9)
                        })
                    }

                    // 3. Tải GeoJSON và vẽ nền, viền
                    if (!style.styleSourceExists("country-background-source")) {
                        style.addSource(geoJsonSource("country-background-source") {
                            data("https://d2ad6b4ur7yvpq.cloudfront.net/naturalearth-3.3.0/ne_50m_admin_0_countries.geojson")
                        })
                    }

                    // Tính màu nền (HEATMAP)
                    val colorMatchArgs = mutableListOf<Expression>()
                    colorMatchArgs.add(Expression.get("iso_a2"))
                    realTimeData.forEach { country ->
                        val valuePrimary = getValueForTab(country, primaryTab)
                        val colorIntensity = getG8SequentialColor(valuePrimary, g8Threshold)
                        colorMatchArgs.add(Expression.literal(country.id))
                        colorMatchArgs.add(Expression.literal(colorIntensity))
                    }
                    colorMatchArgs.add(Expression.literal("#1A1A1A"))

                    // Hiệu ứng Highlight nước đang chọn (100% sáng vs 55% mờ)
                    val opacityMatchArgs = mutableListOf<Expression>()
                    opacityMatchArgs.add(Expression.get("iso_a2"))
                    opacityMatchArgs.add(Expression.literal(selectedCountryId))
                    opacityMatchArgs.add(Expression.literal(1.0))
                    opacityMatchArgs.add(Expression.literal(0.55))

                    val countryBackgroundLayer = fillLayer("country-background-layer", "country-background-source") {
                        fillColor(Expression.match(*colorMatchArgs.toTypedArray()))
                        fillOpacity(Expression.match(*opacityMatchArgs.toTypedArray()))
                    }

                    // VẼ VIỀN QUỐC GIA TRẮNG DỊU MẮT
                    val countryBorderLayer = lineLayer("country-border-layer", "country-background-source") {
                        lineColor("#FFFFFF")
                        lineWidth(1.6)
                        lineOpacity(0.35)
                    }

                    // Xếp layer
                    val firstTowerLayerId = if (selectedTabs.isNotEmpty()) "layer-${selectedTabs[0]}" else null
                    if (firstTowerLayerId != null && style.styleLayerExists(firstTowerLayerId)) {
                        style.addLayerBelow(countryBorderLayer, firstTowerLayerId)
                        style.addLayerBelow(countryBackgroundLayer, "country-border-layer")
                    } else {
                        style.addLayer(countryBorderLayer)
                        style.addLayerBelow(countryBackgroundLayer, "country-border-layer")
                    }
                }

                // SỰ KIỆN CLICK
                mapView.mapboxMap.addOnMapClickListener { point ->
                    val pixel = mapView.mapboxMap.pixelForCoordinate(point)
                    val queryLayers = selectedTabs.map { "layer-$it" }.toMutableList()
                    queryLayers.add("country-background-layer")

                    mapView.mapboxMap.queryRenderedFeatures(
                        com.mapbox.maps.RenderedQueryGeometry(pixel),
                        com.mapbox.maps.RenderedQueryOptions(queryLayers, null)
                    ) { expected ->
                        if (expected.isValue) {
                            val feature = expected.value?.firstOrNull()?.queriedFeature?.feature
                            if (feature != null) {
                                val iso = feature.getStringProperty("iso_a2") ?: feature.getStringProperty("country_id_3d")
                                val name = feature.getStringProperty("NAME") ?: feature.getStringProperty("NAME_EN") ?: feature.getStringProperty("country_name_3d") ?: "Quốc gia vãng lai"

                                val found = realTimeData.find { it.id == iso }
                                if (found != null) {
                                    selectedCountry = found
                                } else {
                                    selectedCountry = CountryData(
                                        id = iso ?: "", name = name, lat = point.latitude(), lng = point.longitude(),
                                        fdi = Double.NaN, tradeBalance = Double.NaN, gdp = Double.NaN, remittances = Double.NaN,
                                        fpi = Double.NaN, reserves = Double.NaN, debt = Double.NaN, tourism = Double.NaN
                                    )
                                }
                            } else {
                                selectedCountry = null
                            }
                        }
                    }
                    true
                }
            }
        }

        // --- GIAO DIỆN CHÚ THÍCH (LEGEND BOX) ---
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 62.dp)) {
            if (isLegendExpanded) {
                Column(
                    modifier = Modifier.width(220.dp).background(Color(0xFF1E1E1E).copy(alpha = 0.95f), RoundedCornerShape(12.dp)).padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("CHÚ THÍCH HỆ THỐNG", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { isLegendExpanded = false }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = Color.Gray) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("MẬT ĐỘ NỀN ĐẤT (TOP 8):", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LegendItem("#D32F2F", "Nằm trong TOP 8 G8")
                    LegendItem("#F57C00", "Quy mô cao (Cận Top)")
                    LegendItem("#FBC02D", "Quy mô trung bình")
                    LegendItem("#FFF59D", "Quy mô nhỏ")
                    LegendItem("#1A1A1A", "Chưa có dữ liệu real-time")

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("CỘT DÒNG TIỀN ĐANG BẬT:", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (selectedTabs.isNotEmpty()) {
                        selectedTabs.forEach { tabName ->
                            LegendItem(colorMapTowers[tabName] ?: "#808080", "Tháp $tabName")
                        }
                    } else {
                        Text("Chưa bật cột nào trên Menu", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { isLegendExpanded = true }, containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp), shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = "Chú thích", tint = Color.Cyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        // --- HEADER TÌM KIẾM & CHIPS ---
        Column(modifier = Modifier.fillMaxWidth().padding(top = 45.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(30.dp), shadowElevation = 10.dp, color = Color(0xFF2C2C2C)
            ) {
                OutlinedTextField(
                    value = "", onValueChange = {}, placeholder = { Text("Tìm kiếm quốc gia, khu vực...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = Color.Cyan) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dataTabs) { tabName ->
                    FilterChip(
                        selected = selectedTabs.contains(tabName),
                        onClick = { if (selectedTabs.contains(tabName)) selectedTabs.remove(tabName) else selectedTabs.add(tabName) },
                        label = { Text(tabName) },
                        leadingIcon = { Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(colorMapTowers[tabName])), CircleShape)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.DarkGray, selectedLabelColor = Color.White, labelColor = Color.Gray)
                    )
                }
            }
        }

        // --- BẢNG BOTTOM SHEET ĐỘNG ---
        if (selectedCountry != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedCountry = null }, sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E), dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(text = "HỆ THỐNG DÒNG TIỀN QUỐC TẾ", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = selectedCountry!!.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.DarkGray)

                    LazyColumn(modifier = Modifier.padding(top = 16.dp, bottom = 40.dp)) {
                        if (selectedTabs.isEmpty()) {
                            item { Text("Hãy chọn ít nhất 1 dòng tiền trên thanh Menu (GDP, FDI...) để xem số liệu chi tiết.", color = Color.Gray, fontSize = 14.sp) }
                        } else {
                            selectedTabs.forEach { tabName ->
                                item {
                                    val currentCountryData = realTimeData.find { it.id == selectedCountry!!.id }
                                    val valueForThisTab = if (currentCountryData != null) getValueForTab(currentCountryData, tabName) else Double.NaN

                                    val rowColorHex = colorMapTowers[tabName] ?: "#FFFFFF"
                                    InfoRow(
                                        title = tabName,
                                        value = formatCurrency(valueForThisTab),
                                        valueColor = Color(android.graphics.Color.parseColor(rowColorHex))
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

@Composable
fun LegendItem(colorHex: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(11.dp).background(Color(android.graphics.Color.parseColor(colorHex)), RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.LightGray, fontSize = 11.sp)
    }
}

fun createResponsivePillarBase(lat: Double, lng: Double, baseSize: Double, offsetLng: Double = 0.0, offsetLat: Double = 0.0): Polygon {
    val shiftedLng = lng + offsetLng
    val shiftedLat = lat + offsetLat
    val points = listOf(
        Point.fromLngLat(shiftedLng - baseSize, shiftedLat - baseSize), Point.fromLngLat(shiftedLng + baseSize, shiftedLat - baseSize),
        Point.fromLngLat(shiftedLng + baseSize, shiftedLat + baseSize), Point.fromLngLat(shiftedLng - baseSize, shiftedLat + baseSize),
        Point.fromLngLat(shiftedLng - baseSize, shiftedLat - baseSize)
    )
    return Polygon.fromLngLats(listOf(points))
}

@Composable
fun InfoRow(title: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.LightGray, fontSize = 15.sp)
        Text(text = value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}