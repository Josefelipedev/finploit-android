package com.finploit.android.data.repository

import com.finploit.android.data.api.BatchToggleRequest
import com.finploit.android.data.api.MealPlannerApi
import com.finploit.android.data.dto.GeneratePlanRequest
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.MealShoppingItemDto
import com.finploit.android.data.dto.SaveScheduleRequest
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.data.dto.UserProfileDto
import com.finploit.android.data.dto.UserProfileRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlannerRepository @Inject constructor(
    private val api: MealPlannerApi,
) {
    suspend fun getSchedule(): Result<List<ScheduleItemDto>> = runCatching {
        api.getSchedule()
    }.recoverCatching { e ->
        if (e is retrofit2.HttpException && e.code() == 404) emptyList()
        else throw e
    }

    suspend fun saveSchedule(schedule: List<ScheduleItemDto>): Result<List<ScheduleItemDto>> =
        runCatching { api.saveSchedule(SaveScheduleRequest(schedule)) }

    suspend fun getActivePlan(): Result<MealPlanDto?> = runCatching {
        api.getActivePlan()
    }.recoverCatching { e ->
        if (e is retrofit2.HttpException && e.code() == 404) null
        else throw e
    }

    suspend fun generatePlan(budget: Double? = null, currencyCode: String? = null, currencySymbol: String? = null, currencyLocale: String? = null, mealPrepMode: Boolean? = null): Result<MealPlanDto> = runCatching {
        api.generatePlan(GeneratePlanRequest(budget, currencyCode, currencySymbol, currencyLocale, mealPrepMode))
    }.recoverCatching { e ->
        if (e is retrofit2.HttpException && e.code() == 404)
            throw Exception("Funcionalidade não disponível no servidor. Atualize o backend.")
        else throw e
    }

    suspend fun toggleItem(id: Int): Result<MealShoppingItemDto> = runCatching { api.toggleItem(id) }

    suspend fun batchToggleItems(itemIds: List<Int>): Result<Unit> = runCatching { api.batchToggleItems(BatchToggleRequest(itemIds)) }

    suspend fun getAllPlans(): Result<List<MealPlanDto>> = runCatching { api.getAllPlans() }

    suspend fun getProfile(): Result<UserProfileDto> = runCatching { api.getProfile() }
        .recoverCatching { e ->
            if (e is retrofit2.HttpException && e.code() == 404) UserProfileDto()
            else throw e
        }

    suspend fun saveProfile(request: UserProfileRequest): Result<UserProfileDto> =
        runCatching { api.saveProfile(request) }

    suspend fun deletePlan(id: Int): Result<Unit> = runCatching { api.deletePlan(id) }

    suspend fun clearHistory(): Result<Unit> = runCatching { api.clearHistory() }
}
