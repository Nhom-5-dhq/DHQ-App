package com.nhom5.dhqapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        User::class,
        Category::class,
        Transaction::class,
        PasswordResetRequest::class,
        PasswordChangeLog::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun passwordResetDao(): PasswordResetDao

    abstract fun passwordChangeLogDao(): PasswordChangeLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration từ version 2 lên 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE transactions
                    ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0
                """)

                db.execSQL("""
                    ALTER TABLE transactions
                    ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        // Migration từ version 3 lên 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bổ sung quyền tài khoản
                db.execSQL("""
                    ALTER TABLE users
                    ADD COLUMN role TEXT NOT NULL DEFAULT 'user'
                """)

                // Trạng thái khóa tài khoản
                db.execSQL("""
                    ALTER TABLE users
                    ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1
                """)

                // Thời gian đổi mật khẩu
                db.execSQL("""
                    ALTER TABLE users
                    ADD COLUMN lastPasswordChangedAt
                    INTEGER NOT NULL DEFAULT 0
                """)

                // Người đổi mật khẩu
                db.execSQL("""
                    ALTER TABLE users
                    ADD COLUMN passwordChangedBy
                    TEXT NOT NULL DEFAULT 'self'
                """)

                // Bảng yêu cầu lấy lại mật khẩu
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS password_reset_requests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        tempPassword TEXT,
                        requestedAt INTEGER NOT NULL,
                        processedAt INTEGER,
                        processedBy TEXT,
                        deliveredAt INTEGER
                    )
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS
                    index_password_reset_requests_userId
                    ON password_reset_requests(userId)
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS
                    index_password_reset_requests_status
                    ON password_reset_requests(status)
                """)

                // Bảng lịch sử đổi mật khẩu
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS password_change_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        changedBy TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS
                    index_password_change_logs_userId
                    ON password_change_logs(userId)
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS
                    index_password_change_logs_createdAt
                    ON password_change_logs(createdAt)
                """)
            }
        }

        // Migration từ version 4 lên 5 (thêm cột isDefault cho danh mục cố định)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE categories
                    ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0
                """)

                db.execSQL("""
                    UPDATE categories SET isDefault = 1
                    WHERE name IN (
                        'Lương', 'Thưởng', 'Đầu tư', 'Thu khác',
                        'Ăn uống', 'Di chuyển', 'Mua sắm', 'Nhà cửa',
                        'Giải trí', 'Sức khỏe', 'Học tập', 'Chi khác'
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dhq_app.db"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .addCallback(seedCallback)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Khởi tạo 12 danh mục mặc định (cố định)
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val thu = listOf(
                    "Lương",
                    "Thưởng",
                    "Đầu tư",
                    "Thu khác"
                )

                val chi = listOf(
                    "Ăn uống",
                    "Di chuyển",
                    "Mua sắm",
                    "Nhà cửa",
                    "Giải trí",
                    "Sức khỏe",
                    "Học tập",
                    "Chi khác"
                )

                thu.forEach {
                    db.execSQL(
                        "INSERT INTO categories (name, type, isDefault) VALUES ('$it', 'THU', 1)"
                    )
                }

                chi.forEach {
                    db.execSQL(
                        "INSERT INTO categories (name, type, isDefault) VALUES ('$it', 'CHI', 1)"
                    )
                }
            }
        }
    }
}