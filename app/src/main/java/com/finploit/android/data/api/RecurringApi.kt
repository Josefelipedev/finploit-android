package com.finploit.android.data.api

import com.finploit.android.data.dto.CreateRecurringRequest
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.data.dto.SettleRecurringResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RecurringApi {
    @GET("recurring-finance")
    suspend fun getAll(): List<RecurringTransactionDto>

    @GET("recurring-finance/by-id/{id}")
    suspend fun getById(@Path("id") id: Int): RecurringTransactionDto

    @POST("recurring-finance")
    suspend fun create(@Body request: CreateRecurringRequest): RecurringTransactionDto

    @PUT("recurring-finance/{id}")
    suspend fun update(
        @Path("id") id: Int,
        @Body request: CreateRecurringRequest,
    ): RecurringTransactionDto

    /**
     * "Paguei tudo": liquida de uma vez o que falta do parcelamento — uma conta
     * paga com a data de hoje e um único lançamento no razão.
     */
    @POST("recurring-finance/{id}/settle")
    suspend fun settle(@Path("id") id: Int): SettleRecurringResultDto

    @DELETE("recurring-finance/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Void>
}
