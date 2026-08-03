package com.finploit.android.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * O período que a Análise está a ver.
 *
 * O ecrã pedia sempre o histórico todo, porque era o único que a API sabia
 * devolver. Agora que ela aceita datas (as mesmas que a web usa), o telemóvel
 * passa a poder fazer a pergunta com um recorte — e "Tudo" continua aqui, que
 * é o que este ecrã sempre mostrou.
 */
enum class AnalysisPeriod(val label: String) {
    Last30Days("30 dias"),
    ThisMonth("Este mês"),
    ThisYear("Este ano"),
    All("Tudo");

    /** `null` no início e no fim significa "sem filtro", como a API espera. */
    fun range(today: LocalDate = LocalDate.now()): Pair<String?, String?> = when (this) {
        Last30Days -> today.minusDays(30).toString() to today.toString()
        ThisMonth -> today.withDayOfMonth(1).toString() to today.toString()
        ThisYear -> today.withDayOfYear(1).toString() to today.toString()
        All -> null to null
    }
}

data class AnalysisUiState(
    val isLoadingAnalysis: Boolean = false,
    val isLoadingInsight: Boolean = false,
    val period: AnalysisPeriod = AnalysisPeriod.Last30Days,
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

    fun setPeriod(period: AnalysisPeriod) {
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
