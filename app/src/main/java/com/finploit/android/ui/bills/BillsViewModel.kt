package com.finploit.android.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.CreateBillRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.UpdateBillRequest
import com.finploit.android.data.repository.BillsRepository
import com.finploit.android.data.repository.FinanceCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BillsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val month: String = YearMonth.now().toString(),
    val items: List<BillItemDto> = emptyList(),
    val totalPending: Double = 0.0,
    val totalPaid: Double = 0.0,
    val expensePending: Double = 0.0,
    val expensePaid: Double = 0.0,
    val incomePending: Double = 0.0,
    val incomePaid: Double = 0.0,
    val projectedBalance: Double = 0.0,
    val realizedBalance: Double = 0.0,
    val categories: List<FinanceCategoryDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val repository: BillsRepository,
    private val categoryRepository: FinanceCategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState

    init {
        load(_uiState.value.month)
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories(active = true)
                .onSuccess { list -> _uiState.value = _uiState.value.copy(categories = list) }
        }
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
                expensePending = data?.expense?.pending ?: 0.0,
                expensePaid = data?.expense?.paid ?: 0.0,
                incomePending = data?.income?.pending ?: 0.0,
                incomePaid = data?.income?.paid ?: 0.0,
                projectedBalance = data?.projectedBalance ?: 0.0,
                realizedBalance = data?.realizedBalance ?: 0.0,
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

    fun createBill(
        description: String,
        amount: Double,
        dueDate: String,
        type: String,
        currency: String?,
        categoryId: Int?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = repository.createBill(
                CreateBillRequest(
                    description = description,
                    amount = amount,
                    dueDate = dueDate,
                    type = type,
                    currency = currency,
                    categoryId = categoryId,
                ),
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(error = "Não foi possível criar a conta. Tente novamente.")
            }
        }
    }

    fun updateBill(
        id: Int,
        description: String?,
        amount: Double?,
        dueDate: String?,
        categoryId: Int?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = repository.updateBill(
                id,
                UpdateBillRequest(
                    description = description,
                    amount = amount,
                    dueDate = dueDate,
                    categoryId = categoryId,
                ),
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(error = "Não foi possível editar a conta. Tente novamente.")
            }
        }
    }

    fun deleteBill(id: Int) {
        viewModelScope.launch {
            val result = repository.deleteBill(id)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(error = "Não foi possível excluir a conta. Tente novamente.")
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
