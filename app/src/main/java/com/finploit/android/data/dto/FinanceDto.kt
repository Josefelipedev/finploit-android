package com.finploit.android.data.dto

data class FinanceCategoryDto(
    val id: Int,
    val name: String,
    val color: String? = null,
    val iconName: String? = null,
    val isActive: Boolean = true,
    /**
     * Receitas desta categoria contam como faturação da atividade (C6) — é
     * isto que o módulo fiscal soma para o limiar do art. 53.º.
     */
    val isBusinessIncome: Boolean = false,
)

data class UpdateFinanceRequest(
    val type: String,
    val amount: Double,
    val description: String? = null,
    val referenceDate: String? = null,
    val categoryId: Int? = null,
)

data class CreateFinanceRequest(
    val type: String,
    val amount: Double,
    val description: String? = null,
    val iconName: String? = null,
    val referenceDate: String? = null,
    val categoryId: Int? = null,
    val accountId: Int? = null,
)

data class TransactionDto(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val tag: String,
    /** Valor como foi lançado; a moeda é a de `currency`, não a do utilizador. */
    val amount: Double,
    val currency: String? = null,
    val category: String? = null,
    val categoryIcon: String? = null,
    val createdAt: String? = null,
)

data class DashboardResponse(
    val totalBalance: Double,
    val totalIncome: Double = 0.0,
    val totalExpense: Double,
    val stats: StatsDto,
    val transactions: List<TransactionDto>,
)

/**
 * "O que me sobra depois de pagar o que falta este mês?" — `GET /finance/forecast`.
 *
 * É **sempre o mês corrente**, de propósito: misturar um intervalo arbitrário
 * com "o que falta pagar até ao fim do mês" daria um número que não responde a
 * pergunta nenhuma. Vem somado do servidor, do mesmo motor que o resumo do
 * WhatsApp usa, com as moedas convertidas antes de somar.
 */
data class MonthForecastDto(
    val month: String,
    val realized: ForecastAmountsDto,
    val pending: ForecastPendingDto,
    val projectedBalance: Double,
    val displayCurrency: String? = null,
)

data class ForecastAmountsDto(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
)

data class ForecastPendingDto(
    val income: Double = 0.0,
    val expense: Double = 0.0,
)

data class StatsDto(
    val revenueLastWeek: Double,
    val expenseLastWeek: Double = 0.0,
    val foodLastWeek: Double,
)

data class FinanceSummaryResponse(
    val totalGanhos: Double,
    val totalDespesas: Double,
    val saldo: Double,
    // Multi-moeda: totais acima vêm convertidos para a moeda de exibição do usuário
    val displayCurrency: String? = null,
    val rateDate: String? = null,
    val byCurrency: List<CurrencyBreakdownDto>? = null,
    // Já convertido pelo servidor: é o que os ecrãs devem mostrar. Somar por
    // categoria no cliente dava a soma crua de moedas diferentes.
    val byCategory: List<CategoryBreakdownDto>? = null,
    val transactionCount: Int = 0,
    /** Moedas que entraram no total sem conversão (sem taxa disponível). */
    val unconvertedCurrencies: List<String>? = null,
)

data class CurrencyBreakdownDto(
    val currency: String,
    val ganhos: Double,
    val despesas: Double,
)

data class CategoryBreakdownDto(
    val categoryId: Int? = null,
    val categoryName: String,
    val iconName: String? = null,
    val ganhos: Double = 0.0,
    val despesas: Double = 0.0,
)

data class FinanceListResponse(
    val data: List<FinanceItemDto>,
    val meta: FinanceMetaDto,
)

data class FinanceMetaDto(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val displayCurrency: String? = null,
    val rateDate: String? = null,
    val unconvertedCurrencies: List<String>? = null,
)

data class FinanceItemDto(
    val id: Int,
    val type: String?,
    val description: String?,
    /** Valor como foi lançado, na moeda de `currency`. Nunca somar entre moedas. */
    val amount: Double?,
    val iconName: String?,
    val createdAt: String,
    val categoryId: Int?,
    /** Moeda do lançamento; ausente só em registos vindos do cache antigo. */
    val currency: String? = null,
    /** Dia em que o movimento aconteceu — é por ele que a API filtra o período. */
    val referenceDate: String? = null,
    /** O mesmo valor na moeda de exibição do utilizador. É este que se soma. */
    val convertedAmount: Double? = null,
    /** Quem lançou. No workspace do casal a lista mistura os dois. */
    val userId: Int? = null,
    /** Conta a pagar/receber que este lançamento quita; null = lançamento solto. */
    val billOccurrenceId: Int? = null,
    /** A mesma conta já resolvida pela API (descrição e vencimento). */
    val bill: LinkedBillDto? = null,
    /** De onde veio este lançamento; null = escrito à mão. */
    val origin: FinanceOriginDto? = null,
) {
    /** Data do movimento (recuo para a de criação, como a API faz). */
    val movementDate: String get() = (referenceDate ?: createdAt).take(10)

    /** Valor somável: o convertido quando a API o mandou, senão o original. */
    val amountForTotals: Double get() = convertedAmount ?: amount ?: 0.0
}

/**
 * De onde veio um lançamento que a app criou sozinha (T6.6).
 *
 * Cinco módulos emitem lançamentos — contas a pagar, metas, listas de compras,
 * cardápio, e a mão do utilizador — e na lista eram todos linhas iguais. Sem
 * isto não há como ver que a mesma compra foi contada duas vezes, uma pela
 * lista fechada e outra à mão. A app não adivinha duplicados: mostra a origem.
 */
data class FinanceOriginDto(
    /** `bill` | `goal` | `shopping` | `meal`. */
    val kind: String,
    val label: String,
    val refId: Int,
) {
    val chipLabel: String
        get() = when (kind) {
            "goal" -> "Meta · $label"
            "shopping" -> "Compras · $label"
            "meal" -> "Cardápio"
            else -> label
        }
}

/**
 * A conta que um lançamento quita (B5).
 *
 * Vem resolvida do servidor: o vínculo é por id solto nos dois sentidos, sem
 * relação no schema, por isso o cliente não tem como ir buscar a descrição e o
 * vencimento sozinho. Um registo servido do cache offline não a traz — nesse
 * caso o crachá não aparece, em vez de se adivinhar.
 */
data class LinkedBillDto(
    val id: Int,
    val description: String,
    val dueDate: String,
    val status: String? = null,
    val recurringId: Int? = null,
) {
    /** `2026-08-10T00:00:00.000Z` → `10/08`. */
    val dueLabel: String
        get() {
            val partes = dueDate.take(10).split("-")
            return if (partes.size == 3) "${partes[2]}/${partes[1]}" else dueDate
        }
}
