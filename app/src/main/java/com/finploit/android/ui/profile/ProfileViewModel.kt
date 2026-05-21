package com.finploit.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.UserDto
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currencyCode: StateFlow<String> = preferencesRepository.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BRL")

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getMe()
                .onSuccess { user -> _uiState.value = ProfileUiState(user = user) }
                .onFailure { e -> _uiState.value = ProfileUiState(error = e.message) }
        }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { preferencesRepository.setCurrencyCode(code) }
    }

    fun logout() = authRepository.logout()
}
