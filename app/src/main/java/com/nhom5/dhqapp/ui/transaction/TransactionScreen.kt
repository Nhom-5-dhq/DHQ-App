package com.nhom5.dhqapp.ui.transaction

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.Category
import com.nhom5.dhqapp.ui.theme.BlueLight
import com.nhom5.dhqapp.ui.theme.BluePrimary
import com.nhom5.dhqapp.ui.theme.ExpenseRed
import com.nhom5.dhqapp.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val CardLavenderBg = Color(0xFFEBE4EE)
private val ScreenBg = Color(0xFFF8F9FC)

private fun fmt(v: Double): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(v.toLong()) + " đ"

private fun fmtDate(ts: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(ts))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(userId: Int, db: AppDatabase) {
    val context = LocalContext.current
    val viewModel: TransactionViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(userId, db) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Điều hướng giữa 3 màn hình con trong tab Giao dịch
    var subScreen by remember { mutableStateOf(TransactionSubScreen.MAIN) }

    // Hỗ trợ nút Back vật lý của Android khi đang ở màn hình con
    BackHandler(enabled = subScreen != TransactionSubScreen.MAIN) {
        subScreen = TransactionSubScreen.MAIN
    }

    when (subScreen) {
        TransactionSubScreen.MAIN -> {
            TransactionMainSubScreen(
                uiState = uiState,
                categories = categories,
                onOpenCategories = { subScreen = TransactionSubScreen.CATEGORIES },
                onOpenHistory = { subScreen = TransactionSubScreen.HISTORY },
                onAddTransaction = { type, catId, amount, note ->
                    viewModel.addTransaction(type, catId, amount, note)
                }
            )
        }

        TransactionSubScreen.CATEGORIES -> {
            CategoryManagementSubScreen(
                categories = categories,
                onBack = { subScreen = TransactionSubScreen.MAIN },
                onAddCategory = { name, type -> viewModel.addCategory(name, type) },
                onUpdateCategory = { cat, newName, newType ->
                    viewModel.updateCategory(cat, newName, newType)
                },
                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) }
            )
        }

        TransactionSubScreen.HISTORY -> {
            TransactionHistorySubScreen(
                uiState = uiState,
                categories = categories,
                onBack = { subScreen = TransactionSubScreen.MAIN },
                onRetry = { viewModel.load() },
                onUpdateTransaction = { id, type, catId, amount, note ->
                    viewModel.updateTransaction(id, type, catId, amount, note)
                },
                onDeleteTransaction = { id ->
                    viewModel.deleteTransaction(id)
                }
            )
        }
    }
}

// =========================================================================
// 1. MÀN HÌNH CHÍNH "GIAO DỊCH" (Frame 2 trong Figma)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionMainSubScreen(
    uiState: TransactionUiState,
    categories: List<Category>,
    onOpenCategories: () -> Unit,
    onOpenHistory: () -> Unit,
    onAddTransaction: (type: String, categoryId: Int, amount: Double, note: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val transactionCount = (uiState as? TransactionUiState.Success)?.items?.size ?: 0

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Giao dịch", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BlueLight,
                contentColor = Color(0xFF0D253F),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nút điều hướng: Danh mục
            OutlinedCard(
                onClick = onOpenCategories,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF9CA3AF)),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Danh mục", fontSize = 16.sp, color = Color(0xFF374151))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            // Nút điều hướng: Lịch sử giao dịch
            OutlinedCard(
                onClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF9CA3AF)),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lịch sử giao dịch", fontSize = 16.sp, color = Color(0xFF374151))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            // Vùng thông báo trạng thái ở giữa màn hình
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (transactionCount == 0) {
                        "Chưa có giao dịch. Bấm + để thêm."
                    } else {
                        "Hiện có $transactionCount giao dịch. Bấm + để thêm mới."
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }

    if (showAddDialog) {
        TransactionFormDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { type, catId, amount, note ->
                onAddTransaction(type, catId, amount, note)
                showAddDialog = false
            }
        )
    }
}

