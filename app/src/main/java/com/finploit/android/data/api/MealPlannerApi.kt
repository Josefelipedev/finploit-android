package com.finploit.android.data.api

import com.finploit.android.data.dto.GeneratePlanRequest
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.MealShoppingItemDto
import com.finploit.android.data.dto.SaveScheduleRequest
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.data.dto.UserProfileDto
import com.finploit.android.data.dto.UserProfileRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class BatchToggleRequest(val itemIds: List<Int>)

interface MealPlannerApi {
    @GET("meal-planner/schedule")
    suspend fun getSchedule(): List<ScheduleItemDto>

    @POST("meal-planner/schedule")
    suspend fun saveSchedule(@Body request: SaveScheduleRequest): List<ScheduleItemDto>

    @GET("meal-planner/active")
    suspend fun getActivePlan(): MealPlanDto?

    @GET("meal-planner/plans")
    suspend fun getAllPlans(): List<MealPlanDto>

    @POST("meal-planner/generate")
    suspend fun generatePlan(@Body request: GeneratePlanRequest): MealPlanDto

    @PATCH("meal-planner/item/{id}/toggle")
    suspend fun toggleItem(@Path("id") id: Int): MealShoppingItemDto

    @PATCH("meal-planner/items/batch-toggle")
    suspend fun batchToggleItems(@Body request: BatchToggleRequest)

    @GET("meal-planner/profile")
    suspend fun getProfile(): UserProfileDto

    @PATCH("meal-planner/profile")
    suspend fun saveProfile(@Body request: UserProfileRequest): UserProfileDto

    @DELETE("meal-planner/plans/{id}")
    suspend fun deletePlan(@Path("id") id: Int)

    @DELETE("meal-planner/plans/history")
    suspend fun clearHistory()
}
