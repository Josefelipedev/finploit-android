package com.finploit.android.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.BillsForecastDto
import com.finploit.android.data.dto.CreateBillRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.UpdateBillRequest
import com.finploit.android.data.repository.BankAccountRepository
import com.finploit.android.data.repository.BillsRepository
import com.finploit.android.data.repository.CoupleRepository
import com.finploit.android.data.repository.FinanceCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** Estado do filtro por estado da conta. */
enum class BillStatusFilter(val label: String) {
    All("Pendentes e pagas"), Pending("Só pendentes"), Paid("Só pagas")
}

/** A pagar, a receber, ou os dois. */
enum class BillTypeFilter(val label: String) {
    All("A pagar e a receber"), Expense("Só a pagar"), Income("Só a receber")
}

/** De quem são as contas — só aparece num workspace de casal. */
enum class BillOwnerFilter(val label: String) {
    All("Do casal"), Mine("Só minhas")
}

/**
 * Os filtros da lista do mês.
 *
 * Com o backfill a materializar meses antigos, uma lista passa a ter linhas em
 * que metade são atrasadas de meses anteriores — e o único filtro que havia era
 * o mês. São todos no cliente: a lista do mês já vem toda, e filtrar no servidor
 * obrigava a ir buscá-la outra vez a cada toque. É o mesmo conjunto que a web
 * tem (`BillsPage`).
 */
data class BillFilters(
    val showCarriedOver: Boolean = true,
    val status: BillStatusFilter = BillStatusFilter.All,
    val type: BillTypeFilter = BillTypeFilter.All,
    /** `null` = todas; senão o nome da categoria (ou "Sem categoria"). */
    val category: String? = null,
    val owner: BillOwnerFilter = BillOwnerFilter.All,
) {
    val isActive: Boolean
        get() = !showCarriedOver ||
            status != BillStatusFilter.All ||
            type != BillTypeFilter.All ||
            category != null ||
            owner != BillOwnerFilter.All
}

data class BillsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val month: String = YearMonth.now().toString(),
    val items: List<BillItemDto> = emptyList(),
    val filters: BillFilters = BillFilters(),
    /** Id de quem está autenticado, para o filtro "só minhas". */
    val myUserId: Int? = null,
    val totalPending: Double = 0.0,
    val totalPaid: Double = 0.0,
    val expensePending: Double = 0.0,
    val expensePaid: Double = 0.0,
    val incomePending: Double = 0.0,
    val incomePaid: Double = 0.0,
    val projectedBalance: Double = 0.0,
    val realizedBalance: Double = 0.0,
    /**
     * O que fica em cada conta bancária depois de pagar o que falta. Nulo num
     * mês já fechado — a previsão parte do saldo de hoje.
     */
    val accountsForecast: BillsForecastDto? = null,
    /**
     * As contas bancárias do casal, para o seletor do formulário e para dizer
     * o nome do banco em cada linha. Vêm daqui (e não da previsão) para o nome
     * continuar a aparecer num mês fechado, onde não há previsão.
     */
    val bankAccounts: List<BankAccountDto> = emptyList(),
    val categories: List<FinanceCategoryDto> = emptyList(),
    val error: String? = null,
    /**
     * A leitura do mês falhou. Fica separado do `error` porque o `error` é
     * consumido por um snackbar que desaparece sozinho: sem esta marca, o que
     * sobrava no ecrã eram quatro totais a 0,00 e um "nenhuma conta este mês"
     * — indistinguível de um mês realmente vazio, quando o que aconteceu foi
     * não se ter conseguido perguntar (T9).
     */
    val loadFailed: Boolean = false,
) {
    /** O nome do banco de uma conta, quando ela diz de onde sai. */
    fun accountName(accountId: Int?): String? =
        accountId?.let { id -> bankAccounts.find { it.id == id }?.bankName }

    val hasBankAccounts: Boolean get() = bankAccounts.isNotEmpty()

    /** O que passa nos filtros. Os totais em cima continuam a ser os do mês inteiro. */
    val visibleItems: List<BillItemDto>
        get() = items.filter { item ->
            when {
                !filters.showCarriedOver && item.carriedOver -> false
                filters.status == BillStatusFilter.Pending && item.isPaid -> false
                filters.status == BillStatusFilter.Paid && !item.isPaid -> false
                filters.type == BillTypeFilter.Expense && item.isIncome -> false
                filters.type == BillTypeFilter.Income && !item.isIncome -> false
                filters.category != null && item.categoryLabel != filters.category -> false
                filters.owner == BillOwnerFilter.Mine &&
                    myUserId != null && item.userId != myUserId -> false
                else -> true
            }
        }

    /** As categorias que existem no mês, para a lista do filtro. */
    val categoriesInMonth: List<String>
        get() = items.map { it.categoryLabel }.distinct().sorted()

    val carriedOverCount: Int get() = items.count { it.carriedOver }

    /** Só se mostra o filtro de pessoa quando há contas de mais do que uma. */
    val hasMultipleOwners: Boolean
        get() = items.mapNotNull { it.userId }.distinct().size > 1

    /**
     * Subtotal do que está à vista — só quando todas as linhas filtradas estão
     * na mesma moeda. Somar 100 BRL com 100 EUR e escrever 200 é o erro que os
     * totais do servidor existem para evitar, e aqui não há taxas à mão.
     */
    val visibleTotal: Pair<Double, String>?
        get() {
            val visible = visibleItems
            if (visible.isEmpty()) return null
            val currency = visible.first().currency
            if (visible.any { it.currency != currency }) return null
            return visible.sumOf { if (it.isPaid) it.paidAmount ?: it.amount else it.amount } to currency
        }
}

