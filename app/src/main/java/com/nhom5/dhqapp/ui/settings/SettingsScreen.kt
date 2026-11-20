package com.nhom5.dhqapp.ui.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nhom5.dhqapp.BackupRestoreActivity
import com.nhom5.dhqapp.ChangePasswordActivity
import com.nhom5.dhqapp.ExportCsvActivity
import com.nhom5.dhqapp.ProfileInfoActivity
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: Int,
    db: AppDatabase,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }

    // Tải thông tin user lần đầu
    LaunchedEffect(Unit) {
        user = db.userDao().findById(userId)
    }

    // Tải lại mỗi khi màn hình được hiển thị lại (sau khi sửa thông tin ở trang cá nhân)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { user = db.userDao().findById(userId) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Cài đặt & Hồ sơ") }) }) { padding ->
        // ✅ Một Column duy nhất, cuộn được — mọi thứ (kể cả nút Đăng xuất) cuộn chung
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thẻ hồ sơ
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user?.name?.firstOrNull()?.uppercase()?.toString() ?: "U",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column {
                        Text(user?.name ?: "Người dùng", fontWeight = FontWeight.Bold)
                        Text(
                            user?.email ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dark mode switch
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Giao diện tối (Dark mode)")
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
            }

            HorizontalDivider()

            // ✅ Xem thông tin cá nhân — nằm TRÊN mục Đổi mật khẩu
            Surface(
                onClick = { context.startActivity(Intent(context, ProfileInfoActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Xem thông tin cá nhân") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )
            }

            HorizontalDivider()

            // Đổi mật khẩu
            Surface(
                onClick = { context.startActivity(Intent(context, ChangePasswordActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Đổi mật khẩu") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )
            }

            // Xuất dữ liệu CSV
            Surface(
                onClick = { context.startActivity(android.content.Intent(context, ExportCsvActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Xuất dữ liệu CSV") },
                    supportingContent = { Text("Xuất giao dịch ra file Excel/CSV") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )
            }

            // Sao lưu / Khôi phục
            Surface(
                onClick = { context.startActivity(android.content.Intent(context, BackupRestoreActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Sao lưu / Khôi phục") },
                    supportingContent = { Text("Sao lưu file + khôi phục giao dịch đã xóa") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )
            }

            // ✅ Nút Đăng xuất nằm trong luồng cuộn, cách dòng cuối rõ rệt — KHÔNG bị đè
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Đăng xuất", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp)) // đệm đáy, tránh nút dính sát mép màn hình
        }
    }
}