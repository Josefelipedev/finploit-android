package com.finploit.android.data.api

import com.finploit.android.data.dto.BudgetLimitDto
import com.finploit.android.data.dto.SetBudgetLimitRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Limites de orçamento (C1).
 *
 * Viviam numa base Room local — e no browser, noutro sítio. Eram dois conjuntos
 * que divergiam em silêncio e que não sobreviviam a trocar de telemóvel. A
 * chave é a categoria, tal como era a chave primária do Room.
 */
interface BudgetApi {
    @GET("budget")
    suspend fun getAll(): List<BudgetLimitDto>

    @PUT("budget/{categoryId}")
    suspend fun set(
        @Path("categoryId") categoryId: Int,
        @Body request: SetBudgetLimitRequest,
    ): BudgetLimitDto

    @DELETE("budget/{categoryId}")
    suspend fun delete(@Path("categoryId") categoryId: Int): Response<Unit>
}
