package com.finploit.android.data.api

import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.CreateBankAccountRequest
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface BankAccountApi {
    @GET("bank-accounts")
    suspend fun getAll(): List<BankAccountDto>

    @POST("bank-accounts")
    suspend fun create(@Body request: CreateBankAccountRequest): BankAccountDto

    @PATCH("bank-accounts/{id}")
    suspend fun update(
        @Path("id") id: Int,
        @Body request: JsonObject,
    ): BankAccountDto

    @DELETE("bank-accounts/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>
}
