package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

// --- HIỆU ỨNG MƯA RƠI (DIGITAL RAIN) ---
data class RainDrop(var x: Float, var y: Float, var speed: Float, val length: Float, val alpha: Float)

@Composable
fun RainBackground() {
    val rainDrops = remember {
        List(40) {
            RainDrop(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 0.02f + 0.01f, Random.nextFloat() * 50f + 20f, Random.nextFloat() * 0.5f + 0.1f)
        }
    }
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)))

    Canvas(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        rainDrops.forEach { drop ->
            drop.y += drop.speed + (time * 0.0001f)
            if (drop.y > 1f) { drop.y = -0.1f; drop.x = Random.nextFloat() }
            val startY = drop.y * size.height
            drawLine(
                color = Color(0xFF00B8D4).copy(alpha = drop.alpha),
                start = Offset(drop.x * size.width, startY),
                end = Offset(drop.x * size.width, startY + drop.length),
                strokeWidth = 2f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit, onNavigateToForgot: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) } // Trạng thái quay quay khi gọi API

    fun validateForm(): Boolean {
        var isValid = true
        if (email.isBlank() || !email.trim().endsWith("@gmail.com")) { emailError = "Email @gmail.com không hợp lệ"; isValid = false } else emailError = null
        if (password.isBlank() || password.length < 8) { passwordError = "Mật khẩu tối thiểu 8 ký tự"; isValid = false } else passwordError = null
        return isValid
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RainBackground() // Gọi hiệu ứng mưa

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("GLOBAL CASH FLOW", color = Color(0xFF00E676), fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("Hệ thống giám sát kinh tế vĩ mô", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 40.dp))

            // Khung Kính (Glassmorphism)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161622).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ĐĂNG NHẬP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it; emailError = null },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color(0xFF00B8D4)) },
                    isError = emailError != null, supportingText = { emailError?.let { Text(it, color = Color(0xFFFF1744)) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E28), unfocusedContainerColor = Color(0xFF1E1E28))
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it; passwordError = null },
                    label = { Text("Mật Khẩu") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Color(0xFF00B8D4)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null, supportingText = { passwordError?.let { Text(it, color = Color(0xFFFF1744)) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E28), unfocusedContainerColor = Color(0xFF1E1E28))
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    Text("Quên mật khẩu?", color = Color(0xFF00B8D4), fontSize = 14.sp, modifier = Modifier.clickable { onNavigateToForgot() })
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (validateForm() && !isLoading) {
                            isLoading = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    // Bật link Render, Tắt link Localhost
                                    val url = URL("https://globalcashflowbackend.onrender.com/api/auth/login")
                                    // val url = URL("http://10.0.2.2:5000/api/auth/login")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")

                                    conn.connectTimeout = 15000
                                    conn.readTimeout = 15000

                                    conn.doOutput = true

                                    val jsonParam = JSONObject().apply {
                                        put("email", email.trim())
                                        put("password", password)
                                    }

                                    OutputStreamWriter(conn.outputStream).use { it.write(jsonParam.toString()); it.flush() }

                                    val responseCode = conn.responseCode
                                    val responseStr = if (responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream.bufferedReader().use { it.readText() }
                                    val jsonResponse = JSONObject(responseStr)

                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        if (jsonResponse.getBoolean("success")) {
                                            val data = jsonResponse.getJSONObject("data")
                                            // Lưu thông tin vào bộ nhớ
                                            sharedPref.edit()
                                                .putBoolean("IS_LOGGED_IN", true)
                                                .putString("USER_EMAIL", data.getString("email"))
                                                .putString("USER_NAME", data.getString("name"))
                                                .apply()

                                            Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        } else {
                                            Toast.makeText(context, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace() // In lỗi ra Logcat Android Studio
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        // 🌟 HIỆN LỖI THẬT ĐỂ BIẾT TẠI SAO CHẾT
                                        Toast.makeText(context, "Chi tiết lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues() // Bỏ padding mặc định để bo Gradient
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF00B8D4), Color(0xFF00E676))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("ĐĂNG NHẬP", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản?", color = Color.Gray, fontSize = 14.sp)
                Text(" Đăng ký ngay", color = Color(0xFF00B8D4), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToRegister() })
            }
        }
    }
}