/** O rótulo por que se agrupa e filtra; a conta sem categoria também é um grupo. */
private val BillItemDto.categoryLabel: String
    get() = categoryName?.takeIf { it.isNotBlank() } ?: "Sem categoria"

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val repository: BillsRepository,
    private val categoryRepository: FinanceCategoryRepository,
    private val coupleRepository: CoupleRepository,
    private val bankAccountRepository: BankAccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState

    init {
        load(_uiState.value.month)
        loadCategories()
        loadMyUserId()
        loadBankAccounts()
    }

    /**
     * Falhar isto não estraga o ecrã: sem contas na lista, o seletor do
     * formulário fica escondido e as linhas não mostram o nome do banco — mas
     * a previsão que vem no GET /bills continua a aparecer, porque é calculada
     * no servidor.
     */
    private fun loadBankAccounts() {
        viewModelScope.launch {
            bankAccountRepository.getAll()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        bankAccounts = list.filterNot { it.isArchived },
                    )
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories(active = true)
                .onSuccess { list -> _uiState.value = _uiState.value.copy(categories = list) }
        }
    }

    /**
     * Falhar isto não estraga o ecrã: sem id, o filtro "só minhas" não aparece
     * (`hasMultipleOwners` continua a decidir) e a lista mostra tudo, que é o
     * que mostrava antes de haver filtros.
     */
    private fun loadMyUserId() {
        viewModelScope.launch {
            coupleRepository.getProfile()
                .onSuccess { profile -> _uiState.value = _uiState.value.copy(myUserId = profile.id) }
        }
    }

    fun setFilters(filters: BillFilters) {
        _uiState.value = _uiState.value.copy(filters = filters)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(filters = BillFilters())
    }

    fun load(month: String) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    month = month,
                    error = null,
                    loadFailed = false,
                )
            val result = repository.getBills(month)
            val data = result.getOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loadFailed = result.isFailure,
                month = data?.month?.takeIf { it.isNotBlank() } ?: month,
                items = data?.items ?: emptyList(),
                totalPending = data?.totalPending ?: 0.0,
                totalPaid = data?.totalPaid ?: 0.0,
                expensePending = data?.expense?.pending ?: 0.0,
                expensePaid = data?.expense?.paid ?: 0.0,
                incomePending = data?.income?.pending ?: 0.0,
                incomePaid = data?.income?.paid ?: 0.0,
                projectedBalance = data?.projectedBalance ?: 0.0,
                realizedBalance = data?.realizedBalance ?: 0.0,
                accountsForecast = data?.accounts,
                error = if (result.isFailure) "Não foi possível carregar as contas." else null,
            )
        }
    }

    fun prevMonth() {
        load(shiftMonth(-1))
    }

    fun nextMonth() {
        load(shiftMonth(1))
    }

    fun togglePaid(item: BillItemDto, amount: Double? = null) {
        viewModelScope.launch {
            val result = if (item.isPaid) repository.unpay(item.id) else repository.pay(item.id, amount)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Não foi possível atualizar a conta. Tente novamente.",
                )
            }
        }
    }

    fun createBill(
        description: String,
        amount: Double,
        dueDate: String,
        type: String,
        currency: String?,
        categoryId: Int?,
        accountId: Int?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = repository.createBill(
                CreateBillRequest(
                    description = description,
                    amount = amount,
                    dueDate = dueDate,
                    type = type,
                    currency = currency,
                    categoryId = categoryId,
                    accountId = accountId,
                ),
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(error = "Não foi possível criar a conta. Tente novamente.")
            }
        }
    }

    fun updateBill(
        id: Int,
        description: String?,
        amount: Double?,
        dueDate: String?,
        categoryId: Int?,
        accountId: Int?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = repository.updateBill(
                id,
                UpdateBillRequest(
                    description = description,
                    amount = amount,
                    dueDate = dueDate,
                    categoryId = categoryId,
                    accountId = accountId,
                ),
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(error = "Não foi possível editar a conta. Tente novamente.")
            }
        }
    }

    fun deleteBill(id: Int) {
        viewModelScope.launch {
            val result = repository.deleteBill(id)
            if (result.isSuccess) {
                load(_uiState.value.month)
            } else {
                _uiState.value = _uiState.value.copy(error = "Não foi possível excluir a conta. Tente novamente.")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun shiftMonth(delta: Long): String =
        runCatching { YearMonth.parse(_uiState.value.month).plusMonths(delta).toString() }
            .getOrElse { YearMonth.now().plusMonths(delta).toString() }
}
