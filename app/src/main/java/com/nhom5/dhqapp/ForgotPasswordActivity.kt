
package com.nhom5.dhqapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhom5.dhqapp.data.*
import com.nhom5.dhqapp.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnReset.setOnClickListener {

            val input =
                binding.edtInput.text?.toString()?.trim().orEmpty()

            if (input.isBlank()) {

                Toast.makeText(
                    this,
                    "Vui lòng nhập Email hoặc Số điện thoại",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            lifecycleScope.launch {

                val user =
                    db.userDao().findByEmail(input)
                        ?: db.userDao().findByPhone(input)

                if (user == null || user.role != "user") {

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Không tìm thấy tài khoản người dùng",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // Kiểm tra yêu cầu đã duyệt nhưng chưa nhận
                val approved =
                    db.passwordResetDao()
                        .findUndeliveredApproved(user.id)

                if (approved != null) {

                    val pass =
                        approved.tempPassword.orEmpty()

                    db.passwordResetDao().markDelivered(
                        approved.id,
                        System.currentTimeMillis()
                    )

                    AlertDialog.Builder(
                        this@ForgotPasswordActivity
                    )
                        .setTitle("Yêu cầu đã được duyệt")
                        .setMessage(
                            """
                            Mật khẩu tạm của bạn:

                            $pass

                            Hãy đăng nhập và đổi mật khẩu ngay.
                            """.trimIndent()
                        )
                        .setPositiveButton("Đến đăng nhập") { _, _ ->

                            startActivity(
                                Intent(
                                    this@ForgotPasswordActivity,
                                    LoginActivity::class.java
                                )
                            )

                            finish()
                        }
                        .setCancelable(false)
                        .show()

                    return@launch
                }

                // Kiểm tra yêu cầu đang chờ
                val pending =
                    db.passwordResetDao().findPending(user.id)

                if (pending != null) {

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Yêu cầu đang chờ quản trị viên duyệt.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // Tạo yêu cầu mới
                db.passwordResetDao().insert(
                    PasswordResetRequest(
                        userId = user.id,
                        status = "PENDING"
                    )
                )

                // Ghi lịch sử
                db.passwordChangeLogDao().insert(
                    PasswordChangeLog(
                        userId = user.id,
                        action = "RESET_REQUESTED",
                        changedBy = "user",
                        note = "Người dùng gửi yêu cầu lấy lại mật khẩu"
                    )
                )

                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Đã gửi yêu cầu. Vui lòng chờ Admin duyệt.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
