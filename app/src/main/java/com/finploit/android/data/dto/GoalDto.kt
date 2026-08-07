package com.finploit.android.data.dto

data class GoalDto(
    val id: Int,
    val name: String,
    val targetValue: Double,
    val currentValue: Double?,
    val description: String?,
    val status: String?,
    val startDate: String?,
    val endDate: String?,
    val userId: Int?,
    /** Ritmo pretendido e ordem de prioridade (planeamento de longo prazo). */
    val monthlyContribution: Double? = null,
    val priority: Int? = null,
)

/**
 * Só o ritmo e a prioridade. O `PUT /goals/:id` aceita um parcial de propósito
 * (o DTO do servidor é `PartialType`), portanto reenviar nome e alvo aqui só
 * arriscaria sobrescrevê-los com o que o ecrã tivesse em mão.
 */
data class UpdateGoalPaceRequest(
    val monthlyContribution: Double,
    val priority: Int,
)

data class CreateGoalRequest(
    val name: String,
    val targetValue: Double,
    val currentValue: Double? = null,
    val description: String? = null,
    val status: String? = "ACTIVE",
    val startDate: String? = null,
    val endDate: String? = null,
)

/**
 * Um depósito numa meta (C2).
 *
 * `ledger = false` grava o depósito sem lançamento — para quem move dinheiro
 * entre contas suas e não quer que isso conte como gasto.
 */
data class DepositRequest(
    val amount: Double,
    val ledger: Boolean = true,
)

data class GoalDepositDto(
    val id: Int,
    val goalId: Int,
    val amount: Double,
    val currency: String?,
    val financeId: Int?,
    val financeAutoCreated: Boolean = true,
    val createdAt: String?,
)

/** O que o servidor devolve ao depositar: a meta já atualizada e o depósito. */
data class DepositResultDto(
    val goal: GoalDto,
    val deposit: GoalDepositDto,
)
