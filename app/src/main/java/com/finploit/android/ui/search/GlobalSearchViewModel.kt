package com.finploit.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.dto.GoalDto
import com.finploit.android.data.dto.ShoppingListDto
import com.finploit.android.data.repository.FinanceRepository
import com.finploit.android.data.repository.GoalRepository
import com.finploit.android.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalSearchState(
    val query: String = "",
    val transactions: List<FinanceItemDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    val shoppingLists: List<ShoppingListDto> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val goalRepository: GoalRepository,
    private val shoppingRepository: ShoppingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSearchState())
    val state: StateFlow<GlobalSearchState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .filter { it.length >= 2 }
                .collect { q -> search(q) }
        }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        queryFlow.value = q
        if (q.length < 2) _state.update { it.copy(transactions = emptyList(), goals = emptyList(), shoppingLists = emptyList()) }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val txResult = financeRepository.getTransactions(limit = 200)
            val txs = txResult.getOrDefault(emptyList()).filter { tx ->
                tx.description?.contains(query, ignoreCase = true) == true
            }
            val goals = goalRepository.getGoals().getOrDefault(emptyList()).filter { goal ->
                goal.name.contains(query, ignoreCase = true)
            }
            val lists = shoppingRepository.getLists().getOrDefault(emptyList()).filter { list ->
                list.name.contains(query, ignoreCase = true)
            }
            _state.update { it.copy(isLoading = false, transactions = txs, goals = goals, shoppingLists = lists) }
        }
    }
}