// =========================================================================
// 2. MÀN HÌNH CON "DANH MỤC" (Frame 3 & 4 trong Figma)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManagementSubScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onAddCategory: (name: String, type: String) -> Unit,
    onUpdateCategory: (category: Category, newName: String, newType: String) -> Unit,
    onDeleteCategory: (category: Category) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "THU", "CHI"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    val filteredCategories = remember(categories, selectedFilter) {
        when (selectedFilter) {
            "THU" -> categories.filter { it.type == "THU" }
            "CHI" -> categories.filter { it.type == "CHI" }
            else -> categories
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Danh mục", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BlueLight,
                contentColor = Color(0xFF0D253F),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm danh mục")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 3 nút lọc: Tất cả | Thu | Chi
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Tất cả") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEADDFF)
                    )
                )
                FilterChip(
                    selected = selectedFilter == "THU",
                    onClick = { selectedFilter = "THU" },
                    label = { Text("Thu") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEADDFF)
                    )
                )
                FilterChip(
                    selected = selectedFilter == "CHI",
                    onClick = { selectedFilter = "CHI" },
                    label = { Text("Chi") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEADDFF)
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredCategories, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (cat.isDefault) {
                                    Toast.makeText(
                                        context,
                                        "\"${cat.name}\" là danh mục cố định, không thể sửa hoặc xóa",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    editingCategory = cat
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardLavenderBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon tròn xanh da trời giống Figma
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF90CAF9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(BluePrimary)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                    if (cat.isDefault) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = "Cố định",
                                                fontSize = 10.sp,
                                                color = Color(0xFF6B7280),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (cat.type == "THU") "Danh mục Thu" else "Danh mục Chi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (cat.type == "THU") IncomeGreen else ExpenseRed
                                )
                            }

                            if (!cat.isDefault) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Sửa hoặc xóa",
                                    tint = Color(0xFF4B5563)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Popup thêm danh mục mới
    if (showAddDialog) {
        CategoryFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, type ->
                onAddCategory(name, type)
                showAddDialog = false
            }
        )
    }

    // Popup sửa / xóa danh mục do người dùng thêm
    editingCategory?.let { cat ->
        CategoryFormDialog(
            editing = cat,
            onDismiss = { editingCategory = null },
            onSave = { newName, newType ->
                onUpdateCategory(cat, newName, newType)
                editingCategory = null
            },
            onDelete = {
                onDeleteCategory(cat)
                editingCategory = null
            }
        )
    }
}

// =========================================================================
// 3. MÀN HÌNH CON "LỊCH SỬ GIAO DỊCH" (Frame 1 trong Figma)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistorySubScreen(
    uiState: TransactionUiState,
    categories: List<Category>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUpdateTransaction: (id: Int, type: String, categoryId: Int, amount: Double, note: String) -> Unit,
    onDeleteTransaction: (id: Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<TransactionListItem?>(null) }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lịch sử giao dịch", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Tìm kiếm theo tên danh mục
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Tìm kiếm theo danh mục") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.height(14.dp))

            when (uiState) {
                is TransactionUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is TransactionUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.message)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onRetry) { Text("Thử lại") }
                        }
                    }
                }

                is TransactionUiState.Success -> {
                    val filtered = uiState.items.filter {
                        it.categoryName.contains(query.trim(), ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (query.isBlank()) "Chưa có giao dịch nào" else "Không tìm thấy danh mục \"$query\"",
                                color = Color(0xFF6B7280)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTransaction = item },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardLavenderBg)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.categoryName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${fmtDate(item.date)} · ${item.note.ifEmpty { "Không có ghi chú" }}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF4B5563)
                                            )
                                        }
                                        Text(
                                            text = (if (item.type == "THU") "+ " else "- ") + fmt(item.amount),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.type == "THU") IncomeGreen else ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bấm vào giao dịch trong lịch sử -> Xem chi tiết, Sửa/Lưu hoặc Xóa
    selectedTransaction?.let { item ->
        TransactionFormDialog(
            categories = categories,
            editing = item,
            onDismiss = { selectedTransaction = null },
            onSave = { type, catId, amount, note ->
                onUpdateTransaction(item.id, type, catId, amount, note)
                selectedTransaction = null
            },
            onDelete = {
                onDeleteTransaction(item.id)
                selectedTransaction = null
            }
        )
    }
}