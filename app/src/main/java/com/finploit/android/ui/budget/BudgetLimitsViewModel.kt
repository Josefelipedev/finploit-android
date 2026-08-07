package com.finploit.android.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BudgetLimitDto
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.repository.BudgetRepository
import com.finploit.android.data.repository.FinanceCategoryRepository
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val limits: List<BudgetLimitDto> = emptyList(),
    /** As categorias do workspace, para escolher a que leva limite. */
    val categories: List<FinanceCategoryDto> = emptyList(),
    /** categoria (em minúsculas) -> gasto do mês, na moeda do utilizador */
    val monthlySummary: Map<String, Double> = emptyMap(),
    val displayCurrency: String? = null,
    val unconvertedCurrencies: List<String> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingLimit: BudgetLimitDto? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * O gasto por categoria vem do `/finance/summary`, já convertido para a moeda
 * do utilizador — somar a listagem no cliente misturava moedas.
 *
 * Os limites vêm do servidor (C1). Antes viviam numa base Room local e a
 * categoria era texto livre, identificada pelo `hashCode()` do nome: dois
 * telemóveis nunca concordavam, e o casal via orçamentos diferentes. Agora
 * escolhe-se uma categoria a sério, que é o que o servidor guarda.
 */
@HiltViewModel
class BudgetLimitsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: FinanceCategoryRepository,
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val limits = budgetRepository.getAll()
            val categories = categoryRepository.getCategories()
            _state.update {
                it.copy(
                    isLoading = false,
                    limits = limits.getOrDefault(emptyList()),
                    categories = categories.getOrDefault(emptyList()),
                    error = limits.exceptionOrNull()?.message,
                )
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
            financeRepository.getSummary(startDate, endDate)
                .onSuccess { summary ->
                    val spentByCategory = summary.byCategory.orEmpty()
                        .associate { it.categoryName.trim().lowercase() to it.despesas }
                    _state.update {
                        it.copy(
                            monthlySummary = spentByCategory,
                            displayCurrency = summary.displayCurrency,
                            unconvertedCurrencies = summary.unconvertedCurrencies ?: emptyList(),
                        )
                    }
                }
        }
    }

    fun showAddDialog(limit: BudgetLimitDto? = null) {
        _state.update { it.copy(showAddDialog = true, editingLimit = limit) }
    }

    fun hideDialog() {
        _state.update { it.copy(showAddDialog = false, editingLimit = null) }
    }

    fun save(categoryId: Int, monthlyLimit: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            budgetRepository.upsert(categoryId, monthlyLimit)
                .onSuccess {
                    _state.update {
                        it.copy(isSaving = false, showAddDialog = false, editingLimit = null)
                    }
                    load()
                }
                .onFailure { e ->
                    // Gravar pode falhar: antes era escrita local e o ecrã dizia
                    // sempre que tinha guardado.
                    _state.update {
                        it.copy(isSaving = false, error = e.message ?: "Não foi possível guardar.")
                    }
                }
        }
    }

    fun delete(categoryId: Int) {
        viewModelScope.launch {
            budgetRepository.delete(categoryId)
                .onSuccess { load() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Não foi possível remover.") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
