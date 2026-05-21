package com.finploit.android.data.api

import com.finploit.android.data.dto.CreateRecurringRequest
import com.finploit.android.data.dto.RecurringTransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RecurringApi {
    @GET("recurring-finance")
    suspend fun getAll(): List<RecurringTransactionDto>

    @GET("recurring-finance/by-id/{id}")
    suspend fun getById(@Path("id") id: Int): RecurringTransactionDto

    @POST("recurring-finance")
    suspend fun create(@Body request: CreateRecurringRequest): RecurringTransactionDto

    @DELETE("recurring-finance/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Void>
}
