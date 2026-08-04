package com.finploit.android.data.api

import com.finploit.android.data.dto.CreateGoalRequest
import com.finploit.android.data.dto.GoalDto
import com.finploit.android.data.dto.UpdateGoalPaceRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GoalApi {
    @GET("goals")
    suspend fun getGoals(): List<GoalDto>

    @GET("goals/{id}")
    suspend fun getGoal(@Path("id") id: Int): GoalDto

    @POST("goals")
    suspend fun createGoal(@Body request: CreateGoalRequest): GoalDto

    @PUT("goals/{id}")
    suspend fun updateGoal(@Path("id") id: Int, @Body request: CreateGoalRequest): GoalDto

    /** Só o ritmo mensal e a prioridade — o servidor aceita um corpo parcial. */
    @PUT("goals/{id}")
    suspend fun updateGoalPace(@Path("id") id: Int, @Body request: UpdateGoalPaceRequest): GoalDto

    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Int)
}
