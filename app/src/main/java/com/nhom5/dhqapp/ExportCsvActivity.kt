package com.nhom5.dhqapp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class ExportCsvActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val session = SessionManager(this)
        setContent {
            DHQAppTheme(darkTheme = false) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var period by remember { mutableStateOf("MONTH") }
                var exporting by remember { mutableStateOf(false) }

                val saveLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/csv")
                ) { uri: Uri? ->
                    uri ?: return@rememberLauncherForActivityResult
                    scope.launch {
                        exporting = true
                        try {
                            val userId = session.loggedInUserId.first()
                            val csv = buildCsv(db, userId, period)
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(csv.toByteArray(Charsets.UTF_8))
                            }
                            Toast.makeText(context, "Đã xuất file CSV thành công", Toast.LENGTH_LONG).show()
                            finish()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Xuất file thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally { exporting = false }
                    }
                }

                Scaffold(topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Xuất dữ liệu CSV") },
                        navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, null) } }
                    )
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                        Text("Chọn khoảng thời gian xuất:", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        listOf(
                            "WEEK" to "Tuần này (7 ngày gần nhất)",
                            "MONTH" to "Tháng này (30 ngày gần nhất)",
                            "ALL" to "Tất cả giao dịch"
                        ).forEach { (value, label) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { period = value }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = period == value, onClick = { period = value })
                                Spacer(Modifier.width(12.dp))
                                Text(label)
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                saveLauncher.launch("dhq_giaodich_${period.lowercase()}_$stamp.csv")
                            },
                            enabled = !exporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (exporting) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Đang xuất...")
                            } else Text("Xuất file CSV", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Bấm nút trên sẽ mở hộp thoại cho bạn chọn vị trí lưu (thư mục Download hoặc Bộ nhớ trong). File mở được bằng Excel / Google Sheets.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private suspend fun buildCsv(db: AppDatabase, userId: Int, period: String): String {
        val all = db.transactionDao().getActiveByUserId(userId)
        val now = System.currentTimeMillis()
        val minTime = when (period) {
            "WEEK" -> now - 7L * 24 * 3600 * 1000
            "MONTH" -> now - 30L * 24 * 3600 * 1000
            else -> 0L
        }
        val fmtDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        val lines = mutableListOf("STT,Ngày,Loại,Danh mục,Số tiền,Ghi chú")
        all.filter { it.date >= minTime }.sortedByDescending { it.date }.forEachIndexed { i, t ->
            val cat = db.categoryDao().getById(t.categoryId)?.name ?: "Khác"
            val type = if (t.type == "THU") "Thu" else "Chi"
            val note = t.note.replace(",", " ").replace("\n", " ")
            lines.add("${i + 1},${fmtDate.format(Date(t.date))},$type,\"$cat\",${t.amount},\"$note\"")
        }
        // BOM \uFEFF để Excel đọc đúng tiếng Việt UTF-8
        return "\uFEFF" + lines.joinToString("\n")
    }
}