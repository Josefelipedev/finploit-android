package com.finploit.android.data.dto

data class GoalDto(
    val id: Int,
    val name: String,
    val targetValue: Double,
    val currentValue: Double?,
    val description: String?,
    val status: String?,
    val startDate: String?,
    val endDate: String?,
    val userId: Int?,
)

data class CreateGoalRequest(
    val name: String,
    val targetValue: Double,
    val currentValue: Double? = null,
    val description: String? = null,
    val status: String? = "ACTIVE",
    val startDate: String? = null,
    val endDate: String? = null,
)
