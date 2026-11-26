package com.nhom5.dhqapp.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhom5.dhqapp.data.Category
import com.nhom5.dhqapp.ui.theme.BluePrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    categories: List<Category>,
    editing: TransactionListItem? = null,   // null = thêm mới, != null = xem chi tiết / sửa
    onDismiss: () -> Unit,
    onSave: (type: String, categoryId: Int, amount: Double, note: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(editing?.type ?: "CHI") }
    var selectedCategoryId by remember {
        mutableStateOf(
            editing?.categoryId
                ?: categories.firstOrNull { it.type == "CHI" }?.id
                ?: -1
        )
    }
    var amountText by remember { mutableStateOf(editing?.let { it.amount.toLong().toString() } ?: "") }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val catsByType = categories.filter { it.type == type }
    val selectedCatName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Chọn danh mục"
    val dateFormatted = remember(editing?.date) {
        editing?.date?.let {
            SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale("vi", "VN")).format(Date(it))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFFF3F6FC),
        title = {
            Column {
                Text(
                    text = if (editing == null) "Thêm giao dịch" else "Chi tiết giao dịch",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                if (dateFormatted != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Ngày tạo: $dateFormatted",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = type == "THU",
                        onClick = {
                            type = "THU"
                            selectedCategoryId = categories.firstOrNull { it.type == "THU" }?.id ?: -1
                        },
                        label = { Text("Thu") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEADDFF)
                        )
                    )
                    FilterChip(
                        selected = type == "CHI",
                        onClick = {
                            type = "CHI"
                            selectedCategoryId = categories.firstOrNull { it.type == "CHI" }?.id ?: -1
                        },
                        label = { Text("Chi") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEADDFF)
                        )
                    )
                }

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Danh mục") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        catsByType.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() }
                        error = null
                    },
                    label = { Text("Số tiền (VND)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú (không bắt buộc)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                when {
                    selectedCategoryId == -1 -> error = "Vui lòng chọn danh mục"
                    amount <= 0 -> error = "Số tiền phải lớn hơn 0"
                    else -> onSave(type, selectedCategoryId, amount, note.trim())
                }
            }) {
                Text("Lưu", color = BluePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = BluePrimary)
                }
            }
        }
    )
}