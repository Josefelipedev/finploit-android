package com.finploit.android.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.CreateRecurringRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.data.repository.BankAccountRepository
import com.finploit.android.data.repository.FinanceCategoryRepository
import com.finploit.android.data.repository.RecurringRepository
import com.finploit.android.ui.theme.currencyConfigByCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

/**
 * A explicação que o servidor deu para a recusa.
 *
 * O envelope de erro do Nest é `{ success: false, message: "..." }` e o
 * interceptor que desembrulha as respostas só trata das bem-sucedidas — sem
 * isto, um 400 com a razão escrita chegava ao ecrã como "HTTP 400".
 */
private fun serverMessage(e: Throwable): String? {
    val body = (e as? HttpException)?.response()?.errorBody()?.string() ?: return null
    return runCatching {
        val message = JSONObject(body).opt("message")
        when (message) {
            is String -> message.takeIf { it.isNotBlank() }
            is org.json.JSONArray -> (0 until message.length())
                .joinToString(". ") { message.optString(it) }
                .takeIf { it.isNotBlank() }
            else -> null
        }
    }.getOrNull()
}

data class RecurringUiState(
    val isLoading: Boolean = false,
    val transactions: List<RecurringTransactionDto> = emptyList(),
    val categories: List<FinanceCategoryDto> = emptyList(),
    /** Contas bancárias do casal, para o seletor "sai da conta" do formulário. */
    val bankAccounts: List<BankAccountDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val deletingIds: Set<Int> = emptySet(),
    /** Recorrentes a liquidar agora — o botão não pode ser tocado duas vezes. */
    val settlingIds: Set<Int> = emptySet(),
    /** Resultado da última quitação, para o ecrã o poder dizer. */
    val settleMessage: String? = null,
    val settleError: String? = null,
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: RecurringRepository,
    private val categoryRepository: FinanceCategoryRepository,
    private val bankAccountRepository: BankAccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState(isLoading = true))
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        loadCategories()
        loadBankAccounts()
    }

    /**
     * Falhar isto não estraga o formulário: sem contas, o seletor não aparece e
     * a recorrente é criada sem conta atribuída — como eram todas antes disto.
     */
    fun loadBankAccounts() {
        viewModelScope.launch {
            bankAccountRepository.getAll()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(bankAccounts = list.filterNot { it.isArchived })
                }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAll()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, transactions = it, error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories()
                .onSuccess { list -> _uiState.value = _uiState.value.copy(categories = list) }
        }
    }

    /**
     * Criar e editar mandam o MESMO corpo: o `PUT` do servidor é um
     * `PartialType` do `POST`, e enviar o formulário inteiro evita a
     * ambiguidade de "o que é que ficou por dizer".
     */
    private fun buildRequest(
        description: String,
        amount: Double,
        type: String,
        frequency: String,
        dueDay: Int?,
        businessDay: Int?,
        categoryId: Int,
        startDate: String?,
        endDate: String?,
        occurrences: Int,
        notification: Boolean,
        totalAmount: Double?,
        currency: String?,
        accountId: Int?,
    ) = CreateRecurringRequest(
        description = description,
        amount = amount,
        currency = currency,
        // O servidor aceita os dois; o Android sempre mandou em português.
        type = when (type) {
            "expense" -> "despesa"
            "income" -> "receita"
            else -> type
        },
        frequency = frequency,
        dueDay = dueDay,
        businessDay = businessDay,
        weekDay = 0,
        notification = notification,
        categoryId = categoryId,
        accountId = accountId,
        startDate = startDate,
        endDate = endDate,
        occurrences = occurrences,
        totalAmount = totalAmount,
    )

    fun update(
        id: Int,
        description: String,
        amount: Double,
        type: String,
        frequency: String,
        dueDay: Int?,
        businessDay: Int?,
        categoryId: Int,
        startDate: String?,
        endDate: String?,
        occurrences: Int,
        notification: Boolean = true,
        totalAmount: Double? = null,
        currency: String? = null,
        /** Conta bancária de onde sai (ou onde entra); as contas geradas herdam-na. */
        accountId: Int? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)
            repository.update(
                id,
                buildRequest(
                    description, amount, type, frequency, dueDay, businessDay,
                    categoryId, startDate, endDate, occurrences, notification,
                    totalAmount, currency, accountId,
                ),
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                    loadAll()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, saveError = it.message) }
        }
    }

    fun create(
        description: String,
        amount: Double,
        type: String,
        frequency: String,
        dueDay: Int?,
        businessDay: Int?,
        categoryId: Int,
        startDate: String?,
        endDate: String?,
        occurrences: Int,
        notification: Boolean = true,
        totalAmount: Double? = null,
        currency: String? = null,
        /** Conta bancária de onde sai (ou onde entra); as contas geradas herdam-na. */
        accountId: Int? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)
            repository.create(
                buildRequest(
                    description, amount, type, frequency, dueDay, businessDay,
                    categoryId, startDate, endDate, occurrences, notification,
                    totalAmount, currency, accountId,
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                    loadAll()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, saveError = it.message) }
        }
    }

    /**
     * "Paguei tudo": liquida de uma vez o que falta do parcelamento.
     *
     * O servidor apaga as parcelas por pagar, cria UMA conta paga com o valor em
     * falta (data de hoje) e o lançamento correspondente, e fecha a recorrente.
     * A lista é recarregada porque muda tudo o que ela mostra: o pago, o falta
     * pagar e o compromisso mensal.
     */
    fun settle(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                settlingIds = _uiState.value.settlingIds + id,
                settleMessage = null,
                settleError = null,
            )
            repository.settle(id)
                .onSuccess { result ->
                    val moeda = currencyConfigByCode(result.currency ?: "BRL")
                    val recebe = result.recurring?.type == "income"
                    _uiState.value = _uiState.value.copy(
                        settlingIds = _uiState.value.settlingIds - id,
                        settleMessage = (if (recebe) "Recebido: " else "Quitado: ") +
                            "${moeda.format(result.settledAmount)} lançados hoje.",
                    )
                    loadAll()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        settlingIds = _uiState.value.settlingIds - id,
                        // O servidor explica melhor ("já está quitada", "não tem
                        // fim definido") do que um "erro" que não diz o que fazer.
                        settleError = serverMessage(e) ?: "Não foi possível liquidar. Tente novamente.",
                    )
                }
        }
    }

    fun clearSettleFeedback() {
        _uiState.value = _uiState.value.copy(settleMessage = null, settleError = null)
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingIds = _uiState.value.deletingIds + id)
            repository.delete(id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        deletingIds = _uiState.value.deletingIds - id,
                        transactions = _uiState.value.transactions.filter { it.id != id },
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(deletingIds = _uiState.value.deletingIds - id)
                }
        }
    }

    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, saveError = null)
    }
}
