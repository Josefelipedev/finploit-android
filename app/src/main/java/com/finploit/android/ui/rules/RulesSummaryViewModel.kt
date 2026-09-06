package com.finploit.android.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.RulesSummaryDto
import com.finploit.android.data.repository.RulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Só o resumo, para o cartão do Dashboard.
 *
 * O ecrã das regras pede tudo — categorias, histórico, presets. O cartão
 * precisa de quatro números, e fazê-lo pagar o pedido inteiro tornava a
 * primeira página mais lenta por causa de um cartão.
 *
 * Em falha fica **nulo e calado**: o cartão desaparece. Um erro em vermelho na
 * primeira página por causa de um cartão secundário custa mais atenção do que
 * vale — o ecrã das regras diz o que se passa a quem lá for.
 */
@HiltViewModel
class RulesSummaryViewModel @Inject constructor(
    private val repository: RulesRepository,
) : ViewModel() {

    private val _summary = MutableStateFlow<RulesSummaryDto?>(null)
    val summary: StateFlow<RulesSummaryDto?> = _summary

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.getSummary().onSuccess { _summary.value = it }
        }
    }
}
