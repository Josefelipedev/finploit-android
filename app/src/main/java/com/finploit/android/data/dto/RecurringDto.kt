package com.finploit.android.data.dto

data class RecurringTransactionDto(
    val id: Int,
    val description: String?,
    val amount: Double,
    val type: String,
    val frequency: String,
    val dueDay: Int?,
    val weekDay: Int?,
    val notification: Boolean?,
    val endDate: String?,
    val occurrences: Int?,
    val userId: Int?,
)

data class CreateRecurringRequest(
    val description: String,
    val amount: Double,
    val type: String,
    val frequency: String,
    val dueDay: Int,
    val weekDay: Int,
    val notification: Boolean,
    val categoria: String,
    val endDate: String,
    val occurrences: Int,
)
