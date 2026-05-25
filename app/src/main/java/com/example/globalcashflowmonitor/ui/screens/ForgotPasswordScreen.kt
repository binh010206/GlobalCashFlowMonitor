package com.example.globalcashflowmonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    // State để chuyển đổi UI khi người dùng bấm gửi thành công
    var isSent by remember { mutableStateOf(false) }

    fun validateEmail(): Boolean {
        return if (email.isBlank()) {
            emailError = "Vui lòng nhập email của bạn"
            false
        } else if (!email.trim().endsWith("@gmail.com")) {
            emailError = "Email không hợp lệ (phải có đuôi @gmail.com)"
            false
        } else {
            emailError = null
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
            .padding(24.dp)
            .padding(top = 24.dp) // Né thanh trạng thái
    ) {
        // Nút Back
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại", tint = Color.Cyan)
        }

        if (!isSent) {
            // --- GIAO DIỆN NHẬP EMAIL ---
            Text("KHÔI PHỤC", color = Color.Cyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("MẬT KHẨU", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Đừng lo lắng! Vui lòng nhập địa chỉ email bạn đã dùng để đăng ký. Hệ thống sẽ gửi một liên kết để tạo lại mật khẩu mới.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Ô nhập Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("Email @gmail.com") },
                leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color.Cyan) },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it, color = Color.Red) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nút Gửi
            Button(
                onClick = {
                    if (validateEmail()) {
                        // TODO: Gọi API Node.js gửi email ở đây
                        isSent = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("GỬI LIÊN KẾT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            // --- GIAO DIỆN THÔNG BÁO THÀNH CÔNG ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.MarkEmailRead,
                    contentDescription = "Thành công",
                    tint = Color.Cyan,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ĐÃ GỬI EMAIL!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Chúng tôi đã gửi hướng dẫn khôi phục mật khẩu đến hòm thư:\n${email}\n\nVui lòng kiểm tra cả hộp thư rác (Spam).",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Cyan
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Cyan)
                ) {
                    Text("QUAY LẠI ĐĂNG NHẬP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}