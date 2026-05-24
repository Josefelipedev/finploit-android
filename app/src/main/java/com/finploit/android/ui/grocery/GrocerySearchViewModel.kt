package com.finploit.android.ui.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.GroceryProductDto
import com.finploit.android.data.dto.NearbyStoreDto
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.GrocerySearchRepository
import com.finploit.android.data.service.LocationService
import com.finploit.android.data.service.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroceryUiState(
    val query: String = "",
    val postalCode: String = "1000-001",
    val location: UserLocation? = null,
    val selectedSupermarkets: Set<String> = setOf("continente", "auchan", "pingodoce"),
    val hiddenSupermarkets: Set<String> = emptySet(),
    val products: List<GroceryProductDto> = emptyList(),
    val nearbyStores: List<NearbyStoreDto> = emptyList(),
    val enrichedItems: List<EnrichedShoppingItemDto> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isEnriching: Boolean = false,
    val error: String? = null,
    val locationError: String? = null,
    val mode: GroceryMode = GroceryMode.SEARCH,
    val hasSearched: Boolean = false,
    val lastQuery: String = "",
    val planSearchQuery: String = "",
    val planStoreFilter: String? = null,
    val searchStoreFilter: String? = null,
)

enum class GroceryMode { SEARCH, PLAN_PRICES }

@HiltViewModel
class GrocerySearchViewModel @Inject constructor(
    private val repository: GrocerySearchRepository,
    private val locationService: LocationService,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroceryUiState())
    val state: StateFlow<GroceryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val postal = preferencesRepository.postalCode.first()
            val selected = preferencesRepository.selectedSupermarkets.first()
            val hidden = preferencesRepository.hiddenSupermarkets.first()
            _state.update { it.copy(postalCode = postal, selectedSupermarkets = selected, hiddenSupermarkets = hidden) }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q, error = null) }

    fun setPostalCode(code: String) {
        _state.update { it.copy(postalCode = code) }
        viewModelScope.launch { preferencesRepository.setPostalCode(code) }
    }

    fun toggleSupermarket(chain: String) {
        _state.update { s ->
            val new = if (chain in s.selectedSupermarkets)
                s.selectedSupermarkets - chain
            else
                s.selectedSupermarkets + chain
            s.copy(selectedSupermarkets = new.ifEmpty { s.selectedSupermarkets })
        }
        viewModelScope.launch {
            preferencesRepository.setSelectedSupermarkets(_state.value.selectedSupermarkets)
        }
    }

    fun hideSupermarket(chain: String) {
        _state.update { s ->
            val newHidden = s.hiddenSupermarkets + chain
            val newSelected = (s.selectedSupermarkets - chain).ifEmpty { s.selectedSupermarkets }
            s.copy(hiddenSupermarkets = newHidden, selectedSupermarkets = newSelected)
        }
        viewModelScope.launch {
            preferencesRepository.setHiddenSupermarkets(_state.value.hiddenSupermarkets)
            preferencesRepository.setSelectedSupermarkets(_state.value.selectedSupermarkets)
        }
    }

    fun restoreHiddenSupermarkets() {
        _state.update { it.copy(hiddenSupermarkets = emptySet()) }
        viewModelScope.launch { preferencesRepository.setHiddenSupermarkets(emptySet()) }
    }

    fun setMode(mode: GroceryMode) = _state.update { it.copy(mode = mode) }

    fun clearEnrichedItems() = _state.update { it.copy(enrichedItems = emptyList(), error = null) }

    fun setPlanSearchQuery(q: String) = _state.update { it.copy(planSearchQuery = q) }
    fun setPlanStoreFilter(store: String?) = _state.update { it.copy(planStoreFilter = store) }
    fun setSearchStoreFilter(store: String?) = _state.update { it.copy(searchStoreFilter = store) }

    fun detectLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLocation = true, locationError = null) }
            val loc = locationService.getCurrentLocation()
            if (loc == null) {
                _state.update { it.copy(isLoadingLocation = false, locationError = "Localização indisponível") }
                return@launch
            }
            // Get nearest postal code from backend
            val postal = repository.getNearestPostalCode(loc.lat, loc.lng)
                .getOrNull() ?: loc.postalCode ?: "1000-001"

            // Get nearby stores
            val stores = repository.getStoresNearby(
                loc.lat, loc.lng,
                chains = _state.value.selectedSupermarkets.toList(),
            ).getOrNull() ?: emptyList()

            _state.update {
                it.copy(
                    location = loc,
                    postalCode = postal,
                    nearbyStores = stores,
                    isLoadingLocation = false,
                )
            }
            preferencesRepository.setPostalCode(postal)
        }
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null, products = emptyList(), hasSearched = false, lastQuery = q, searchStoreFilter = null) }
            val result = repository.search(
                q,
                _state.value.selectedSupermarkets.toList(),
                _state.value.postalCode,
            )
            _state.update { s ->
                result.fold(
                    onSuccess = { resp -> s.copy(products = resp.products, isSearching = false, hasSearched = true) },
                    onFailure = { e -> s.copy(error = e.message ?: "Erro na pesquisa", isSearching = false, hasSearched = true) },
                )
            }
        }
    }

    fun enrichPlanShoppingList(shoppingItems: List<EnrichItem>) {
        if (shoppingItems.isEmpty()) {
            _state.update { it.copy(enrichedItems = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isEnriching = true, error = null, mode = GroceryMode.PLAN_PRICES) }
            val result = repository.enrichItems(
                shoppingItems,
                _state.value.selectedSupermarkets.toList(),
                _state.value.postalCode,
            )
            _state.update { s ->
                result.fold(
                    onSuccess = { items -> s.copy(enrichedItems = items, isEnriching = false) },
                    onFailure = { e -> s.copy(error = e.message, isEnriching = false) },
                )
            }
        }
    }
}
