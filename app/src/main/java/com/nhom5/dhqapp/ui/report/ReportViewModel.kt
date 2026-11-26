package com.nhom5.dhqapp.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.data.Category
import com.nhom5.dhqapp.data.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportViewModel(
    private val userId: Int,
    private val db: AppDatabase
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        load(ReportPeriod.MONTH)
    }

    fun load(period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            try {
                val allTxns = db.transactionDao().getActiveByUserId(userId)
                val catMap = db.categoryDao().getAll().associateBy { it.id }

                val (startTime, endTime) = getPeriodBounds(period)
                val filteredTxns = allTxns.filter { it.date in startTime until endTime }

                val incomeTxns = filteredTxns.filter { it.type == "THU" }
                val expenseTxns = filteredTxns.filter { it.type == "CHI" }

                val totalIncome = incomeTxns.sumOf { it.amount }
                val totalExpense = expenseTxns.sumOf { it.amount }

                val expenseBreakdown = buildBreakdown(expenseTxns, totalExpense, catMap)
                val incomeBreakdown = buildBreakdown(incomeTxns, totalIncome, catMap)

                val incomeBars = buildBarPoints(period, incomeTxns)
                val expenseBars = buildBarPoints(period, expenseTxns)

                _uiState.value = ReportUiState.Success(
                    period = period,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    topExpenseCategory = expenseBreakdown.firstOrNull(),
                    expenseBreakdown = expenseBreakdown,
                    incomeBreakdown = incomeBreakdown,
                    incomeBars = incomeBars,
                    expenseBars = expenseBars
                )
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Không thể tải báo cáo thống kê")
            }
        }
    }

    private fun getPeriodBounds(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (period) {
            ReportPeriod.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                start to cal.timeInMillis
            }
            ReportPeriod.WEEK -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                start to cal.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                start to cal.timeInMillis
            }
            ReportPeriod.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                start to cal.timeInMillis
            }
        }
    }

    private fun buildBreakdown(
        txns: List<Transaction>,
        total: Double,
        catMap: Map<Int, Category>
    ): List<CategoryStat> {
        return txns.groupBy { it.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.amount }
                val name = catMap[catId]?.name ?: "Khác"
                val percent = if (total > 0) (sum / total).toFloat() else 0f
                CategoryStat(name = name, amount = sum, percent = percent)
            }
            .sortedByDescending { it.amount }
    }

    private fun buildBarPoints(period: ReportPeriod, txns: List<Transaction>): List<BarPoint> {
        val cal = Calendar.getInstance()
        return when (period) {
            ReportPeriod.DAY -> {
                val labels = listOf("Sáng", "Trưa", "Chiều", "Tối")
                val sums = DoubleArray(4)
                txns.forEach { t ->
                    cal.timeInMillis = t.date
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val idx = when (hour) {
                        in 0..11 -> 0
                        in 12..14 -> 1
                        in 15..18 -> 2
                        else -> 3
                    }
                    sums[idx] += t.amount
                }
                labels.mapIndexed { i, label -> BarPoint(label, sums[i]) }
            }
            ReportPeriod.WEEK -> {
                val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                val sums = DoubleArray(7)
                txns.forEach { t ->
                    cal.timeInMillis = t.date
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    val idx = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
                    if (idx in 0..6) sums[idx] += t.amount
                }
                labels.mapIndexed { i, label -> BarPoint(label, sums[i]) }
            }
            ReportPeriod.MONTH -> {
                val labels = listOf("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4")
                val sums = DoubleArray(4)
                txns.forEach { t ->
                    cal.timeInMillis = t.date
                    val dom = cal.get(Calendar.DAY_OF_MONTH)
                    val idx = ((dom - 1) / 7).coerceIn(0, 3)
                    sums[idx] += t.amount
                }
                labels.mapIndexed { i, label -> BarPoint(label, sums[i]) }
            }
            ReportPeriod.YEAR -> {
                val labels = listOf("Quý 1", "Quý 2", "Quý 3", "Quý 4")
                val sums = DoubleArray(4)
                txns.forEach { t ->
                    cal.timeInMillis = t.date
                    val month = cal.get(Calendar.MONTH) // 0..11
                    val idx = (month / 3).coerceIn(0, 3)
                    sums[idx] += t.amount
                }
                labels.mapIndexed { i, label -> BarPoint(label, sums[i]) }
            }
        }
    }
}