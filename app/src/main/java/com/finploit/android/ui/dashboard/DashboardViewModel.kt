package com.finploit.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.DashboardResponse
import com.finploit.android.data.dto.MonthForecastDto
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val data: DashboardResponse? = null,
    /**
     * O mês corrente até ao fim. Fica fora do `data` de propósito: falhar a
     * previsão não pode apagar o resto do ecrã, e o cartão simplesmente não
     * aparece enquanto não houver resposta.
     */
    val forecast: MonthForecastDto? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            financeRepository.getDashboard()
                .onSuccess { data -> _uiState.value = _uiState.value.copy(isLoading = false, data = data) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
        }
        loadForecast()
    }

    private fun loadForecast() {
        viewModelScope.launch {
            financeRepository.getMonthForecast()
                .onSuccess { f -> _uiState.value = _uiState.value.copy(forecast = f) }
        }
    }
}
