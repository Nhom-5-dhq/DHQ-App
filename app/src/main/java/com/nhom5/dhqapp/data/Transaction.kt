package com.nhom5.dhqapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val amount: Double,
    val type: String,      // "THU" hoặc "CHI"
    val note: String,
    val date: Long,        // timestamp
    val isDeleted: Int = 0,   // 0 = bình thường, 1 = đang trong thùng rác
    val deletedAt: Long = 0L  // thời điểm xóa (timestamp), 0 nếu chưa xóa
)