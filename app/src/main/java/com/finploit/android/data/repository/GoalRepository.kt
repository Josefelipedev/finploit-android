package com.finploit.android.data.repository

import com.finploit.android.data.api.GoalApi
import com.finploit.android.data.dto.CreateGoalRequest
import com.finploit.android.data.dto.GoalDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(private val api: GoalApi) {
    suspend fun getGoals(): Result<List<GoalDto>> = runCatching { api.getGoals() }

    suspend fun createGoal(request: CreateGoalRequest): Result<GoalDto> =
        runCatching { api.createGoal(request) }

    suspend fun updateGoal(id: Int, request: CreateGoalRequest): Result<GoalDto> =
        runCatching { api.updateGoal(id, request) }

    suspend fun deleteGoal(id: Int): Result<Unit> =
        runCatching { api.deleteGoal(id) }
}
