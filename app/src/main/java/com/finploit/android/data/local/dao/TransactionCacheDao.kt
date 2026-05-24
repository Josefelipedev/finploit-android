package com.finploit.android.data.local.dao

import androidx.room.*
import com.finploit.android.data.local.entity.TransactionCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionCacheDao {
    @Query("SELECT * FROM transaction_cache ORDER BY createdAt DESC LIMIT 50")
    fun getRecent(): Flow<List<TransactionCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionCacheEntity>)

    @Query("DELETE FROM transaction_cache WHERE cachedAt < :threshold")
    suspend fun clearOld(threshold: Long)

    @Query("SELECT COUNT(*) FROM transaction_cache")
    suspend fun count(): Int
}
