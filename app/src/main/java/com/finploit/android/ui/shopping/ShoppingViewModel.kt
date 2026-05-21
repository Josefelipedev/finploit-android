package com.finploit.android.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CreateOrUpdateShoppingItemRequest
import com.finploit.android.data.dto.MonthlyPlanResponse
import com.finploit.android.data.dto.ShoppingListDto
import com.finploit.android.data.dto.StorePriceDto
import com.finploit.android.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val isLoading: Boolean = false,
    val lists: List<ShoppingListDto> = emptyList(),
    val selectedList: ShoppingListDto? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val isEnriching: Boolean = false,
    val enrichResult: String? = null,
    val storePrices: List<StorePriceDto> = emptyList(),
    val storePricesItem: String? = null,
    val isLoadingPrices: Boolean = false,
    val monthlyPlan: MonthlyPlanResponse? = null,
    val isGeneratingMonthly: Boolean = false,
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repository: ShoppingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState(isLoading = true))
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init { loadLists() }

    fun loadLists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getLists()
                .onSuccess { _uiState.value = ShoppingUiState(lists = it) }
                .onFailure { _uiState.value = ShoppingUiState(error = it.message) }
        }
    }

    fun selectList(list: ShoppingListDto) {
        _uiState.value = _uiState.value.copy(selectedList = list)
        refreshSelectedList(list.id)
    }

    fun clearSelectedList() {
        _uiState.value = _uiState.value.copy(selectedList = null)
    }

    private fun refreshSelectedList(id: Int) {
        viewModelScope.launch {
            repository.getListById(id)
                .onSuccess { _uiState.value = _uiState.value.copy(selectedList = it) }
        }
    }

    fun createList(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)
            repository.createList(name)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                    loadLists()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, saveError = it.message) }
        }
    }

    fun deleteList(id: Int) {
        viewModelScope.launch {
            repository.deleteList(id).onSuccess { loadLists() }
        }
    }

    fun addOrUpdateItem(listId: Int, name: String, quantity: Double, unit: String, price: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)
            repository.createOrUpdateItem(
                CreateOrUpdateShoppingItemRequest(
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    price = price,
                    shoppingListId = listId,
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                    refreshSelectedList(listId)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, saveError = it.message) }
        }
    }

    fun toggleItem(itemId: Int, purchased: Boolean, listId: Int) {
        viewModelScope.launch {
            repository.toggleItem(itemId, purchased).onSuccess { refreshSelectedList(listId) }
        }
    }

    fun deleteItem(itemId: Int, listId: Int) {
        viewModelScope.launch {
            repository.deleteItem(itemId).onSuccess { refreshSelectedList(listId) }
        }
    }

    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, saveError = null)
    }

    fun enrichPrices(listId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEnriching = true, enrichResult = null)
            repository.enrichPrices(listId)
                .onSuccess { result ->
                    val msg = "Preços actualizados: ${result.enriched} itens."
                    _uiState.value = _uiState.value.copy(isEnriching = false, enrichResult = msg)
                    refreshSelectedList(listId)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isEnriching = false,
                        enrichResult = err.message ?: "Erro ao buscar preços",
                    )
                }
        }
    }

    fun clearEnrichResult() {
        _uiState.value = _uiState.value.copy(enrichResult = null)
    }

    fun loadStorePrices(itemName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPrices = true, storePrices = emptyList(), storePricesItem = itemName)
            repository.getPricesByStore(itemName)
                .onSuccess { prices ->
                    _uiState.value = _uiState.value.copy(isLoadingPrices = false, storePrices = prices)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingPrices = false)
                }
        }
    }

    fun clearStorePrices() {
        _uiState.value = _uiState.value.copy(storePrices = emptyList(), storePricesItem = null)
    }

    fun generateMonthlyPlan(budget: Double? = null, notes: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingMonthly = true, saveError = null)
            repository.generateMonthlyPlan(budget, notes)
                .onSuccess { plan ->
                    _uiState.value = _uiState.value.copy(isGeneratingMonthly = false, monthlyPlan = plan)
                    loadLists()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isGeneratingMonthly = false, saveError = err.message)
                }
        }
    }

    fun clearMonthlyPlan() {
        _uiState.value = _uiState.value.copy(monthlyPlan = null)
    }
}
