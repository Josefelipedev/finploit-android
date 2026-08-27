package com.finploit.android.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.repository.BankAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<BankAccountDto> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: BankAccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAll()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = list.filter { !it.isArchived },
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Não foi possível carregar as contas.",
                    )
                }
        }
    }

    fun createAccount(
        bankName: String,
        accountNumber: String?,
        agency: String?,
        balance: Double?,
        creditLimit: Double?,
        currency: String,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            repository.create(bankName, accountNumber, agency, balance, creditLimit, currency)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, message = "Conta criada!")
                    load()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Não foi possível criar a conta.",
                    )
                }
        }
    }

    fun updateAccount(
        id: Int,
        bankName: String?,
        accountNumber: String?,
        agency: String?,
        balance: Double?,
        creditLimit: Double?,
        currency: String?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            repository.update(id, bankName, accountNumber, agency, balance, creditLimit, currency)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, message = "Conta atualizada!")
                    load()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Não foi possível atualizar a conta.",
                    )
                }
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        message = "Conta arquivada.",
                        accounts = _uiState.value.accounts.filter { it.id != id },
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Não foi possível arquivar a conta.",
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
