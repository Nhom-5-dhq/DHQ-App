package com.nhom5.dhqapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.nhom5.dhqapp.ui.dashboard.DashboardScreen
import com.nhom5.dhqapp.ui.theme.DHQAppTheme
import com.nhom5.dhqapp.ui.transaction.TransactionScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.nhom5.dhqapp.ui.report.ReportScreen
import com.nhom5.dhqapp.ui.settings.SettingsScreen

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Default.Home),
    TRANSACTION("Giao dịch", Icons.Default.ReceiptLong),
    REPORT("Báo cáo", Icons.Default.BarChart),
    SETTINGS("Cài đặt", Icons.Default.Settings)
}

class MainActivity : AppCompatActivity() {
    private lateinit var session: SessionManager
    private lateinit var db: AppDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        db = AppDatabase.getDatabase(this)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            var selectedTab by remember { mutableStateOf(Tab.HOME) }
            var ready by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {

                userId = session.loggedInUserId.first()

                val user = db.userDao().findById(userId)

                when {

                    user == null || !user.isActive -> {

                        session.logout()

                        startActivity(
                            Intent(
                                this@MainActivity,
                                WelcomeActivity::class.java
                            )
                        )

                        finish()
                    }

                    user.role == "admin" -> {

                        startActivity(
                            Intent(
                                this@MainActivity,
                                AdminActivity::class.java
                            )
                        )

                        finish()
                    }

                    else -> {
                        ready = true
                    }
                }
            }

            DHQAppTheme(darkTheme = darkMode) {
                if (!ready) {
                    Surface(Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() } }
                } else {
                    Scaffold(bottomBar = {
                        NavigationBar {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab }
                                )
                            }
                        }
                    }) { padding ->
                        Box(Modifier.padding(padding).fillMaxSize()) {
                            when (selectedTab) {
                                Tab.HOME -> DashboardScreen(userId, db)
                                Tab.TRANSACTION -> TransactionScreen(userId, db)
                                Tab.REPORT -> ReportScreen(userId, db)
                                Tab.SETTINGS -> SettingsScreen(
                                    userId = userId, db = db,
                                    darkMode = darkMode,
                                    onDarkModeChange = { darkMode = it },
                                    onLogout = {
                                        lifecycleScope.launch {
                                            // Đăng xuất cả Firebase, Google Sign-In và phiên Room/DataStore.
                                            FirebaseAuth.getInstance().signOut()
                                            val googleClient = GoogleSignIn.getClient(
                                                this@MainActivity,
                                                GoogleSignInOptions.DEFAULT_SIGN_IN
                                            )
                                            googleClient.signOut().addOnCompleteListener {
                                                lifecycleScope.launch {
                                                    session.logout()
                                                    val intent = Intent(this@MainActivity, WelcomeActivity::class.java).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    }
                                                    startActivity(intent)
                                                    finish()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Placeholder(text: String, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onLogout) { Text("Đăng xuất") }
    }
}