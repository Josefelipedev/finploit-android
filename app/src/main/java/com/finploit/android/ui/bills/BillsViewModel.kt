package com.finploit.android.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.repository.BillsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BillsUiState(
    val isLoading: Boolean = true,
    val month: String = YearMonth.now().toString(),
    val items: List<BillItemDto> = emptyList(),
    val totalPending: Double = 0.0,
    val totalPaid: Double = 0.0,
    val error: String? = null,
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val repository: BillsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState

    init {
        load(_uiState.value.month)
    }

    fun load(month: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, month = month, error = null)
            val result = repository.getBills(month)
            val data = result.getOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                month = data?.month?.takeIf { it.isNotBlank() } ?: month,
                items = data?.items ?: emptyList(),
                totalPending = data?.totalPending ?: 0.0,
                totalPaid = data?.totalPaid ?: 0.0,
                error = if (result.isFailure) "Não foi possível carregar as contas." else null,
            )
        }
    }

    fun prevMonth() {
        load(shiftMonth(-1))
    }

    fun nextMonth() {
        load(shiftMonth(1))
    }

    fun togglePaid(item: BillItemDto, amount: Double? = null) {
        viewModelScope.launch {
            val result = if (item.isPaid) repository.unpay(item.id) else repository.pay(item.id, amount)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Não foi possível atualizar a conta. Tente novamente.",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun shiftMonth(delta: Long): String =
        runCatching { YearMonth.parse(_uiState.value.month).plusMonths(delta).toString() }
            .getOrElse { YearMonth.now().plusMonths(delta).toString() }
}
