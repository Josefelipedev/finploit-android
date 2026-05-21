package com.finploit.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _recurringDueCount = MutableStateFlow(0)
    val recurringDueCount: StateFlow<Int> = _recurringDueCount.asStateFlow()

    val currencyCode: StateFlow<String> = preferencesRepository.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BRL")

    init { refreshDueCount() }

    fun refreshDueCount() {
        viewModelScope.launch {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val maxDay = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
            val tomorrow = if (today >= maxDay) 1 else today + 1
            recurringRepository.getAll().onSuccess { list ->
                _recurringDueCount.value = list.count {
                    it.dueDay == today || it.dueDay == tomorrow
                }
            }
        }
    }
}
