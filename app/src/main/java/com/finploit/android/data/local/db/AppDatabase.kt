package com.finploit.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.finploit.android.data.local.dao.BudgetLimitDao
import com.finploit.android.data.local.dao.TransactionCacheDao
import com.finploit.android.data.local.entity.BudgetLimitEntity
import com.finploit.android.data.local.entity.TransactionCacheEntity

@Database(
    entities = [BudgetLimitEntity::class, TransactionCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun transactionCacheDao(): TransactionCacheDao
}
