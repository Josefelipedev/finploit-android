package com.finploit.android.data.repository

import com.finploit.android.data.api.FinanceApi
import com.finploit.android.data.dto.CreateFinanceRequest
import com.finploit.android.data.dto.DashboardResponse
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.dto.FinanceListResponse
import com.finploit.android.data.dto.FinanceSummaryResponse
import com.finploit.android.data.dto.MonthForecastDto
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
    ): Result<List<FinanceItemDto>> = getTransactionsPage(startDate, endDate, page, limit).map { it.data }

    /**
     * Como [getTransactions], mas devolve também o `meta` — que traz a moeda de
     * exibição e as moedas que a API não conseguiu converter. Quem soma
     * lançamentos precisa disso para avisar que o total é aproximado.
     */
    suspend fun getTransactionsPage(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Result<FinanceListResponse> = runCatching {
        val response = api.getTransactions(startDate, endDate, page, limit)
        if (page == 1) {
            cacheDao.insertAll(response.data.map { it.toCacheEntity() })
            cacheDao.clearOld(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        }
        response
    }

    /**
     * A lista **toda** do período, percorrendo as páginas.
     *
     * Quem soma lançamentos pedia uma página só e escolhia um `limit` grande a
     * pensar que chegava — mas a API limita a 200 (`Math.min(limit, 200)`), sem
     * dizer nada. Num período com mais de 200 lançamentos, esses ecrãs
     * mostravam **menos dinheiro do que existe**, enquanto os cartões somados no
     * servidor davam o total certo: o mesmo ecrã a contradizer-se, com o número
     * mais baixo a parecer o inofensivo.
     *
     * O `meta.totalPages` já vinha na resposta e era ignorado. Vai-se buscar
     * página a página, com o limite máximo que o servidor aceita para fazer o
     * menor número de pedidos, e com um teto de 50 páginas como rede.
     *
     * É a mesma correção que a web fez em `getAllFinances` (`useFinance.ts`).
     */
    suspend fun getAllTransactions(
        startDate: String? = null,
        endDate: String? = null,
    ): Result<FinanceListResponse> = runCatching {
        val first = api.getTransactions(startDate, endDate, page = 1, limit = MAX_PAGE_SIZE)
        cacheDao.insertAll(first.data.map { it.toCacheEntity() })
        cacheDao.clearOld(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)

        val pages = minOf(first.meta.totalPages, MAX_PAGES)
        if (pages <= 1) return@runCatching first

        val all = first.data.toMutableList()
        for (page in 2..pages) {
            all += api.getTransactions(startDate, endDate, page = page, limit = MAX_PAGE_SIZE).data
        }
        first.copy(data = all)
    }

    val cachedTransactions: Flow<List<FinanceItemDto>> =
        cacheDao.getRecent().map { entities -> entities.map { it.toDto() } }

    suspend fun getSummary(startDate: String? = null, endDate: String? = null): Result<FinanceSummaryResponse> =
        runCatching { api.getSummary(startDate, endDate) }

    suspend fun getMonthForecast(): Result<MonthForecastDto> =
        runCatching { api.getMonthForecast() }

    suspend fun createTransaction(
        type: String,
        amount: Double,
        description: String?,
        categoryId: Int?,
        referenceDate: String? = null,
        accountId: Int? = null,
    ): Result<FinanceItemDto> = runCatching {
        api.createTransaction(
            CreateFinanceRequest(
                type = type,
                amount = amount,
                description = description,
                categoryId = categoryId,
                referenceDate = referenceDate,
                accountId = accountId,
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

    private companion object {
        /** O teto que a API aplica ao `limit`; pedir mais não traz mais. */
        const val MAX_PAGE_SIZE = 200

        /** Rede de segurança para não percorrer um histórico sem fim. */
        const val MAX_PAGES = 50
    }
}
