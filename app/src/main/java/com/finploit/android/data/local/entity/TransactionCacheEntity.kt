package com.finploit.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_cache")
data class TransactionCacheEntity(
    @PrimaryKey val id: Int,
    val type: String,
    val amount: Double,
    val description: String?,
    val category: String?,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
