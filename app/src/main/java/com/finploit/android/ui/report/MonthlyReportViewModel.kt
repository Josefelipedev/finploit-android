package com.finploit.android.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CategoryBreakdownDto
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class MonthlyReportState(
    val isLoading: Boolean = false,
    val transactionCount: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val byCategory: List<CategoryBreakdownDto> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    /** Moeda em que os totais acima estão expressos (a do utilizador). */
    val displayCurrency: String? = null,
    /** Moedas somadas sem conversão — o total é aproximado enquanto isto não estiver vazio. */
    val unconvertedCurrencies: List<String> = emptyList(),
    val error: String? = null,
)

/**
 * Os totais e o corte por categoria vêm do `/finance/summary`, já convertidos
 * para a moeda do utilizador.
 *
 * Antes este ecrã pedia a listagem e somava `amount` — que é o valor na moeda
 * de cada lançamento. Num casal com contas em EUR e BRL isso dava a soma crua
 * de moedas diferentes (5.345,29 onde o valor real era 919,10 €), e as
 * categorias apareciam como "Cat 3" porque a listagem só traz o id.
 */
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
            financeRepository.getSummary(startDate, endDate)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            totalIncome = summary.totalGanhos,
                            totalExpense = summary.totalDespesas,
                            transactionCount = summary.transactionCount,
                            byCategory = summary.byCategory
                                ?.filter { cat -> cat.despesas > 0 }
                                ?: emptyList(),
                            displayCurrency = summary.displayCurrency,
                            unconvertedCurrencies = summary.unconvertedCurrencies ?: emptyList(),
                        )
                    }
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
