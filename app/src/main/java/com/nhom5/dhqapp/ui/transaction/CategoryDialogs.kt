package com.nhom5.dhqapp.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhom5.dhqapp.data.Category
import com.nhom5.dhqapp.ui.theme.BluePrimary

@Composable
fun CategoryFormDialog(
    editing: Category? = null, // null = Thêm danh mục, != null = Sửa/Xóa danh mục
    onDismiss: () -> Unit,
    onSave: (name: String, type: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(editing?.type ?: "CHI") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFFF3F6FC),
        title = {
            Text(
                text = if (editing == null) "Thêm danh mục" else "Chi tiết danh mục",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Loại danh mục:",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = type == "THU",
                        onClick = { type = "THU" },
                        label = { Text("Thu", fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEADDFF),
                            selectedLabelColor = Color(0xFF1F2937)
                        )
                    )
                    FilterChip(
                        selected = type == "CHI",
                        onClick = { type = "CHI" },
                        label = { Text("Chi", fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEADDFF),
                            selectedLabelColor = Color(0xFF1F2937)
                        )
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Tên danh mục") },
                    placeholder = { Text("Ví dụ: Mua sắm, Tiền thưởng...") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().isEmpty()) {
                    error = "Vui lòng nhập tên danh mục"
                } else {
                    onSave(name.trim(), type)
                }
            }) {
                Text("Lưu", color = BluePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (editing != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = BluePrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    )
}