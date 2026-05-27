package com.example.globalcashflowmonitor.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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

// --- Tái sử dụng Mưa (Bỏ khúc này nếu 2 file nằm chung package mà báo lỗi Duplicate) ---
@Composable
fun RainBackgroundRegister() {
    val rainDrops = remember { List(40) { RainDrop(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 0.02f + 0.01f, Random.nextFloat() * 50f + 20f, Random.nextFloat() * 0.5f + 0.1f) } }
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)))
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {
        rainDrops.forEach { drop ->
            drop.y += drop.speed + (time * 0.0001f)
            if (drop.y > 1f) { drop.y = -0.1f; drop.x = Random.nextFloat() }
            drawLine(color = Color(0xFF00B8D4).copy(alpha = drop.alpha), start = Offset(drop.x * size.width, drop.y * size.height), end = Offset(drop.x * size.width, drop.y * size.height + drop.length), strokeWidth = 2f)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    fun validateRegister(): Boolean {
        var isValid = true
        if (fullName.isBlank()) { nameError = "Không được để trống"; isValid = false } else nameError = null
        if (!email.trim().endsWith("@gmail.com")) { emailError = "Phải là @gmail.com"; isValid = false } else emailError = null
        if (password.length < 8) { passwordError = "Tối thiểu 8 ký tự"; isValid = false } else passwordError = null
        if (confirmPassword != password) { confirmPasswordError = "Không khớp"; isValid = false } else confirmPasswordError = null
        return isValid
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RainBackgroundRegister()

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("TẠO TÀI KHOẢN", color = Color(0xFF00E676), fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Tham gia hệ thống phân tích vĩ mô", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 32.dp))

            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF161622).copy(alpha = 0.85f), RoundedCornerShape(24.dp)).border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = fullName, onValueChange = { fullName = it; nameError = null },
                    label = { Text("Họ và Tên") }, leadingIcon = { Icon(Icons.Rounded.Person, null, tint = Color(0xFF00B8D4)) },
                    isError = nameError != null, supportingText = { nameError?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E28), unfocusedContainerColor = Color(0xFF1E1E28))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it; emailError = null },
                    label = { Text("Email @gmail.com") }, leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color(0xFF00B8D4)) },
                    isError = emailError != null, supportingText = { emailError?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E28), unfocusedContainerColor = Color(0xFF1E1E28))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it; passwordError = null },
                    label = { Text("Mật khẩu (8+ ký tự)") }, leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Color(0xFF00B8D4)) },
                    trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = Color.Gray) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null, supportingText = { passwordError?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E28), unfocusedContainerColor = Color(0xFF1E1E28))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it; confirmPasswordError = null },
                    label = { Text("Xác nhận mật khẩu") }, leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00E676)) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPasswordError != null, supportingText = { confirmPasswordError?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E28), unfocusedContainerColor = Color(0xFF1E1E28))
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (validateRegister() && !isLoading) {
                            isLoading = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val url = URL("https://globalcashflowbackend.onrender.com/api/auth/register")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")

                                    conn.connectTimeout = 15000
                                    conn.readTimeout = 15000

                                    conn.doOutput = true

                                    val jsonParam = JSONObject().apply {
                                        put("fullName", fullName.trim())
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
                                            Toast.makeText(context, "Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_LONG).show()
                                            onRegisterSuccess() // Chuyển về màn Login
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
                    contentPadding = PaddingValues()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF00B8D4), Color(0xFF00E676))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("ĐĂNG KÝ NGAY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text("Đã có tài khoản? ", color = Color.Gray)
                Text("Đăng nhập", color = Color(0xFF00B8D4), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToLogin() })
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}