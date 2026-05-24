package com.finploit.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey val categoryId: Int,
    val categoryName: String,
    val monthlyLimit: Double,
    val alertAt: Int = 80, // percentage
)
