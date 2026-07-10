package com.finploit.android.data.api

import com.finploit.android.data.dto.CreateCategoryRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.UpdateCategoryRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanceCategoryApi {
    @GET("finance-category")
    suspend fun getCategories(
        @Query("active") active: Boolean = true,
    ): List<FinanceCategoryDto>

    @POST("finance-category")
    suspend fun create(
        @Body body: CreateCategoryRequest,
    ): FinanceCategoryDto

    @PUT("finance-category/{id}")
    suspend fun update(
        @Path("id") id: Int,
        @Body body: UpdateCategoryRequest,
    ): FinanceCategoryDto

    @DELETE("finance-category/{id}")
    suspend fun delete(
        @Path("id") id: Int,
    )

    @POST("finance-category/{id}/toggle-status")
    suspend fun toggle(
        @Path("id") id: Int,
    ): FinanceCategoryDto
}
