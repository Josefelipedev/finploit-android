package com.finploit.android.data.dto

/** Um limite de orçamento vindo do servidor (C1). */
data class BudgetLimitDto(
    val id: Int,
    val categoryId: Int,
    val categoryName: String?,
    /** Já convertido pelo servidor para a moeda de quem está a ler (C4). */
    val monthlyLimit: Double,
    val alertAt: Int = 80,
    /** Moeda em que o `monthlyLimit` acima vem — a de quem lê. */
    val currency: String? = null,
    /** O que foi mesmo escrito, na moeda em que foi escrito. */
    val originalMonthlyLimit: Double? = null,
    val originalCurrency: String? = null,
)

data class SetBudgetLimitRequest(
    val monthlyLimit: Double,
    val alertAt: Int = 80,
)
