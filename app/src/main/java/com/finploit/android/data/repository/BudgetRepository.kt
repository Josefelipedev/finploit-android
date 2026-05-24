package com.finploit.android.data.repository

import com.finploit.android.data.local.dao.BudgetLimitDao
import com.finploit.android.data.local.entity.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetLimitDao,
) {
    fun getAll(): Flow<List<BudgetLimitEntity>> = dao.getAll()

    suspend fun upsert(categoryId: Int, categoryName: String, monthlyLimit: Double, alertAt: Int = 80) {
        dao.upsert(BudgetLimitEntity(categoryId, categoryName, monthlyLimit, alertAt))
    }

    suspend fun delete(categoryId: Int) = dao.deleteById(categoryId)
}
