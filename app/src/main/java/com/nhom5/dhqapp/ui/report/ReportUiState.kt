package com.nhom5.dhqapp.ui.report

enum class ReportPeriod(val label: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng"),
    YEAR("Năm")
}

data class CategoryStat(
    val name: String,
    val amount: Double,
    val percent: Float   // 0..1
)

data class BarPoint(
    val label: String,
    val amount: Double
)

sealed interface ReportUiState {
    data object Loading : ReportUiState
    data class Success(
        val period: ReportPeriod,
        val totalIncome: Double,
        val totalExpense: Double,
        val topExpenseCategory: CategoryStat?,
        val expenseBreakdown: List<CategoryStat>,
        val incomeBreakdown: List<CategoryStat>,
        val incomeBars: List<BarPoint>,
        val expenseBars: List<BarPoint>
    ) : ReportUiState
    data class Error(val message: String) : ReportUiState
}