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
    val error: String? = null,
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
        if (q.length < 2) {
            _state.update {
                it.copy(transactions = emptyList(), goals = emptyList(), shoppingLists = emptyList(), error = null)
            }
        }
    }

    /** Repete a última busca — o `queryFlow` não reemite um valor igual. */
    fun retry() {
        val q = _state.value.query
        if (q.length >= 2) search(q)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            // Percorre as páginas todas: com uma página só, quem tivesse mais de
            // 200 lançamentos não encontrava os mais antigos e a busca dizia que
            // não existiam.
            val txResult = financeRepository.getAllTransactions()
            val goalResult = goalRepository.getGoals()
            val listResult = shoppingRepository.getLists()

            val txs = txResult.map { it.data }.getOrDefault(emptyList()).filter { tx ->
                tx.description?.contains(query, ignoreCase = true) == true
            }
            val goals = goalResult.getOrDefault(emptyList()).filter { goal ->
                goal.name.contains(query, ignoreCase = true)
            }
            val lists = listResult.getOrDefault(emptyList()).filter { list ->
                list.name.contains(query, ignoreCase = true)
            }

            // As três falhas eram engolidas e o ecrã dizia "sem resultados" —
            // indistinguível de a busca ter corrido bem e não haver nada. Basta
            // uma falhar para o resultado estar incompleto, e um resultado
            // incompleto que se anuncia como completo é a mentira que o T9
            // fecha (a web já o corrigiu; o Android tinha ficado para trás).
            val falhou = txResult.isFailure || goalResult.isFailure || listResult.isFailure

            _state.update {
                it.copy(
                    isLoading = false,
                    transactions = txs,
                    goals = goals,
                    shoppingLists = lists,
                    error = if (falhou) "Não foi possível procurar. Tenta novamente." else null,
                )
            }
        }
    }
}
