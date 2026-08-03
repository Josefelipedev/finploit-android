package com.finploit.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.DashboardResponse
import com.finploit.android.data.dto.MonthForecastDto
import com.finploit.android.data.repository.FinanceRepository
import com.finploit.android.util.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    /**
     * O recorte dos cartões. Antes não havia: chamava-se `/finance/dashboard`
     * sem datas, e sem datas o servidor não filtra — o cartão era "de sempre"
     * enquanto o mesmo cartão na web era o do período escolhido (B4).
     */
    val period: Period = Period.Last30Days,
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

    fun setPeriod(period: Period) {
        if (period == _uiState.value.period) return
        _uiState.value = _uiState.value.copy(period = period)
        loadDashboard()
    }

    fun loadDashboard() {
        val (start, end) = _uiState.value.period.range()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, data = null, error = null)
            financeRepository.getDashboard(start, end)
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
