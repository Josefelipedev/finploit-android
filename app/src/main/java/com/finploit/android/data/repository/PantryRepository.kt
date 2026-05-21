package com.finploit.android.data.repository

import com.finploit.android.data.api.PantryApi
import com.finploit.android.data.dto.PantryItemDto
import com.finploit.android.data.dto.UpsertPantryItemRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryRepository @Inject constructor(private val api: PantryApi) {
    suspend fun getAll(): Result<List<PantryItemDto>> = runCatching { api.getAll() }
    suspend fun upsert(req: UpsertPantryItemRequest): Result<PantryItemDto> = runCatching { api.upsert(req) }
    suspend fun update(id: Int, req: UpsertPantryItemRequest): Result<PantryItemDto> = runCatching { api.update(id, req) }
    suspend fun remove(id: Int): Result<Unit> = runCatching { api.remove(id) }
    suspend fun clear(): Result<Unit> = runCatching { api.clear() }
}
