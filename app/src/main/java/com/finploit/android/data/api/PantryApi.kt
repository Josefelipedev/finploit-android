package com.finploit.android.data.api

import com.finploit.android.data.dto.PantryItemDto
import com.finploit.android.data.dto.UpsertPantryItemRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PantryApi {
    @GET("pantry")
    suspend fun getAll(): List<PantryItemDto>

    @POST("pantry")
    suspend fun upsert(@Body req: UpsertPantryItemRequest): PantryItemDto

    @DELETE("pantry/{id}")
    suspend fun remove(@Path("id") id: Int)

    @DELETE("pantry")
    suspend fun clear()
}
