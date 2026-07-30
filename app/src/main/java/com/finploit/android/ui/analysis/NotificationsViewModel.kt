package com.finploit.android.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.repository.BillsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class UpcomingBill(
    val bill: BillItemDto,
    /** Dias até vencer; negativo quando já venceu. */
    val daysUntilDue: Int,
)

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val error: String? = null,
)

/** Janela de "a vencer", igual à da web. */
private const val HORIZON_DAYS = 30L

/**
 * As contas vêm de `GET /bills`, que é quem gera as ocorrências do mês, marca o
 * que está em atraso e diz a moeda de cada uma.
 *
 * Antes isto recalculava o vencimento a partir das recorrentes: só via as
 * mensais com `dueDay`, ignorava contas avulsas, não sabia o que já estava pago
 * e nunca mostrava atrasos.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val billsRepository: BillsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState(isLoading = true)

            val thisMonth = YearMonth.now()
            // Dois meses: uma janela de 30 dias a meio do mês atravessa o seguinte.
            val current = billsRepository.getBills(thisMonth.toString())
            val next = billsRepository.getBills(thisMonth.plusMonths(1).toString())

            val failure = current.exceptionOrNull() ?: next.exceptionOrNull()
            if (current.isFailure && next.isFailure) {
                _uiState.value = NotificationsUiState(error = failure?.message)
                return@launch
            }

            val today = LocalDate.now()
            val items = (current.getOrNull()?.items.orEmpty() + next.getOrNull()?.items.orEmpty())
                .filter { !it.isPaid }
                .distinctBy { it.id } // o mês seguinte repete as atrasadas
                .mapNotNull { bill ->
                    val due = runCatching { LocalDate.parse(bill.dueDate.take(10)) }.getOrNull()
                        ?: return@mapNotNull null
                    UpcomingBill(bill, ChronoUnit.DAYS.between(today, due).toInt())
                }
                .filter { it.daysUntilDue <= HORIZON_DAYS }
                .sortedBy { it.daysUntilDue }

            _uiState.value = NotificationsUiState(upcomingBills = items)
        }
    }
}
