package com.nhom5.dhqapp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.SessionManager
import com.nhom5.dhqapp.ui.theme.DHQAppTheme
import com.nhom5.dhqapp.ui.theme.ExpenseRed
import com.nhom5.dhqapp.ui.theme.IncomeGreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fmtMoney(v: Double): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(v.toLong()) + " đ"
private fun fmtDay(ts: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(ts))

private fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

data class DeletedItem(
    val id: Int, val categoryName: String, val type: String,
    val amount: Double, val note: String, val date: Long, val deletedAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
class BackupRestoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val session = SessionManager(this)
        setContent {
            DHQAppTheme(darkTheme = false) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var deleted by remember { mutableStateOf<List<DeletedItem>>(emptyList()) }
                var loading by remember { mutableStateOf(true) }
                var backingUp by remember { mutableStateOf(false) }

                fun loadDeleted() {
                    scope.launch {
                        loading = true
                        val userId = session.loggedInUserId.first()
                        val list = db.transactionDao().getDeletedByUserId(userId).map { t ->
                            val cat = db.categoryDao().getById(t.categoryId)?.name ?: "Khác"
                            DeletedItem(t.id, cat, t.type, t.amount, t.note, t.date, t.deletedAt)
                        }
                        deleted = list
                        loading = false
                    }
                }
                LaunchedEffect(Unit) { loadDeleted() }

                val backupLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri: Uri? ->
                    uri ?: return@rememberLauncherForActivityResult
                    scope.launch {
                        backingUp = true
                        try {
                            val userId = session.loggedInUserId.first()
                            val json = buildBackupJson(db, userId)
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(json.toByteArray(Charsets.UTF_8))
                            }
                            Toast.makeText(context, "Đã sao lưu thành công", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sao lưu thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally { backingUp = false }
                    }
                }

                Scaffold(topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Sao lưu / Khôi phục") },
                        navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, null) } }
                    )
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                        // ===== Phần 1: Sao lưu =====
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Sao lưu giao dịch", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("Tạo file JSON chứa tất cả giao dịch hiện có, lưu vào điện thoại.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                        backupLauncher.launch("dhq_backup_$stamp.json")
                                    },
                                    enabled = !backingUp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (backingUp) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("Đang sao lưu...") }
                                    else Text("Sao lưu ngay", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        // ===== Phần 2: Khôi phục giao dịch đã xóa =====
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Giao dịch đã xóa (${deleted.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (deleted.isNotEmpty()) {
                                TextButton(onClick = {
                                    scope.launch {
                                        val userId = session.loggedInUserId.first()
                                        db.transactionDao().restoreAllDeleted(userId)
                                        Toast.makeText(context, "Đã khôi phục tất cả", Toast.LENGTH_SHORT).show()
                                        loadDeleted()
                                    }
                                }) { Text("Khôi phục tất cả") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        when {
                            loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            deleted.isEmpty() -> Text(
                                "Thùng rác trống. Giao dịch bạn xóa sẽ nằm đây để khôi phục.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                items(deleted, key = { it.id }) { item ->
                                    Card(Modifier.fillMaxWidth()) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(item.categoryName, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "Tạo ${fmtDay(item.date)} · Xóa ${fmtDay(item.deletedAt)} · ${item.note.ifEmpty { "Không ghi chú" }}",
                                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                (if (item.type == "THU") "+ " else "- ") + fmtMoney(item.amount),
                                                color = if (item.type == "THU") IncomeGreen else ExpenseRed,
                                                fontWeight = FontWeight.Bold, fontSize = 13.sp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            TextButton(onClick = {
                                                scope.launch {
                                                    val userId = session.loggedInUserId.first()
                                                    db.transactionDao().restoreById(item.id, userId)
                                                    Toast.makeText(context, "Đã khôi phục", Toast.LENGTH_SHORT).show()
                                                    loadDeleted()
                                                }
                                            }) { Text("Khôi phục") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun buildBackupJson(db: AppDatabase, userId: Int): String {
        val txns = db.transactionDao().getActiveByUserId(userId)
        val sb = StringBuilder()
        sb.append("{\"app\":\"DHQ-App\",\"version\":1,\"userId\":$userId,\"exportedAt\":${System.currentTimeMillis()},\"transactions\":[")
        txns.forEachIndexed { i, t ->
            if (i > 0) sb.append(",")
            val cat = db.categoryDao().getById(t.categoryId)?.name?.escapeJson() ?: "Khác"
            sb.append("{\"id\":${t.id},\"category\":\"$cat\",\"type\":\"${t.type}\",\"amount\":${t.amount},\"note\":\"${t.note.escapeJson()}\",\"date\":${t.date}}")
        }
        sb.append("]}")
        return sb.toString()
    }
}