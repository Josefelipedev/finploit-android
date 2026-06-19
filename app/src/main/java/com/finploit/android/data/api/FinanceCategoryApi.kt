package com.finploit.android.data.api

import com.finploit.android.data.dto.FinanceCategoryDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FinanceCategoryApi {
    @GET("finance-category")
    suspend fun getCategories(
        @Query("active") active: Boolean = true,
    ): List<FinanceCategoryDto>
}
