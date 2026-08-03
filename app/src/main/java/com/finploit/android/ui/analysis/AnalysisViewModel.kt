package com.finploit.android.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.repository.AnalysisRepository
import com.finploit.android.util.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val isLoadingAnalysis: Boolean = false,
    val isLoadingInsight: Boolean = false,
    val period: Period = Period.Last30Days,
    val analysis: AnalysisResponse? = null,
    val insight: String? = null,
    val analysisError: String? = null,
    val insightError: String? = null,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val repository: AnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState(isLoadingAnalysis = true, isLoadingInsight = true))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun setPeriod(period: Period) {
        if (period == _uiState.value.period) return
        _uiState.value = _uiState.value.copy(period = period)
        loadAll()
    }

    fun loadAll() {
        loadAnalysis()
        loadInsight()
    }

    fun loadAnalysis() {
        val (start, end) = _uiState.value.period.range()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAnalysis = true, analysisError = null)
            repository.getAnalysis(start, end)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoadingAnalysis = false, analysis = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoadingAnalysis = false, analysisError = it.message) }
        }
    }

    fun loadInsight() {
        val (start, end) = _uiState.value.period.range()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInsight = true, insightError = null)
            repository.getInsight(start, end)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoadingInsight = false, insight = it.insight) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoadingInsight = false, insightError = it.message) }
        }
    }
}
