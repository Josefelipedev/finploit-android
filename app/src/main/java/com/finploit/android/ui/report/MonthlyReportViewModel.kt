package com.finploit.android.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class MonthlyReportState(
    val isLoading: Boolean = false,
    val transactions: List<FinanceItemDto> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val byCategory: Map<String, Double> = emptyMap(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val error: String? = null,
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MonthlyReportState())
    val state: StateFlow<MonthlyReportState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val month = _state.value.selectedMonth
            val year = _state.value.selectedYear
            val lastDay = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startDate = "%04d-%02d-01".format(year, month)
            val endDate = "%04d-%02d-%02d".format(year, month, lastDay)

            _state.update { it.copy(isLoading = true, error = null) }
            financeRepository.getTransactions(startDate, endDate, limit = 500)
                .onSuccess { txs ->
                    val income = txs.filter { it.type == "income" }.sumOf { it.amount ?: 0.0 }
                    val expense = txs.filter { it.type == "expense" }.sumOf { it.amount ?: 0.0 }
                    val byCategory = txs
                        .filter { it.type == "expense" }
                        .groupBy { tx -> if (tx.categoryId != null) "Cat ${tx.categoryId}" else "Sem categoria" }
                        .mapValues { (_, list) -> list.sumOf { it.amount ?: 0.0 } }
                        .entries.sortedByDescending { it.value }
                        .associate { it.toPair() }
                    _state.update { it.copy(isLoading = false, transactions = txs, totalIncome = income, totalExpense = expense, byCategory = byCategory) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun previousMonth() {
        val cur = _state.value
        val (newMonth, newYear) = if (cur.selectedMonth == 1) 12 to (cur.selectedYear - 1)
        else (cur.selectedMonth - 1) to cur.selectedYear
        _state.update { it.copy(selectedMonth = newMonth, selectedYear = newYear) }
        load()
    }

    fun nextMonth() {
        val cur = _state.value
        val now = Calendar.getInstance()
        val isCurrentMonth = cur.selectedYear == now.get(Calendar.YEAR) && cur.selectedMonth == now.get(Calendar.MONTH) + 1
        if (isCurrentMonth) return
        val (newMonth, newYear) = if (cur.selectedMonth == 12) 1 to (cur.selectedYear + 1)
        else (cur.selectedMonth + 1) to cur.selectedYear
        _state.update { it.copy(selectedMonth = newMonth, selectedYear = newYear) }
        load()
    }
}
