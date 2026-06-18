package com.example.globalcashflowmonitor.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToForgot: () -> Unit // Thêm hàm này để đá qua màn Quên Mật Khẩu
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("MacroAppPrefs", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()

    var userName by remember { mutableStateOf(sharedPref.getString("USER_NAME", "Kỹ sư Ẩn danh") ?: "") }
    var userEmail by remember { mutableStateOf(sharedPref.getString("USER_EMAIL", "Chưa cập nhật") ?: "") }

    // Đọc ảnh từ ổ cứng nội bộ (đã lưu cứng)
    var avatarUri by remember { mutableStateOf<Uri?>(try { sharedPref.getString("USER_AVATAR", null)?.let { Uri.parse(it) } } catch (e: Exception) { null }) }

    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }

    // State cho Dialog Đổi Mật Khẩu
    var showChangePassDialog by remember { mutableStateOf(false) }

    // Trình chọn ảnh và COPY VÀO BỘ NHỚ TRONG
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // COPY ảnh vào ổ cứng app để không bao giờ bị mất quyền truy cập
                val savedUri = saveImageToInternalStorage(context, uri)
                if (savedUri != null) {
                    avatarUri = savedUri
                    sharedPref.edit().putString("USER_AVATAR", savedUri.toString()).apply()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("CÀI ĐẶT & HỒ SƠ", color = Color(0xFF00E676), fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))

        // --- KHU VỰC AVATAR ---
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E2E))
                    .border(2.dp, Color(0xFF00B8D4), CircleShape)
                    .clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    val bitmap = loadBitmapSafe(context, avatarUri!!)
                    if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Rounded.Person, null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                } else Icon(Icons.Rounded.Person, null, tint = Color.Gray, modifier = Modifier.size(50.dp))
            }
            Box(modifier = Modifier.size(32.dp).offset(x = (-4).dp, y = (-4).dp).clip(CircleShape).background(Color(0xFF00E676)).border(2.dp, Color(0xFF0D0D12), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Nhấn vào ảnh để thay đổi", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(20.dp))

        // --- TÊN VÀ EMAIL ---
        if (isEditingName) {
            OutlinedTextField(
                value = tempName, onValueChange = { tempName = it }, label = { Text("Tên hiển thị", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00B8D4), unfocusedBorderColor = Color.DarkGray), singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = { isEditingName = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A35))) { Text("Hủy", color = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { userName = tempName; isEditingName = false; sharedPref.edit().putString("USER_NAME", tempName).apply() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4))) { Text("Lưu", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { tempName = userName; isEditingName = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(userEmail, color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- DANH SÁCH MENU SETTINGS ---
        SettingsGroup(title = "BẢO MẬT & TÀI KHOẢN") {
            SettingsItem(icon = Icons.Rounded.VpnKey, title = "Đổi mật khẩu", onClick = { showChangePassDialog = true })
            SettingsItem(icon = Icons.Rounded.Notifications, title = "Nhận thông báo biến động", hasSwitch = true, initialSwitchState = true)
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroup(title = "HỆ THỐNG") {
            SettingsItem(icon = Icons.Rounded.CleaningServices, title = "Xóa bộ nhớ đệm", onClick = {
                coroutineScope.launch {
                    Toast.makeText(context, "Đang dọn dẹp...", Toast.LENGTH_SHORT).show()
                    delay(1000)
                    Toast.makeText(context, "Đã giải phóng 45MB rác!", Toast.LENGTH_SHORT).show()
                }
            })
            SettingsItem(icon = Icons.Rounded.Info, title = "Phiên bản ứng dụng", value = "v1.0.1 (Beta)", onClick = null)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744))
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color(0xFFFF1744))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ĐĂNG XUẤT", color = Color(0xFFFF1744), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(130.dp)) // Né bottom bar
    }

    // --- DIALOG ĐỔI MẬT KHẨU ---
    if (showChangePassDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePassDialog = false },
            onNavigateToForgot = {
                showChangePassDialog = false
                onNavigateToForgot()
            },
            onSave = { oldPass, newPass ->
                // Giả lập lưu thành công cho Giai đoạn 1 (Làm Backend Phase sau)
                coroutineScope.launch {
                    Toast.makeText(context, "Đang kiểm tra...", Toast.LENGTH_SHORT).show()
                    delay(1000)
                    Toast.makeText(context, "✅ Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show()
                    showChangePassDialog = false
                }
            }
        )
    }
}

// ==========================================
// COMPONENT: Bảng Nhập Đổi Mật Khẩu
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2E),
        title = { Text("ĐỔI MẬT KHẨU", color = Color(0xFF00B8D4), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPass, onValueChange = { oldPass = it },
                    label = { Text("Mật khẩu cũ", color = Color.Gray) },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPass, onValueChange = { newPass = it },
                    label = { Text("Mật khẩu mới (8+ ký tự)", color = Color.Gray) },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPass, onValueChange = { confirmPass = it; errorMsg = null },
                    label = { Text("Xác nhận mật khẩu mới", color = Color.Gray) },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = errorMsg != null,
                    supportingText = { errorMsg?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { passVisible = !passVisible }) {
                        Icon(if (passVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (passVisible) "Ẩn" else "Hiện", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("Quên mật khẩu cũ?", color = Color.Cyan, fontSize = 12.sp, modifier = Modifier.clickable { onNavigateToForgot() })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPass.isBlank() || newPass.length < 8) errorMsg = "Vui lòng nhập đủ thông tin hợp lệ"
                    else if (newPass != confirmPass) errorMsg = "Mật khẩu mới không khớp!"
                    else onSave(oldPass, newPass)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B8D4))
            ) { Text("Lưu thay đổi", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        }
    )
}

// --- CÁC HÀM HỖ TRỢ ---
@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF161622)).border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(16.dp))) {
            content()
        }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String? = null, hasSwitch: Boolean = false, initialSwitchState: Boolean = false, onClick: (() -> Unit)? = null) {
    var switchState by remember { mutableStateOf(initialSwitchState) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp)
        }
        if (hasSwitch) {
            Switch(checked = switchState, onCheckedChange = { switchState = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00E676), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.DarkGray))
        } else if (value != null) {
            Text(value, color = Color.Gray, fontSize = 14.sp)
        } else if (onClick != null) {
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.DarkGray)
        }
    }
}

// HÀM SAO CHÉP ẢNH VÀO BỘ NHỚ TRONG (CỨU TINH VỤ LỖI MẤT ẢNH)
fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "avatar_profile.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) { null }
}

fun loadBitmapSafe(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE; decoder.isMutableRequired = true }
        } else MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } catch (e: Exception) { null }
}