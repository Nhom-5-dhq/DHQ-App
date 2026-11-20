package com.nhom5.dhqapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, Category::class, Transaction::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration 2 → 3: thêm 2 cột isDeleted + deletedAt, giữ nguyên dữ liệu cũ
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dhq_app.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()   // vẫn giữ làm lớp an toàn
                    .addCallback(seedCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val thu = listOf("Lương", "Thưởng", "Đầu tư", "Thu khác")
                val chi = listOf("Ăn uống", "Di chuyển", "Mua sắm", "Nhà cửa", "Giải trí", "Sức khỏe", "Học tập", "Chi khác")
                thu.forEach { db.execSQL("INSERT INTO categories (name, type) VALUES ('$it', 'THU')") }
                chi.forEach { db.execSQL("INSERT INTO categories (name, type) VALUES ('$it', 'CHI')") }
            }
        }
    }
}