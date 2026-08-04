package com.finploit.android.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.PlanEventRequest
import com.finploit.android.data.dto.PlanningOverviewDto
import com.finploit.android.data.dto.ScenarioRequest
import com.finploit.android.data.dto.YearPlanDto
import com.finploit.android.data.dto.YearPlanItemRequest
import com.finploit.android.data.repository.FinanceCategoryRepository
import com.finploit.android.data.repository.GoalRepository
import com.finploit.android.data.repository.PlanningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PlanningUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val overview: PlanningOverviewDto? = null,
    /** Cenário escolhido. Nulo = o de referência, que é o que o servidor escolhe. */
    val selectedScenarioId: Int? = null,
    val yearPlan: YearPlanDto? = null,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR) + 1,
    val isLoadingYear: Boolean = false,
    val categories: List<FinanceCategoryDto> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

/**
 * O planeamento dos próximos anos.
 *
 * Tudo o que se vê é derivado no servidor — projeção, ritmo das metas,
 * planeado vs real — e por isso qualquer gravação **recarrega a visão geral**:
 * mudar uma premissa muda a projeção, que muda o excedente, que muda o
 * veredicto das metas. Guardar o resultado localmente e corrigi-lo à mão daria
 * um ecrã que discorda de si próprio.
 */
@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val repository: PlanningRepository,
    private val goalRepository: GoalRepository,
    private val categoryRepository: FinanceCategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState: StateFlow<PlanningUiState> = _uiState

    init {
        refresh()
        loadCategories()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getOverview(_uiState.value.selectedScenarioId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                overview = result.getOrNull() ?: _uiState.value.overview,
                error = if (result.isFailure) "Não foi possível carregar o planeamento." else null,
            )
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val result = categoryRepository.getCategories()
            _uiState.value = _uiState.value.copy(categories = result.getOrNull() ?: emptyList())
        }
    }

    fun selectScenario(id: Int?) {
        if (_uiState.value.selectedScenarioId == id) return
        _uiState.value = _uiState.value.copy(selectedScenarioId = id)
        refresh()
    }

    fun loadYear(year: Int) {
        _uiState.value = _uiState.value.copy(year = year, isLoadingYear = true)
        viewModelScope.launch {
            val result = repository.getYearPlan(year)
            _uiState.value = _uiState.value.copy(
                isLoadingYear = false,
                yearPlan = result.getOrNull(),
                error = if (result.isFailure) "Não foi possível carregar o plano de $year." else null,
            )
        }
    }

    // ── Cenários ──────────────────────────────────────────────────────────

    fun saveScenario(id: Int?, request: ScenarioRequest) {
        runWrite(
            block = {
                if (id == null) repository.createScenario(request)
                else repository.updateScenario(id, request)
            },
            success = if (id == null) "Cenário criado." else "Cenário actualizado.",
            failure = "Não foi possível gravar o cenário.",
        )
    }

    fun deleteScenario(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.deleteScenario(id)
            // O cenário apagado não pode continuar a ser o escolhido, senão o
            // pedido seguinte pede um id que já não existe e volta 404.
            if (result.isSuccess && _uiState.value.selectedScenarioId == id) {
                _uiState.value = _uiState.value.copy(selectedScenarioId = null)
            }
            finishWrite(result.isSuccess, "Cenário apagado.", "Não foi possível apagar o cenário.")
        }
    }

    // ── Eventos "e se" ────────────────────────────────────────────────────

    fun saveEvent(scenarioId: Int, eventId: Int?, request: PlanEventRequest) {
        runWrite(
            block = {
                if (eventId == null) repository.createEvent(scenarioId, request)
                else repository.updateEvent(eventId, request)
            },
            success = if (eventId == null) "Alteração adicionada." else "Alteração actualizada.",
            failure = "Não foi possível gravar a alteração.",
        )
    }

    fun deleteEvent(id: Int) {
        runWrite(
            block = { repository.deleteEvent(id) },
            success = "Alteração removida.",
            failure = "Não foi possível remover a alteração.",
        )
    }

    // ── Metas ─────────────────────────────────────────────────────────────

    /**
     * O ritmo e a prioridade de uma meta vivem no próprio `Goal` — daí passar
     * pelo `GoalRepository` e não pelo de planeamento. O `PUT /goals/:id`
     * aceita um parcial, portanto não é preciso reenviar nome nem alvo.
     */
    fun saveGoalPace(goalId: Int, monthlyContribution: Double, priority: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = goalRepository.updateGoalPace(goalId, monthlyContribution, priority)
            finishWrite(result.isSuccess, "Ritmo actualizado.", "Não foi possível gravar o ritmo.")
        }
    }

    // ── Plano anual ───────────────────────────────────────────────────────

    fun saveYearPlanItem(categoryId: Int, type: String, plannedAmount: Double) {
        val year = _uiState.value.year
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.saveYearPlan(
                year,
                listOf(YearPlanItemRequest(categoryId, type, plannedAmount)),
            )
            _uiState.value = _uiState.value.copy(yearPlan = result.getOrNull() ?: _uiState.value.yearPlan)
            finishWrite(result.isSuccess, "Plano gravado.", "Não foi possível gravar o plano.")
        }
    }

    fun deleteYearPlanItem(categoryId: Int) {
        val year = _uiState.value.year
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = repository.deleteYearPlanItem(year, categoryId)
            if (result.isSuccess) loadYear(year)
            finishWrite(result.isSuccess, "Removido do plano.", "Não foi possível remover.")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    // ── Plumbing ──────────────────────────────────────────────────────────

    private fun <T> runWrite(
        block: suspend () -> Result<T>,
        success: String,
        failure: String,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = block()
            finishWrite(result.isSuccess, success, failure)
        }
    }

    /** Uma gravação só está feita quando a projeção que a reflecte volta a chegar. */
    private fun finishWrite(ok: Boolean, success: String, failure: String) {
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            message = if (ok) success else null,
            error = if (ok) null else failure,
        )
        if (ok) {
            refresh()
            if (_uiState.value.yearPlan != null) loadYear(_uiState.value.year)
        }
    }
}
