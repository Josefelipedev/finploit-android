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
    /** Total contratado gravado (nulo nas recorrentes anteriores à coluna). */
    val totalAmount: Double? = null,
    /** Total já resolvido pelo servidor: `totalAmount` ou parcela × parcelas. */
    val contractedTotal: Double? = null,
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
    val startDate: String? = null,
    val endDate: String? = null,
    val occurrences: Int,
    /** Quando vem, manda: o servidor reparte-o pelas parcelas. */
    val totalAmount: Double? = null,
)
