package com.finploit.android.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.repository.BankAccountRepository
import com.finploit.android.data.repository.FinanceCategoryRepository
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val transactions: List<FinanceItemDto> = emptyList(),
    val error: String? = null,
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val createError: String? = null,
    val deletingIds: Set<Int> = emptySet(),
    val searchQuery: String = "",
    val typeFilter: String = "all",
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val editingTransaction: FinanceItemDto? = null,
    val isUpdating: Boolean = false,
    val updateError: String? = null,
    val snackbarMessage: String? = null,
    val categories: List<FinanceCategoryDto> = emptyList(),
    val accounts: List<BankAccountDto> = emptyList(),
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val categoryRepository: FinanceCategoryRepository,
    private val bankAccountRepository: BankAccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState(isLoading = true))
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
        loadCategories()
        loadAccounts()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, currentPage = 1)
            financeRepository.getTransactions(page = 1, limit = 20)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactions = list,
                        currentPage = 1,
                        hasMore = list.size >= 20,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories()
                .onSuccess { list -> _uiState.value = _uiState.value.copy(categories = list) }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            bankAccountRepository.getAll()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(accounts = list.filter { !it.isArchived })
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isLoading) return
        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            financeRepository.getTransactions(page = nextPage, limit = 20)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        transactions = _uiState.value.transactions + list,
                        currentPage = nextPage,
                        hasMore = list.size >= 20,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
        }
    }

    fun setSearch(query: String) { _uiState.value = _uiState.value.copy(searchQuery = query) }
    fun setTypeFilter(filter: String) { _uiState.value = _uiState.value.copy(typeFilter = filter) }

    fun selectForEdit(tx: FinanceItemDto) {
        _uiState.value = _uiState.value.copy(editingTransaction = tx, updateError = null)
    }

    fun clearEdit() {
        _uiState.value = _uiState.value.copy(editingTransaction = null, updateError = null)
    }

    fun createTransaction(
        type: String,
        amount: Double,
        description: String?,
        categoryId: Int?,
        referenceDate: String? = null,
        accountId: Int? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null, createSuccess = false)
            financeRepository.createTransaction(type, amount, description, categoryId, referenceDate, accountId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isCreating = false, createSuccess = true)
                    loadTransactions()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreating = false, createError = e.message)
                }
        }
    }

    fun updateTransaction(
        id: Int,
        type: String,
        amount: Double,
        description: String?,
        referenceDate: String? = null,
        categoryId: Int? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, updateError = null)
            financeRepository.updateTransaction(id, type, amount, description, referenceDate, categoryId)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        editingTransaction = null,
                        transactions = _uiState.value.transactions.map { if (it.id == id) updated else it },
                        snackbarMessage = "Transação atualizada! ✅",
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isUpdating = false, updateError = e.message)
                }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingIds = _uiState.value.deletingIds + id)
            financeRepository.deleteTransaction(id)
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

    fun clearCreateState() {
        _uiState.value = _uiState.value.copy(createSuccess = false, createError = null)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
