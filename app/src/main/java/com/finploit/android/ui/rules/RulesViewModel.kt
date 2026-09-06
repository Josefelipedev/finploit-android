package com.finploit.android.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.CategoryBucketItem
import com.finploit.android.data.dto.RulesOverviewDto
import com.finploit.android.data.dto.SaveSplitRequest
import com.finploit.android.data.dto.UpsertRuleRequest
import com.finploit.android.data.repository.RulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RulesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val overview: RulesOverviewDto? = null,
    val error: String? = null,
)

/**
 * As regras do dinheiro.
 *
 * Todas as escritas devolvem a visão **inteira** já refeita pelo servidor, e
 * é ela que substitui o estado — não se corrige o objecto que mudou e se deixa
 * o resto como estava. Mudar um alvo muda o veredicto de todas as outras
 * regras, e remendar localmente daria um ecrã que discorda de si próprio.
 *
 * Em caso de falha o estado **não é limpo**: entre um ecrã em branco e os
 * números de há dez segundos com uma mensagem por cima, os números velhos são
 * mais úteis — e é a única maneira de se perceber que o que falhou foi a
 * gravação e não tudo.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repository: RulesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getOverview()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                overview = result.getOrNull() ?: _uiState.value.overview,
                error = if (result.isFailure) "Não foi possível carregar as regras." else null,
            )
        }
    }

    private fun write(erro: String, block: suspend () -> Result<RulesOverviewDto>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = block()
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                overview = result.getOrNull() ?: _uiState.value.overview,
                error = if (result.isFailure) erro else null,
            )
        }
    }

    fun choosePreset(key: String) =
        write("Não foi possível guardar a regra.") {
            repository.saveSplit(SaveSplitRequest(preset = key))
        }

    fun saveCustomSplit(needs: Int, wants: Int) =
        write("Não foi possível guardar a regra.") {
            repository.saveSplit(
                SaveSplitRequest(
                    preset = "custom",
                    needsPct = needs,
                    wantsPct = wants,
                    // A poupança é sempre o que sobra das outras duas: deixar
                    // escrever os três deixava gravar números que não somam
                    // 100, e o servidor recusava depois de já se ter arrastado.
                    savingsPct = 100 - needs - wants,
                ),
            )
        }

    /** `bucket` a null desfaz a escolha e devolve a categoria ao palpite. */
    fun setBucket(categoryId: Int, bucket: String?) =
        write("Não foi possível arrumar a categoria.") {
            repository.setCategoryBuckets(listOf(CategoryBucketItem(categoryId, bucket)))
        }

    /** Aceita todos os palpites de uma vez — o atalho de quem confia neles. */
    fun acceptGuesses() {
        val pendentes = _uiState.value.overview?.categories.orEmpty()
            .filter { it.source == "guess" && it.categoryId != null && it.bucket != null }
            .map { CategoryBucketItem(it.categoryId!!, it.bucket) }
        if (pendentes.isEmpty()) return
        write("Não foi possível aceitar os palpites.") {
            repository.setCategoryBuckets(pendentes)
        }
    }

    fun createRule(kind: String, target: Double, bucket: String?) =
        write("Não foi possível criar a regra.") {
            repository.createRule(
                UpsertRuleRequest(
                    kind = kind,
                    target = target,
                    // Só o tecto vigia um balde; mandá-lo nas outras famílias
                    // faz o servidor recusar, e bem.
                    bucket = if (kind == "ceiling") bucket else null,
                ),
            )
        }

    fun toggleRule(id: Int, isActive: Boolean) =
        write("Não foi possível mudar a regra.") {
            repository.updateRule(id, UpsertRuleRequest(isActive = isActive))
        }

    fun deleteRule(id: Int) =
        write("Não foi possível apagar a regra.") { repository.deleteRule(id) }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
