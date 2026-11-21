
package com.nhom5.dhqapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val email: String,
    val phone: String,
    val password: String,

    // Phân quyền tài khoản
    @ColumnInfo(defaultValue = "'user'")
    val role: String = "user",

    // Trạng thái tài khoản
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true,

    // Thời gian đổi mật khẩu gần nhất
    @ColumnInfo(defaultValue = "0")
    val lastPasswordChangedAt: Long = 0L,

    // Người thực hiện đổi mật khẩu
    @ColumnInfo(defaultValue = "'self'")
    val passwordChangedBy: String = "self"
)
