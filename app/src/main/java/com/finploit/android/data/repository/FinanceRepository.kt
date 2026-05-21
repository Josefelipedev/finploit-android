package com.finploit.android.data.repository

import com.finploit.android.data.api.FinanceApi
import com.finploit.android.data.dto.CreateFinanceRequest
import com.finploit.android.data.dto.DashboardResponse
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.dto.FinanceSummaryResponse
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val api: FinanceApi,
) {
    suspend fun getDashboard(startDate: String? = null, endDate: String? = null): Result<DashboardResponse> =
        runCatching { api.getDashboard(startDate, endDate) }

    suspend fun getTransactions(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Result<List<FinanceItemDto>> =
        runCatching { api.getTransactions(startDate, endDate, page, limit).data }

    suspend fun getSummary(startDate: String? = null, endDate: String? = null): Result<FinanceSummaryResponse> =
        runCatching { api.getSummary(startDate, endDate) }

    suspend fun createTransaction(
        type: String,
        amount: Double,
        description: String?,
        categoryId: Int?,
    ): Result<FinanceItemDto> = runCatching {
        api.createTransaction(
            CreateFinanceRequest(
                type = type,
                amount = amount,
                description = description,
                categoryId = categoryId,
            )
        )
    }

    suspend fun updateTransaction(
        id: Int,
        type: String,
        amount: Double,
        description: String?,
    ): Result<FinanceItemDto> = runCatching {
        val body = buildMap<String, Any> {
            put("type", type)
            put("amount", amount)
            description?.let { put("description", it) }
        }
        api.updateTransaction(id, body)
    }

    suspend fun deleteTransaction(id: Int): Result<Unit> = runCatching {
        val response = api.deleteTransaction(id)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}
