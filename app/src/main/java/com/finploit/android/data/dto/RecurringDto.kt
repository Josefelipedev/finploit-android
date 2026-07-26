package com.finploit.android.data.dto

data class RecurringTransactionDto(
    val id: Int,
    val description: String?,
    val amount: Double,
    val currency: String? = null,
    val type: String,
    val frequency: String,
    val dueDay: Int?,
    val weekDay: Int?,
    val notification: Boolean?,
    val endDate: String?,
    val occurrences: Int?,
    val userId: Int?,
    val categoryId: Int? = null,
)

data class CreateRecurringRequest(
    val description: String,
    val amount: Double,
    val currency: String? = null,
    val type: String,
    val frequency: String,
    val dueDay: Int,
    val weekDay: Int,
    val notification: Boolean,
    val categoryId: Int,
    val endDate: String,
    val occurrences: Int,
)
