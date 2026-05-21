package com.finploit.android.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CreateGoalRequest
import com.finploit.android.data.dto.GoalDto
import com.finploit.android.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val isLoading: Boolean = false,
    val goals: List<GoalDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState(isLoading = true))
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init { loadGoals() }

    fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getGoals()
                .onSuccess { _uiState.value = GoalsUiState(goals = it) }
                .onFailure { _uiState.value = GoalsUiState(error = it.message) }
        }
    }

    fun createGoal(name: String, targetValue: Double, currentValue: Double?, description: String?, endDate: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)
            repository.createGoal(
                CreateGoalRequest(
                    name = name,
                    targetValue = targetValue,
                    currentValue = currentValue,
                    description = description?.ifBlank { null },
                    endDate = endDate?.ifBlank { null },
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                    loadGoals()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, saveError = it.message) }
        }
    }

    fun deleteGoal(id: Int) {
        viewModelScope.launch {
            repository.deleteGoal(id).onSuccess { loadGoals() }
        }
    }

    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, saveError = null)
    }
}
