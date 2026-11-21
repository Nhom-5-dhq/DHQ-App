
package com.nhom5.dhqapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "password_reset_requests",
    indices = [
        Index("userId"),
        Index("status")
    ]
)
data class PasswordResetRequest(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    // PENDING, APPROVED, REJECTED
    val status: String = "PENDING",

    // Mật khẩu tạm do Admin tạo
    val tempPassword: String? = null,

    val requestedAt: Long = System.currentTimeMillis(),

    val processedAt: Long? = null,

    val processedBy: String? = null,

    // Thời gian người dùng nhận mật khẩu
    val deliveredAt: Long? = null
)
