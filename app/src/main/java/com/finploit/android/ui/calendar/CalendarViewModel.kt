package com.finploit.android.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.repository.FinanceRepository
import com.finploit.android.util.signedForTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = false,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedDay: Int? = null,
    val transactionsByDay: Map<Int, List<FinanceItemDto>> = emptyMap(),
    /** Saldo do dia na moeda do utilizador (soma dos valores já convertidos). */
    val dailyBalances: Map<Int, Double> = emptyMap(),
    val displayCurrency: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val month = _state.value.selectedMonth
            val year = _state.value.selectedYear
            val lastDay = Calendar.getInstance().apply {
                set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
            _state.update { it.copy(isLoading = true) }
            financeRepository.getAllTransactions(
                startDate = "%04d-%02d-01".format(year, month),
                endDate = "%04d-%02d-%02d".format(year, month, lastDay),
            ).onSuccess { page ->
                // Pelo dia do movimento (`referenceDate`), que é por onde a API
                // filtra o período — agrupar por `createdAt` punha no dia da
                // digitação tudo o que foi lançado com atraso.
                val byDay = page.data.groupBy { tx ->
                    tx.movementDate.split("-").lastOrNull()?.toIntOrNull() ?: 1
                }
                // `amountForTotals` é o valor já convertido pelo servidor: somar
                // `amount` misturava moedas (100 BRL + 100 EUR davam 200).
                //
                // `signedForTotals` deixa de fora o tipo que não se sabe somar —
                // antes, tudo o que não fosse `income` entrava como despesa.
                val dailyBalances = byDay.mapValues { (_, list) ->
                    list.sumOf { tx -> signedForTotals(tx.type, tx.amountForTotals) ?: 0.0 }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        transactionsByDay = byDay,
                        dailyBalances = dailyBalances,
                        displayCurrency = page.meta.displayCurrency,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectDay(day: Int) {
        _state.update { it.copy(selectedDay = if (it.selectedDay == day) null else day) }
    }

    fun previousMonth() {
        val cur = _state.value
        val (newMonth, newYear) = if (cur.selectedMonth == 1) 12 to (cur.selectedYear - 1) else (cur.selectedMonth - 1) to cur.selectedYear
        _state.update { it.copy(selectedMonth = newMonth, selectedYear = newYear, selectedDay = null) }
        load()
    }

    fun nextMonth() {
        val cur = _state.value
        val now = Calendar.getInstance()
        if (cur.selectedYear == now.get(Calendar.YEAR) && cur.selectedMonth == now.get(Calendar.MONTH) + 1) return
        val (newMonth, newYear) = if (cur.selectedMonth == 12) 1 to (cur.selectedYear + 1) else (cur.selectedMonth + 1) to cur.selectedYear
        _state.update { it.copy(selectedMonth = newMonth, selectedYear = newYear, selectedDay = null) }
        load()
    }
}
