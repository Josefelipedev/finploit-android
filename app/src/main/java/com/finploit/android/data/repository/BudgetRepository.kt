package com.finploit.android.data.repository

import com.finploit.android.data.api.BudgetApi
import com.finploit.android.data.dto.BudgetLimitDto
import com.finploit.android.data.dto.SetBudgetLimitRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Os limites vêm do servidor (C1).
 *
 * Estavam numa base Room só deste telemóvel, enquanto a web tinha os dela no
 * `localStorage`: dois conjuntos que divergiam em silêncio, que não sobreviviam
 * a trocar de aparelho e que o casal nunca via iguais. A tabela local saiu.
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val api: BudgetApi,
) {
    suspend fun getAll(): Result<List<BudgetLimitDto>> = runCatching { api.getAll() }

    suspend fun upsert(categoryId: Int, monthlyLimit: Double, alertAt: Int = 80): Result<BudgetLimitDto> =
        runCatching { api.set(categoryId, SetBudgetLimitRequest(monthlyLimit, alertAt)) }

    suspend fun delete(categoryId: Int): Result<Unit> = runCatching {
        val response = api.delete(categoryId)
        if (!response.isSuccessful) error("Falha ao remover o limite (${response.code()})")
    }
}
