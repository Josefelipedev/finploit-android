package com.finploit.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.finploit.android.data.local.dao.TransactionCacheDao
import com.finploit.android.data.local.entity.TransactionCacheEntity

/**
 * Versão 2: a tabela `budget_limits` saiu — os limites passaram para o servidor
 * (C1). A base tem `fallbackToDestructiveMigration`, e o que resta aqui é
 * cache de transações, que se volta a encher sozinha.
 */
@Database(
    entities = [TransactionCacheEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionCacheDao(): TransactionCacheDao
}
