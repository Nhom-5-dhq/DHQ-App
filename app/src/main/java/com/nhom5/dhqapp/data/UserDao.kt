
package com.nhom5.dhqapp.data

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    // Tìm tài khoản theo email
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    // Tìm tài khoản theo số điện thoại
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): User?

    // Tìm tài khoản theo ID
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): User?

    // Lấy danh sách người dùng, không lấy Admin
    @Query("""
        SELECT * FROM users
        WHERE role = 'user'
        ORDER BY name COLLATE NOCASE
    """)
    suspend fun getAllCustomers(): List<User>

    // Đếm tổng số người dùng
    @Query("SELECT COUNT(*) FROM users WHERE role = 'user'")
    suspend fun countCustomers(): Int

    // Đặt mật khẩu mới
    @Query("""
        UPDATE users
        SET password = :password,
            lastPasswordChangedAt = :time,
            passwordChangedBy = :by
        WHERE id = :id AND role = 'user'
    """)
    suspend fun updatePassword(
        id: Int,
        password: String,
        time: Long,
        by: String
    ): Int

    // Khóa hoặc mở khóa tài khoản
    @Query("""
        UPDATE users
        SET isActive = :active
        WHERE id = :id AND role = 'user'
    """)
    suspend fun setActive(
        id: Int,
        active: Boolean
    ): Int
}
