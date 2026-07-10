package com.finploit.android.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CreateCategoryRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.UpdateCategoryRequest
import com.finploit.android.data.repository.FinanceCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<FinanceCategoryDto> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: FinanceCategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Include inactive categories so they can be toggled back on / managed.
            val result = repository.getCategories(active = false)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                categories = result.getOrDefault(emptyList()),
                error = if (result.isFailure) "Não foi possível carregar as categorias." else null,
            )
        }
    }

    fun create(name: String, description: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.create(
                CreateCategoryRequest(
                    name = trimmedName,
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                    isActive = true,
                ),
            )
            finishMutation(result.isSuccess, "Categoria criada.", result.exceptionOrNull())
        }
    }

    fun update(id: Int, name: String, description: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.update(
                id,
                UpdateCategoryRequest(
                    name = trimmedName,
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            finishMutation(result.isSuccess, "Categoria atualizada.", result.exceptionOrNull())
        }
    }

    fun toggle(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.toggleStatus(id)
            finishMutation(result.isSuccess, "Estado atualizado.", result.exceptionOrNull())
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.delete(id)
            finishMutation(result.isSuccess, "Categoria excluída.", result.exceptionOrNull())
        }
    }

    private fun finishMutation(success: Boolean, successMessage: String, error: Throwable?) {
        if (success) {
            _uiState.value = _uiState.value.copy(isSaving = false, message = successMessage)
            load()
        } else {
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = error?.message ?: "Não foi possível concluir. Tente novamente.",
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
