package com.finploit.android.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.PantryItemDto
import com.finploit.android.data.dto.UpsertPantryItemRequest
import com.finploit.android.data.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PantryUiState(
    val items: List<PantryItemDto> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingItem: PantryItemDto? = null,
    val nameInput: String = "",
    val quantityInput: String = "",
    val unitInput: String = "",
    val categoryInput: String = "",
    val expiryInput: String = "", // ISO date string "YYYY-MM-DD" or empty
    val isSaving: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
)

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repository: PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PantryUiState(isLoading = true))
    val state: StateFlow<PantryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getAll()
                .onSuccess { _state.value = _state.value.copy(isLoading = false, items = it) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun openAddDialog(item: PantryItemDto? = null) {
        _state.value = _state.value.copy(
            showAddDialog = true,
            editingItem = item,
            nameInput = item?.name ?: "",
            quantityInput = item?.quantity?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it) } ?: "",
            unitInput = item?.unit ?: "",
            categoryInput = item?.category ?: "",
            expiryInput = item?.expiresAt?.take(10) ?: "",
        )
    }

    fun closeDialog() {
        _state.value = _state.value.copy(showAddDialog = false, editingItem = null, nameInput = "", quantityInput = "", unitInput = "", categoryInput = "", expiryInput = "")
    }

    fun setName(v: String) { _state.value = _state.value.copy(nameInput = v) }
    fun setQuantity(v: String) { _state.value = _state.value.copy(quantityInput = v.filter { it.isDigit() || it == '.' }.take(6)) }
    fun setUnit(v: String) { _state.value = _state.value.copy(unitInput = v) }
    fun setCategory(v: String) { _state.value = _state.value.copy(categoryInput = v) }
    fun setExpiry(v: String) { _state.value = _state.value.copy(expiryInput = v) }

    fun save() {
        val name = _state.value.nameInput.trim()
        if (name.isEmpty()) return
        val editingItem = _state.value.editingItem
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val req = UpsertPantryItemRequest(
                name = name,
                quantity = _state.value.quantityInput.toDoubleOrNull(),
                unit = _state.value.unitInput.trim().ifEmpty { null },
                category = _state.value.categoryInput.trim().ifEmpty { null },
                expiresAt = _state.value.expiryInput.trim().ifEmpty { null },
            )
            // Use PATCH when editing an existing item (supports name changes)
            val result = if (editingItem != null) repository.update(editingItem.id, req)
                         else repository.upsert(req)
            result
                .onSuccess { saved ->
                    val existing = _state.value.items.toMutableList()
                    val idx = existing.indexOfFirst { it.id == saved.id }
                        .takeIf { it >= 0 } ?: existing.indexOfFirst { editingItem != null && it.id == editingItem.id }
                    if (idx >= 0) existing[idx] = saved else existing.add(saved)
                    _state.value = _state.value.copy(isSaving = false, items = existing, snackbarMessage = "\"${saved.name}\" guardado no estoque.")
                    closeDialog()
                }
                .onFailure { _state.value = _state.value.copy(isSaving = false, snackbarMessage = "Erro ao guardar item.") }
        }
    }

    fun remove(id: Int) {
        val name = _state.value.items.find { it.id == id }?.name ?: ""
        viewModelScope.launch {
            repository.remove(id)
                .onSuccess { _state.value = _state.value.copy(items = _state.value.items.filter { it.id != id }, snackbarMessage = "\"$name\" removido.") }
                .onFailure { _state.value = _state.value.copy(snackbarMessage = "Erro ao remover item.") }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clear()
                .onSuccess { _state.value = _state.value.copy(items = emptyList(), snackbarMessage = "Estoque limpo.") }
                .onFailure { _state.value = _state.value.copy(snackbarMessage = "Erro ao limpar estoque.") }
        }
    }

    fun clearSnackbar() { _state.value = _state.value.copy(snackbarMessage = null) }
}
