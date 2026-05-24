package com.finploit.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.api.UnauthorizedEventBus
import com.finploit.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val unauthorizedEventBus: UnauthorizedEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())

    init {
        // React to 401 responses from any API call: force logout
        viewModelScope.launch {
            unauthorizedEventBus.events.collect {
                isLoggedIn.value = false
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            authRepository.login(email, password)
                .onSuccess {
                    isLoggedIn.value = true
                    _uiState.value = LoginUiState(success = true)
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState(error = e.message ?: "Erro ao fazer login")
                }
        }
    }

    fun loginWithGoogle(idToken: String, email: String, name: String?, picture: String?) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            authRepository.googleLogin(idToken, email, name, picture)
                .onSuccess {
                    isLoggedIn.value = true
                    _uiState.value = LoginUiState(success = true)
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState(error = e.message ?: "Erro ao entrar com Google")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
