package com.finploit.android.data.api

import com.finploit.android.data.dto.CreateFinanceRequest
import com.finploit.android.data.dto.DashboardResponse
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.dto.FinanceListResponse
import com.finploit.android.data.dto.FinanceSummaryResponse
import com.finploit.android.data.dto.MonthForecastDto
import com.finploit.android.data.dto.UpdateFinanceRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanceApi {
    @GET("finance/dashboard")
    suspend fun getDashboard(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): DashboardResponse

    @GET("finance")
    suspend fun getTransactions(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): FinanceListResponse

    /** Mês corrente: realizado + o que falta pagar/receber até ao fim. */
    @GET("finance/forecast")
    suspend fun getMonthForecast(): MonthForecastDto

    @GET("finance/summary")
    suspend fun getSummary(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): FinanceSummaryResponse

    @POST("finance")
    suspend fun createTransaction(@Body request: CreateFinanceRequest): FinanceItemDto

    // A API expõe PATCH; o PUT que estava aqui devolvia 404 e o ecrã de edição
    // falhava sempre.
    @PATCH("finance/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Int,
        @Body body: UpdateFinanceRequest,
    ): FinanceItemDto

    @DELETE("finance/{id}")
    suspend fun deleteTransaction(@Path("id") id: Int): Response<Unit>
}
