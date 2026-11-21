
package com.nhom5.dhqapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.SessionManager
import com.nhom5.dhqapp.data.PasswordChangeLog
import com.nhom5.dhqapp.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        session = SessionManager(this)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {

            val old =
                binding.edtOld.text?.toString().orEmpty()

            val new =
                binding.edtNew.text?.toString().orEmpty()

            val confirm =
                binding.edtConfirm.text?.toString().orEmpty()

            when {

                old.isEmpty() || new.isEmpty() || confirm.isEmpty() -> {

                    Toast.makeText(
                        this,
                        "Vui lòng nhập đầy đủ thông tin",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                new.length < 6 -> {

                    Toast.makeText(
                        this,
                        "Mật khẩu mới tối thiểu 6 ký tự",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                new != confirm -> {

                    Toast.makeText(
                        this,
                        "Xác nhận mật khẩu không khớp",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                old == new -> {

                    Toast.makeText(
                        this,
                        "Mật khẩu mới phải khác mật khẩu cũ",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {

                    lifecycleScope.launch {

                        val userId =
                            session.loggedInUserId.first()

                        val user =
                            db.userDao().findById(userId)

                        when {

                            user == null -> {

                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Phiên đăng nhập không hợp lệ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            user.password != old -> {

                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Mật khẩu hiện tại không đúng",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            !user.isActive -> {

                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Tài khoản đã bị khóa",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {

                                val now =
                                    System.currentTimeMillis()

                                db.userDao().update(
                                    user.copy(
                                        password = new,
                                        lastPasswordChangedAt = now,
                                        passwordChangedBy = "self"
                                    )
                                )

                                db.passwordChangeLogDao().insert(
                                    PasswordChangeLog(
                                        userId = user.id,
                                        action = "USER_CHANGED",
                                        changedBy = "user",
                                        createdAt = now,
                                        note = "Người dùng tự đổi mật khẩu"
                                    )
                                )

                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Đổi mật khẩu thành công!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}
