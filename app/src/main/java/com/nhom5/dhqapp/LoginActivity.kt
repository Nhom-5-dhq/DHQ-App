
package com.nhom5.dhqapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.SessionManager
import com.nhom5.dhqapp.data.User
import com.nhom5.dhqapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager
    private lateinit var firebaseAuth: FirebaseAuth

    // Tạo tài khoản Admin mặc định nếu chưa có
    private suspend fun ensureAdminAccount() {

        val adminEmail = "admin@dhqapp.com"

        val existing = db.userDao().findByEmail(adminEmail)

        if (existing == null) {

            db.userDao().insert(
                User(
                    name = "Quản trị viên",
                    email = adminEmail,
                    phone = "",
                    password = "admin123",
                    role = "admin"
                )
            )
        }
    }

    // Chuyển màn hình theo quyền
    private suspend fun openCorrectScreen(user: User) {

        if (!user.isActive) {
            Toast.makeText(
                this,
                "Tài khoản của bạn đã bị khóa.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        session.setLoggedInUserId(user.id)

        val target = if (user.role == "admin") {
            AdminActivity::class.java
        } else {
            MainActivity::class.java
        }

        startActivity(Intent(this, target))
        finishAffinity()
    }

    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {

                val account = task.getResult(ApiException::class.java)

                val credential = GoogleAuthProvider.getCredential(
                    account.idToken,
                    null
                )

                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->

                        if (authTask.isSuccessful) {

                            val fbUser = firebaseAuth.currentUser

                            val email = fbUser?.email.orEmpty()
                            val name = fbUser?.displayName
                                ?: "Người dùng Google"

                            lifecycleScope.launch {

                                ensureAdminAccount()

                                var user = db.userDao().findByEmail(email)

                                // Chỉ tài khoản chưa tồn tại mới được tạo
                                if (user == null) {

                                    db.userDao().insert(
                                        User(
                                            name = name,
                                            email = email,
                                            phone = "",
                                            password = "google-login"
                                        )
                                    )

                                    user = db.userDao().findByEmail(email)
                                }

                                if (user != null) {

                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Đăng nhập Google thành công!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    openCorrectScreen(user)
                                }
                            }

                        } else {

                            Toast.makeText(
                                this@LoginActivity,
                                "Đăng nhập Google thất bại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

            } catch (e: ApiException) {

                Log.e(
                    "GoogleSignIn",
                    "Lỗi: ${e.statusCode}",
                    e
                )

                Toast.makeText(
                    this,
                    "Đăng nhập Google thất bại: ${e.statusCode}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        session = SessionManager(this)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.ivBack.setOnClickListener {

            val intent = Intent(
                this,
                WelcomeActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }

        binding.tvRegister.setOnClickListener {

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )

            finish()
        }

        binding.tvForgot.setOnClickListener {

            startActivity(
                Intent(this, ForgotPasswordActivity::class.java)
            )
        }

        // Đăng nhập Google
        binding.btnGoogle.setOnClickListener {

            val gso = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(
                    getString(R.string.default_web_client_id)
                )
                .requestEmail()
                .build()

            googleSignInLauncher.launch(
                GoogleSignIn.getClient(this, gso).signInIntent
            )
        }

        // Đăng nhập bằng email và mật khẩu
        binding.btnLogin.setOnClickListener {

            val email =
                binding.edtEmail.text?.toString()?.trim().orEmpty()

            val pass =
                binding.edtPassword.text?.toString().orEmpty()

            if (email.isEmpty() || pass.isEmpty()) {

                Toast.makeText(
                    this,
                    "Vui lòng nhập Email và Mật khẩu",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            lifecycleScope.launch {

                ensureAdminAccount()

                val user = db.userDao().findByEmail(email)

                when {

                    user == null -> {

                        Toast.makeText(
                            this@LoginActivity,
                            "Tài khoản chưa được đăng ký",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    user.password != pass -> {

                        Toast.makeText(
                            this@LoginActivity,
                            "Tài khoản hoặc mật khẩu không đúng",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    !user.isActive -> {

                        Toast.makeText(
                            this@LoginActivity,
                            "Tài khoản đã bị khóa",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {

                        Toast.makeText(
                            this@LoginActivity,
                            "Đăng nhập thành công!",
                            Toast.LENGTH_SHORT
                        ).show()

                        openCorrectScreen(user)
                    }
                }
            }
        }
    }
}
