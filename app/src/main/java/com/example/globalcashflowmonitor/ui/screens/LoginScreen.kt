package com.example.globalcashflowmonitor.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit
) {
    var email by remember { mutableStateOf("")}
    var password by remember { mutableStateOf("")}
    var passwordVisible by remember {mutableStateOf(false)}

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Hàm kiểm tra tính hợp lệ
    fun validateForm(): Boolean {
        var isValid = true

        // Kiểm tra Email
        if (email.isBlank()) {
            emailError = "Email không được để trống"
            isValid = false
        } else if (!email.trim().endsWith("@gmail.com")) {
            emailError = "Vui lòng sử dụng đúng định dạng đuôi @gmail.com"
            isValid = false
        } else {
            emailError = null // Hợp lệ thì xóa lỗi
        }

        // Kiểm tra Mật khẩu
        if (password.isBlank()) {
            passwordError = "Mật khẩu không được để trống"
            isValid = false
        } else if (password.length < 8) {
            passwordError = "Mật khẩu phải có ít nhất 8 ký tự"
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GLOBAL CASH FLOW",
            color = Color.Cyan,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "Hệ thống giám sát kinh tế vĩ mô",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 30.dp)
        )
        Text(
            text = "Đăng nhập",
            color = Color.Cyan,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 0.dp, bottom = 15.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it
                if (emailError != null) emailError = null},
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email", tint = Color.Cyan) },
            isError = emailError != null,
            supportingText = {
                if (emailError != null){
                    Text(text = emailError!!, color = Color(0xFFFF1744))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {password = it
                if (passwordError != null) passwordError = null},
            label = {Text("Mật Khẩu")},
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Password", tint = Color.Cyan) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                IconButton(onClick = {passwordVisible = !passwordVisible}) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(text = passwordError!!, color = Color(0xFFFF1744))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Quên mật khẩu?",
                color = Color.Cyan,
                fontSize = 14.sp,
                modifier = Modifier.clickable {onNavigateToForgot()}
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (validateForm()) {
                    onLoginSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("ĐĂNG NHẬP", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chưa có tài khoản?", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = " Đăng ký ngay",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable{ onNavigateToRegister()}
            )
        }
    }
}