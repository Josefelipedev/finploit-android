package com.finploit.android.data.repository

import com.finploit.android.data.api.GoalApi
import com.finploit.android.data.dto.CreateGoalRequest
import com.finploit.android.data.dto.DepositRequest
import com.finploit.android.data.dto.GoalDto
import com.finploit.android.data.dto.UpdateGoalPaceRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(private val api: GoalApi) {
    suspend fun getGoals(): Result<List<GoalDto>> = runCatching { api.getGoals() }

    suspend fun createGoal(request: CreateGoalRequest): Result<GoalDto> =
        runCatching { api.createGoal(request) }

    suspend fun updateGoal(id: Int, request: CreateGoalRequest): Result<GoalDto> =
        runCatching { api.updateGoal(id, request) }

    /**
     * Depositar (C2). Quem soma é o servidor: somar `currentValue + valor` no
     * cliente e mandar um PUT fazia dois depósitos simultâneos perderem-se um
     * ao outro — e não deixava rasto nenhum no livro-razão.
     */
    suspend fun deposit(id: Int, amount: Double, ledger: Boolean = true) =
        runCatching { api.deposit(id, DepositRequest(amount, ledger)) }

    suspend fun updateGoalPace(
        id: Int,
        monthlyContribution: Double,
        priority: Int,
    ): Result<GoalDto> =
        runCatching { api.updateGoalPace(id, UpdateGoalPaceRequest(monthlyContribution, priority)) }

    suspend fun deleteGoal(id: Int): Result<Unit> =
        runCatching { api.deleteGoal(id) }
}
