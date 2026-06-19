package com.finploit.android.data.repository

import com.finploit.android.data.api.FinanceCategoryApi
import com.finploit.android.data.dto.FinanceCategoryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceCategoryRepository @Inject constructor(
    private val api: FinanceCategoryApi,
) {
    suspend fun getCategories(): Result<List<FinanceCategoryDto>> =
        runCatching { api.getCategories(active = true) }
}
