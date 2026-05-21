package com.finploit.android.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.data.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class UpcomingBill(
    val transaction: RecurringTransactionDto,
    val daysUntilDue: Int,
)

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState(isLoading = true)
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            recurringRepository.getAll()
                .onSuccess { list ->
                    val upcoming = list
                        .filter { it.dueDay != null && it.dueDay > 0 }
                        .mapNotNull { tx ->
                            val due = tx.dueDay!!
                            val days = when {
                                due == today -> 0
                                due == today + 1 -> 1
                                due > today -> due - today
                                else -> null
                            }
                            days?.let { UpcomingBill(tx, it) }
                        }
                        .sortedBy { it.daysUntilDue }
                    _uiState.value = NotificationsUiState(upcomingBills = upcoming)
                }
                .onFailure { _uiState.value = NotificationsUiState(error = it.message) }
        }
    }
}
