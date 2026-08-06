package com.finploit.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.CoupleRepository
import com.finploit.android.data.repository.RecurringRepository
import com.finploit.android.ui.theme.OwnerNaming
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
    private val coupleRepository: CoupleRepository,
) : ViewModel() {

    private val _recurringDueCount = MutableStateFlow(0)
    val recurringDueCount: StateFlow<Int> = _recurringDueCount.asStateFlow()

    val currencyCode: StateFlow<String> = preferencesRepository.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BRL")

    /**
     * Os nomes do workspace, para dizer quem lançou cada registo. Carregado
     * aqui em cima e distribuído por `LocalOwnerNaming`: cada ecrã a pedir o
     * perfil por sua conta seriam quatro pedidos para a mesma resposta.
     */
    private val _ownerNaming = MutableStateFlow(OwnerNaming())
    val ownerNaming: StateFlow<OwnerNaming> = _ownerNaming.asStateFlow()

    init {
        refreshDueCount()
        loadOwnerNaming()
    }

    /** Falhar aqui é inofensivo: sem perfil, os chips de autoria não aparecem. */
    private fun loadOwnerNaming() {
        viewModelScope.launch {
            coupleRepository.getProfile().onSuccess { perfil ->
                _ownerNaming.value = OwnerNaming(
                    myId = perfil.id,
                    spouseId = perfil.spouseId?.takeIf { perfil.isMarried == true },
                    spouseName = perfil.spouse?.name,
                )
            }
        }
    }

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
