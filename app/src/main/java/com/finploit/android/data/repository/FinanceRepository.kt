package com.finploit.android.data.repository

import com.finploit.android.data.api.FinanceApi
import com.finploit.android.data.dto.CreateFinanceRequest
import com.finploit.android.data.dto.DashboardResponse
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.dto.FinanceSummaryResponse
import com.finploit.android.data.dto.UpdateFinanceRequest
import com.finploit.android.data.local.dao.TransactionCacheDao
import com.finploit.android.data.local.entity.TransactionCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val api: FinanceApi,
    private val cacheDao: TransactionCacheDao,
) {
    suspend fun getDashboard(startDate: String? = null, endDate: String? = null): Result<DashboardResponse> =
        runCatching { api.getDashboard(startDate, endDate) }

    suspend fun getTransactions(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Result<List<FinanceItemDto>> = runCatching {
        val list = api.getTransactions(startDate, endDate, page, limit).data
        if (page == 1) {
            cacheDao.insertAll(list.map { it.toCacheEntity() })
            cacheDao.clearOld(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        }
        list
    }

    val cachedTransactions: Flow<List<FinanceItemDto>> =
        cacheDao.getRecent().map { entities -> entities.map { it.toDto() } }

    suspend fun getSummary(startDate: String? = null, endDate: String? = null): Result<FinanceSummaryResponse> =
        runCatching { api.getSummary(startDate, endDate) }

    suspend fun createTransaction(
        type: String,
        amount: Double,
        description: String?,
        categoryId: Int?,
        referenceDate: String? = null,
    ): Result<FinanceItemDto> = runCatching {
        api.createTransaction(
            CreateFinanceRequest(
                type = type,
                amount = amount,
                description = description,
                categoryId = categoryId,
                referenceDate = referenceDate,
            )
        )
    }

    suspend fun updateTransaction(
        id: Int,
        type: String,
        amount: Double,
        description: String?,
        referenceDate: String? = null,
        categoryId: Int? = null,
    ): Result<FinanceItemDto> = runCatching {
        api.updateTransaction(id, UpdateFinanceRequest(type, amount, description, referenceDate, categoryId))
    }

    suspend fun deleteTransaction(id: Int): Result<Unit> = runCatching {
        val response = api.deleteTransaction(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private fun FinanceItemDto.toCacheEntity() = TransactionCacheEntity(
        id = id,
        type = type ?: "expense",
        amount = amount ?: 0.0,
        description = description,
        category = null,
        createdAt = createdAt,
    )

    private fun TransactionCacheEntity.toDto() = FinanceItemDto(
        id = id,
        type = type,
        amount = amount,
        description = description,
        iconName = null,
        createdAt = createdAt,
        categoryId = null,
    )
}
