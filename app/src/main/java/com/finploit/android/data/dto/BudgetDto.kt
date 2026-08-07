package com.finploit.android.data.dto

/** Um limite de orçamento vindo do servidor (C1). */
data class BudgetLimitDto(
    val id: Int,
    val categoryId: Int,
    val categoryName: String?,
    val monthlyLimit: Double,
    val alertAt: Int = 80,
)

data class SetBudgetLimitRequest(
    val monthlyLimit: Double,
    val alertAt: Int = 80,
)
