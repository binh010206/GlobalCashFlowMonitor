package com.example.globalcashflowmonitor.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Error States
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    fun validateRegister(): Boolean {
        var isValid = true

        if (fullName.isBlank()) { nameError = "Họ tên không được để trống"; isValid = false } else nameError = null

        if (!email.trim().endsWith("@gmail.com")) {
            emailError = "Email phải có đuôi @gmail.com"; isValid = false
        } else emailError = null

        if (password.length < 8) {
            passwordError = "Mật khẩu phải từ 8 ký tự"; isValid = false
        } else passwordError = null

        if (confirmPassword != password) {
            confirmPasswordError = "Mật khẩu xác nhận không khớp"; isValid = false
        } else confirmPasswordError = null

        return isValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
            .verticalScroll(rememberScrollState()) // Thêm cuộn để không bị tràn màn hình khi hiện bàn phím
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("TẠO TÀI KHOẢN", color = Color.Cyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Tham gia hệ thống phân tích vĩ mô", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 32.dp))

        // Họ và Tên
        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it; nameError = null },
            label = { Text("Họ và Tên") },
            leadingIcon = { Icon(Icons.Rounded.Person, null, tint = Color.Cyan) },
            isError = nameError != null, supportingText = { nameError?.let { Text(it, color = Color.Red) } },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
        OutlinedTextField(
            value = email, onValueChange = { email = it; emailError = null },
            label = { Text("Email @gmail.com") },
            leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color.Cyan) },
            isError = emailError != null, supportingText = { emailError?.let { Text(it, color = Color.Red) } },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mật khẩu
        OutlinedTextField(
            value = password, onValueChange = { password = it; passwordError = null },
            label = { Text("Mật khẩu (8+ ký tự)") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Color.Cyan) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = Color.Gray)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null, supportingText = { passwordError?.let { Text(it, color = Color.Red) } },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Xác nhận mật khẩu
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it; confirmPasswordError = null },
            label = { Text("Xác nhận mật khẩu") },
            leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, tint = Color.Cyan) },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPasswordError != null, supportingText = { confirmPasswordError?.let { Text(it, color = Color.Red) } },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E))
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { if (validateRegister()) onRegisterSuccess() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("ĐĂNG KÝ NGAY", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Đã có tài khoản? ", color = Color.Gray)
            Text("Đăng nhập", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToLogin() })
        }
    }
}