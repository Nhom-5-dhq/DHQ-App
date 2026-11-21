
package com.nhom5.dhqapp.data

import androidx.room.*

@Dao
interface PasswordResetDao {

    @Insert
    suspend fun insert(item: PasswordResetRequest): Long

    // Lấy các yêu cầu đang chờ
    @Query("""
        SELECT * FROM password_reset_requests
        WHERE status = 'PENDING'
        ORDER BY requestedAt ASC
    """)
    suspend fun getPending(): List<PasswordResetRequest>

    // Kiểm tra yêu cầu đang chờ của một người
    @Query("""
        SELECT * FROM password_reset_requests
        WHERE userId = :userId
        AND status = 'PENDING'
        LIMIT 1
    """)
    suspend fun findPending(userId: Int): PasswordResetRequest?

    // Tìm mật khẩu tạm đã duyệt nhưng chưa nhận
    @Query("""
        SELECT * FROM password_reset_requests
        WHERE userId = :userId
        AND status = 'APPROVED'
        AND deliveredAt IS NULL
        ORDER BY processedAt DESC
        LIMIT 1
    """)
    suspend fun findUndeliveredApproved(
        userId: Int
    ): PasswordResetRequest?

    // Admin duyệt hoặc từ chối
    @Query("""
        UPDATE password_reset_requests
        SET status = :status,
            tempPassword = :password,
            processedAt = :time,
            processedBy = :admin
        WHERE id = :id
        AND status = 'PENDING'
    """)
    suspend fun process(
        id: Int,
        status: String,
        password: String?,
        time: Long,
        admin: String
    ): Int

    // Đánh dấu đã nhận mật khẩu tạm
    @Query("""
        UPDATE password_reset_requests
        SET deliveredAt = :time
        WHERE id = :id
    """)
    suspend fun markDelivered(
        id: Int,
        time: Long
    )
}
