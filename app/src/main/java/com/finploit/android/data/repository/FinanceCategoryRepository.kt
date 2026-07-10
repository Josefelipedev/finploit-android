package com.finploit.android.data.repository

import com.finploit.android.data.api.FinanceCategoryApi
import com.finploit.android.data.dto.CreateCategoryRequest
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.UpdateCategoryRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceCategoryRepository @Inject constructor(
    private val api: FinanceCategoryApi,
) {
    suspend fun getCategories(active: Boolean = true): Result<List<FinanceCategoryDto>> =
        runCatching { api.getCategories(active = active) }

    suspend fun create(request: CreateCategoryRequest): Result<FinanceCategoryDto> =
        runCatching { api.create(request) }

    suspend fun update(id: Int, request: UpdateCategoryRequest): Result<FinanceCategoryDto> =
        runCatching { api.update(id, request) }

    suspend fun delete(id: Int): Result<Unit> =
        runCatching { api.delete(id) }

    suspend fun toggleStatus(id: Int): Result<FinanceCategoryDto> =
        runCatching { api.toggle(id) }
}
