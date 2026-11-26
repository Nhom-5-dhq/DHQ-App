package com.nhom5.dhqapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom5.dhqapp.data.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(
    private val userId: Int,
    private val db: AppDatabase
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            delay(300)
            try {
                val txns = db.transactionDao().getActiveByUserId(userId)

                // Tổng thu, tổng chi và số dư vẫn tính trên toàn bộ giao dịch
                val income = txns.filter { it.type == "THU" }.sumOf { it.amount }
                val expense = txns.filter { it.type == "CHI" }.sumOf { it.amount }

                // Tính mốc thời gian 00:00:00.000 của ngày hôm nay và ngày mai
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val startOfTomorrow = calendar.timeInMillis

                // Chỉ lấy các giao dịch được thêm trong ngày hôm nay
                val todayTxns = txns.filter { it.date in startOfToday until startOfTomorrow }

                val recent = todayTxns.map { t ->
                    val cat = db.categoryDao().getById(t.categoryId)
                    TransactionWithCat(t, cat?.name ?: "Khác")
                }

                _uiState.value = DashboardUiState.Success(income, expense, income - expense, recent)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error("Không thể tải thông tin tài chính, vui lòng thử lại")
            }
        }
    }
}