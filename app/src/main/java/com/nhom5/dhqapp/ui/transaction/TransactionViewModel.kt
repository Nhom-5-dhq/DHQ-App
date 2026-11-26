package com.nhom5.dhqapp.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.Category
import com.nhom5.dhqapp.data.DatabaseSeeder
import com.nhom5.dhqapp.data.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val userId: Int,
    private val db: AppDatabase
) : ViewModel() {
    private val _uiState = MutableStateFlow<TransactionUiState>(TransactionUiState.Loading)
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init { load() }

    fun clearMessage() {
        _toastMessage.value = null
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TransactionUiState.Loading
            try {
                DatabaseSeeder.seedCategoriesIfEmpty(db)
                _categories.value = db.categoryDao().getAll()
                val txns = db.transactionDao().getActiveByUserId(userId)
                val items = txns.map { t ->
                    val cat = db.categoryDao().getById(t.categoryId)
                    TransactionListItem(
                        id = t.id,
                        categoryId = t.categoryId,
                        categoryName = cat?.name ?: "Khác",
                        type = t.type,
                        amount = t.amount,
                        note = t.note,
                        date = t.date
                    )
                }
                _uiState.value = TransactionUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error("Không thể tải dữ liệu giao dịch")
            }
        }
    }

    // --- QUẢN LÝ GIAO DỊCH ---
    fun addTransaction(type: String, categoryId: Int, amount: Double, note: String) {
        viewModelScope.launch {
            db.transactionDao().insert(
                Transaction(
                    userId = userId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    note = note,
                    date = System.currentTimeMillis()
                )
            )
            _toastMessage.value = "Đã thêm giao dịch"
            load()
        }
    }

    fun updateTransaction(id: Int, type: String, categoryId: Int, amount: Double, note: String) {
        viewModelScope.launch {
            val old = db.transactionDao().getById(id) ?: return@launch
            db.transactionDao().update(
                old.copy(
                    type = type,
                    categoryId = categoryId,
                    amount = amount,
                    note = note
                )
            )
            _toastMessage.value = "Đã lưu thay đổi giao dịch"
            load()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            db.transactionDao().softDelete(id, userId, System.currentTimeMillis())
            _toastMessage.value = "Đã xóa giao dịch"
            load()
        }
    }

    // --- QUẢN LÝ DANH MỤC ---
    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            val exists = _categories.value.any {
                it.name.equals(trimmed, ignoreCase = true) && it.type == type
            }
            if (exists) {
                _toastMessage.value = "Danh mục \"$trimmed\" đã tồn tại"
                return@launch
            }
            db.categoryDao().insert(Category(name = trimmed, type = type, isDefault = false))
            _toastMessage.value = "Đã thêm danh mục"
            load()
        }
    }

    fun updateCategory(category: Category, newName: String, newType: String) {
        viewModelScope.launch {
            if (category.isDefault) {
                _toastMessage.value = "Không thể sửa danh mục cố định"
                return@launch
            }
            val trimmed = newName.trim()
            val exists = _categories.value.any {
                it.id != category.id && it.name.equals(trimmed, ignoreCase = true) && it.type == newType
            }
            if (exists) {
                _toastMessage.value = "Tên danh mục đã tồn tại"
                return@launch
            }
            db.categoryDao().update(category.copy(name = trimmed, type = newType))
            _toastMessage.value = "Đã cập nhật danh mục"
            load()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (category.isDefault) {
                _toastMessage.value = "Không thể xóa danh mục cố định"
                return@launch
            }
            val usageCount = db.transactionDao().countActiveByCategoryId(category.id)
            if (usageCount > 0) {
                _toastMessage.value = "Danh mục đang có $usageCount giao dịch, không thể xóa"
                return@launch
            }
            db.categoryDao().delete(category)
            _toastMessage.value = "Đã xóa danh mục"
            load()
        }
    }
}