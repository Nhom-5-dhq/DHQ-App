package com.nhom5.dhqapp

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.SessionManager
import com.nhom5.dhqapp.data.User
import com.nhom5.dhqapp.databinding.ActivityProfileInfoBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileInfoBinding
    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager

    private var currentUser: User? = null
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)
        session = SessionManager(this)

        binding.ivBack.setOnClickListener { finish() }

        // Load thông tin user từ Room
        lifecycleScope.launch {
            val userId = session.loggedInUserId.first()
            val user = db.userDao().findById(userId)
            currentUser = user
            if (user == null) {
                Toast.makeText(this@ProfileInfoActivity, "Lỗi phiên đăng nhập", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            binding.edtName.setText(user.name)
            binding.edtEmail.setText(user.email)
            binding.edtPhone.setText(user.phone)
        }

        // Nút Sửa (màu đỏ) — bấm lần 1: mở khóa sửa, đổi chữ thành "Lưu lại"; bấm lần 2: lưu
        binding.btnEdit.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                setFieldsEnabled(true)
                binding.btnEdit.text = "Lưu lại"
                binding.edtName.requestFocus()
            } else {
                val name = binding.edtName.text?.toString().orEmpty().trim()
                val email = binding.edtEmail.text?.toString().orEmpty().trim()
                val phone = binding.edtPhone.text?.toString().orEmpty().trim()

                when {
                    name.isEmpty() -> toast("Vui lòng nhập họ tên")
                    email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Email không hợp lệ")
                    phone.isEmpty() -> toast("Vui lòng nhập số điện thoại")
                    else -> lifecycleScope.launch {
                        val user = currentUser ?: return@launch

                        // Nếu đổi email: kiểm tra trùng email của tài khoản khác (email có unique index)
                        if (email != user.email) {
                            val existing = db.userDao().findByEmail(email)
                            if (existing != null && existing.id != user.id) {
                                toast("Email này đã được đăng ký bởi tài khoản khác")
                                return@launch
                            }
                        }
                        // Nếu đổi SĐT: kiểm tra trùng số điện thoại của tài khoản khác
                        if (phone != user.phone) {
                            val existing = db.userDao().findByPhone(phone)
                            if (existing != null && existing.id != user.id) {
                                toast("Số điện thoại này đã được đăng ký bởi tài khoản khác")
                                return@launch
                            }
                        }

                        val updated = user.copy(name = name, email = email, phone = phone)
                        db.userDao().update(updated)
                        currentUser = updated

                        isEditing = false
                        setFieldsEnabled(false)
                        binding.btnEdit.text = "Sửa"
                        toast("Đã cập nhật thông tin!")
                    }
                }
            }
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        binding.tilName.isEnabled = enabled
        binding.tilEmail.isEnabled = enabled
        binding.tilPhone.isEnabled = enabled
        binding.edtName.isEnabled = enabled
        binding.edtEmail.isEnabled = enabled
        binding.edtPhone.isEnabled = enabled
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}