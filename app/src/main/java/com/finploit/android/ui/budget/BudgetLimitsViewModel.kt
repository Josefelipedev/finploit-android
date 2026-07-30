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
    /** categoria (em minúsculas) -> gasto do mês, na moeda do utilizador */
    val monthlySummary: Map<String, Double> = emptyMap(),
    val displayCurrency: String? = null,
    val unconvertedCurrencies: List<String> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingLimit: BudgetLimitEntity? = null,
    val isSaving: Boolean = false,
)

/**
 * O gasto por categoria vem do `/finance/summary`, já convertido para a moeda
 * do utilizador — somar a listagem no cliente misturava moedas.
 *
 * A chave do mapa é o nome da categoria em minúsculas: os limites guardados
 * localmente não têm o id real da categoria (são criados com `hashCode()` do
 * nome), e a versão anterior indexava por `"Cat $id"` enquanto o ecrã procurava
 * pelo nome — nunca havia correspondência e cada limite aparecia com 0 gasto.
 */
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
