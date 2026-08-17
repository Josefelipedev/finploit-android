package com.finploit.android.data.dto

data class RecurringTransactionDto(
    val id: Int,
    val description: String?,
    val amount: Double,
    val currency: String? = null,
    val type: String,
    val frequency: String,
    val dueDay: Int?,
    /** N-ésimo dia útil do mês; manda sobre o `dueDay`. */
    val businessDay: Int? = null,
    val weekDay: Int?,
    val notification: Boolean?,
    val startDate: String? = null,
    val endDate: String?,
    val occurrences: Int?,
    val userId: Int?,
    val categoryId: Int? = null,
    /**
     * Conta bancária de onde sai (ou onde entra). As contas geradas herdam-na,
     * e é dela que sai a previsão "o que fica na conta no fim do mês".
     */
    val accountId: Int? = null,
    /** Total contratado gravado (nulo nas recorrentes anteriores à coluna). */
    val totalAmount: Double? = null,
    /** Total já resolvido pelo servidor: `totalAmount` ou parcela × parcelas. */
    val contractedTotal: Double? = null,
    /** Quantas ocorrências já foram pagas. */
    val executedCount: Int? = null,
    /** Somatório do que foi mesmo pago (não é parcela × pagamentos). */
    val paidTotal: Double? = null,
)

/** O que a quitação ("paguei tudo") liquidou de facto. */
data class SettleRecurringResultDto(
    val recurring: RecurringTransactionDto? = null,
    /** Valor liquidado, na moeda da recorrente. */
    val settledAmount: Double = 0.0,
    val currency: String? = null,
    /** Lançamento único criado no razão. */
    val financeId: Int? = null,
    val billOccurrenceId: Int? = null,
)

data class CreateRecurringRequest(
    val description: String,
    val amount: Double,
    val currency: String? = null,
    val type: String,
    val frequency: String,
    val dueDay: Int?,
    val businessDay: Int? = null,
    val weekDay: Int,
    val notification: Boolean,
    val categoryId: Int,
    /** Conta bancária de origem/destino; nulo = não foi dito. */
    val accountId: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val occurrences: Int,
    /** Quando vem, manda: o servidor reparte-o pelas parcelas. */
    val totalAmount: Double? = null,
)
