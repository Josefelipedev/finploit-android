package com.finploit.android.ui.couple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CoupleInviteDto
import com.finploit.android.data.dto.CoupleProfileDto
import com.finploit.android.data.repository.CoupleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoupleUiState(
    val isLoading: Boolean = true,
    val profile: CoupleProfileDto? = null,
    val receivedInvite: CoupleInviteDto? = null,
    val sentInvite: CoupleInviteDto? = null,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val isMarried: Boolean get() = profile?.isMarried == true && profile.spouseId != null
}

@HiltViewModel
class CoupleViewModel @Inject constructor(
    private val repository: CoupleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoupleUiState())
    val uiState: StateFlow<CoupleUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profile = repository.getProfile().getOrNull()
            val invites = repository.listInvites().getOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                profile = profile,
                receivedInvite = invites?.received?.firstOrNull(),
                sentInvite = invites?.sent?.firstOrNull(),
                error = if (profile == null) "Não foi possível carregar o perfil." else null,
            )
        }
    }

    fun sendInvite(phone: String) = action {
        val result = repository.createInvite(phone).getOrThrow()
        result.message
    }

    fun accept(id: Int) = action { repository.acceptInvite(id).getOrThrow() }

    fun reject(id: Int) = action { repository.rejectInvite(id).getOrThrow() }

    fun cancel(id: Int) = action { repository.cancelInvite(id).getOrThrow() }

    fun unlink() = action { repository.unlink().getOrThrow() }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun action(block: suspend () -> String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                val message = block()
                _uiState.value = _uiState.value.copy(isSubmitting = false, message = message)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Algo deu errado. Tente novamente.",
                )
            }
        }
    }
}
