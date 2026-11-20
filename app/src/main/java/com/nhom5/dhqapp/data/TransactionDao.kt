package com.nhom5.dhqapp.data

import androidx.room.*

@Dao
interface TransactionDao {
    @Insert suspend fun insert(transaction: Transaction)
    @Update suspend fun update(transaction: Transaction)
    @Delete suspend fun delete(transaction: Transaction)   // xóa vĩnh viễn

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getByUserId(userId: Int): List<Transaction>          // tất cả (dùng sao lưu)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getActiveByUserId(userId: Int): List<Transaction>    // chưa xóa

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun getDeletedByUserId(userId: Int): List<Transaction>   // đã xóa (thùng rác)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Transaction?

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :time WHERE id = :id AND userId = :userId AND isDeleted = 0")
    suspend fun softDelete(id: Int, userId: Int, time: Long)

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = 0 WHERE id = :id AND userId = :userId AND isDeleted = 1")
    suspend fun restoreById(id: Int, userId: Int)

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = 0 WHERE userId = :userId AND isDeleted = 1")
    suspend fun restoreAllDeleted(userId: Int)
}