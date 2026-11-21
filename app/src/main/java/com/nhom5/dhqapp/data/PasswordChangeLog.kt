
package com.nhom5.dhqapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "password_change_logs",
    indices = [
        Index("userId"),
        Index("createdAt")
    ]
)
data class PasswordChangeLog(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    // Loại hành động
    val action: String,

    // Người thực hiện
    val changedBy: String,

    // Thời gian
    val createdAt: Long = System.currentTimeMillis(),

    // Nội dung ghi chú
    val note: String = ""
)
