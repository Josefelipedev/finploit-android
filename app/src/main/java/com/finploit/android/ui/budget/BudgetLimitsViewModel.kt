package com.finploit.android.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.local.entity.BudgetLimitEntity
import com.finploit.android.data.repository.BudgetRepository
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val limits: List<BudgetLimitEntity> = emptyList(),
    val monthlySummary: Map<String, Double> = emptyMap(), // categoryName -> spent
    val showAddDialog: Boolean = false,
    val editingLimit: BudgetLimitEntity? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class BudgetLimitsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            budgetRepository.getAll().collect { limits ->
                _state.update { it.copy(limits = limits) }
            }
        }
        loadMonthlySummary()
    }

    private fun loadMonthlySummary() {
        viewModelScope.launch {
            val now = java.util.Calendar.getInstance()
            val year = now.get(java.util.Calendar.YEAR)
            val month = now.get(java.util.Calendar.MONTH) + 1
            val startDate = "%04d-%02d-01".format(year, month)
            val lastDay = now.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val endDate = "%04d-%02d-%02d".format(year, month, lastDay)
            financeRepository.getTransactions(startDate, endDate, limit = 200)
                .onSuccess { txs ->
                    // Group by categoryId as string key (budget names are user-defined)
                    val summary = txs
                        .filter { it.type == "expense" }
                        .groupBy { "Cat ${it.categoryId ?: 0}" }
                        .mapValues { (_, list) -> list.sumOf { it.amount ?: 0.0 } }
                    _state.update { it.copy(monthlySummary = summary) }
                }
        }
    }

    fun showAddDialog(limit: BudgetLimitEntity? = null) {
        _state.update { it.copy(showAddDialog = true, editingLimit = limit) }
    }

    fun hideDialog() {
        _state.update { it.copy(showAddDialog = false, editingLimit = null) }
    }

    fun save(categoryId: Int, categoryName: String, monthlyLimit: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            budgetRepository.upsert(categoryId, categoryName, monthlyLimit)
            _state.update { it.copy(isSaving = false, showAddDialog = false, editingLimit = null) }
        }
    }

    fun delete(categoryId: Int) {
        viewModelScope.launch { budgetRepository.delete(categoryId) }
    }
}
