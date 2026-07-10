package com.finploit.android.ui.fiscal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.FiscalObligationsResponse
import com.finploit.android.data.repository.FiscalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FiscalUiState(
    val isLoading: Boolean = true,
    val data: FiscalObligationsResponse? = null,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val isConfigured: Boolean get() = data?.configured == true
}

@HiltViewModel
class FiscalViewModel @Inject constructor(
    private val repository: FiscalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiscalUiState())
    val uiState: StateFlow<FiscalUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getObligations()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                data = result.getOrNull(),
                error = if (result.isFailure) "Não foi possível carregar as obrigações fiscais." else null,
            )
        }
    }

    fun saveProfile(activityStartDate: String, fiscalNumber: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            val result = repository.saveProfile(
                country = "PT",
                regime = "PT_SIMPLIFICADO_ISENCAO_ART53",
                activityStartDate = activityStartDate,
                fiscalNumber = fiscalNumber,
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "Perfil fiscal guardado.",
                )
                refresh()
            } else {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = result.exceptionOrNull()?.message ?: "Não foi possível guardar. Tente novamente.",
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
