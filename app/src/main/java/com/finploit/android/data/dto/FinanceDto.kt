package com.finploit.android.data.dto

data class FinanceCategoryDto(
    val id: Int,
    val name: String,
    val color: String? = null,
    val iconName: String? = null,
    val isActive: Boolean = true,
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
)

data class TransactionDto(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val tag: String,
    val amount: Double,
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

data class StatsDto(
    val revenueLastWeek: Double,
    val expenseLastWeek: Double = 0.0,
    val foodLastWeek: Double,
)

data class FinanceSummaryResponse(
    val totalGanhos: Double,
    val totalDespesas: Double,
    val saldo: Double,
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
)

data class FinanceItemDto(
    val id: Int,
    val type: String?,
    val description: String?,
    val amount: Double?,
    val iconName: String?,
    val createdAt: String,
    val categoryId: Int?,
)
