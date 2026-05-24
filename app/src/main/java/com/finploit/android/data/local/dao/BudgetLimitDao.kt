package com.finploit.android.data.local.dao

import androidx.room.*
import com.finploit.android.data.local.entity.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits")
    fun getAll(): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits WHERE categoryId = :categoryId")
    suspend fun getById(categoryId: Int): BudgetLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetLimitEntity)

    @Delete
    suspend fun delete(entity: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE categoryId = :categoryId")
    suspend fun deleteById(categoryId: Int)
}
