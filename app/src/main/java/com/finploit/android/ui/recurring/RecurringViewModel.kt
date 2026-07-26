package com.finploit.android.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CreateRecurringRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.data.repository.FinanceCategoryRepository
import com.finploit.android.data.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val isLoading: Boolean = false,
    val transactions: List<RecurringTransactionDto> = emptyList(),
    val categories: List<FinanceCategoryDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val deletingIds: Set<Int> = emptySet(),
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: RecurringRepository,
    private val categoryRepository: FinanceCategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState(isLoading = true))
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        loadCategories()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAll()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, transactions = it, error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories()
                .onSuccess { list -> _uiState.value = _uiState.value.copy(categories = list) }
        }
    }

    fun create(
        description: String,
        amount: Double,
        type: String,
        frequency: String,
        dueDay: Int,
        categoryId: Int,
        endDate: String,
        occurrences: Int,
        currency: String? = null,
    ) {
        // Backend maps: type == "despesa" → "expense", anything else → "income"
        // So we must send "despesa" for expenses; "receita" for income
        val backendType = when (type) {
            "expense" -> "despesa"
            "income" -> "receita"
            else -> type
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)
            repository.create(
                CreateRecurringRequest(
                    description = description,
                    amount = amount,
                    currency = currency,
                    type = backendType,
                    frequency = frequency,
                    dueDay = dueDay,
                    weekDay = 0,
                    notification = true,
                    categoryId = categoryId,
                    endDate = endDate,
                    occurrences = occurrences,
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                    loadAll()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, saveError = it.message) }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingIds = _uiState.value.deletingIds + id)
            repository.delete(id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        deletingIds = _uiState.value.deletingIds - id,
                        transactions = _uiState.value.transactions.filter { it.id != id },
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(deletingIds = _uiState.value.deletingIds - id)
                }
        }
    }

    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, saveError = null)
    }
}
