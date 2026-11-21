
package com.nhom5.dhqapp.data

import androidx.room.*

@Dao
interface PasswordChangeLogDao {

    // Thêm lịch sử
    @Insert
    suspend fun insert(log: PasswordChangeLog)

    // Lấy toàn bộ lịch sử mới nhất trước
    @Query("""
        SELECT * FROM password_change_logs
        ORDER BY createdAt DESC
    """)
    suspend fun getAll(): List<PasswordChangeLog>
}
