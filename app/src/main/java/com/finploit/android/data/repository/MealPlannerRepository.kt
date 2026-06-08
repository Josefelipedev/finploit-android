package com.finploit.android.data.repository

import com.finploit.android.data.api.BatchToggleRequest
import com.finploit.android.data.api.MealPlannerApi
import com.finploit.android.data.dto.AddShoppingItemRequest
import com.finploit.android.data.dto.GeneratePlanRequest
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.MealShoppingItemDto
import com.finploit.android.data.dto.SaveScheduleRequest
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.data.dto.SubstituteMealRequest
import com.finploit.android.data.dto.SubstituteMealResponse
import com.finploit.android.data.dto.UpdateActualPriceRequest
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
        when {
            // Backend returns 404 when no active plan exists (correct path after fix)
            e is retrofit2.HttpException && e.code() == 404 -> null
            // Fallback: Retrofit NPE from empty/null body — treat as "no plan"
            e is NullPointerException || e.message?.contains("was null but response body") == true -> null
            else -> throw e
        }
    }

    suspend fun generatePlan(budget: Double? = null, currencyCode: String? = null, currencySymbol: String? = null, currencyLocale: String? = null, mealPrepMode: Boolean? = null, dietMode: String? = null, badMeals: List<String>? = null, favoriteMeals: List<String>? = null, dislikedFoods: List<String>? = null): Result<MealPlanDto> = runCatching {
        api.generatePlan(GeneratePlanRequest(budget, currencyCode, currencySymbol, currencyLocale, mealPrepMode, dietMode, badMeals, favoriteMeals, dislikedFoods))
    }.recoverCatching { e ->
        if (e is retrofit2.HttpException && e.code() == 404)
            throw Exception("Funcionalidade não disponível no servidor. Atualize o backend.")
        else throw e
    }

    suspend fun toggleItem(id: Int): Result<MealShoppingItemDto> = runCatching { api.toggleItem(id) }

    suspend fun batchToggleItems(itemIds: List<Int>): Result<Unit> = runCatching { api.batchToggleItems(BatchToggleRequest(itemIds)) }

    suspend fun getAllPlans(): Result<List<MealPlanDto>> = runCatching { api.getAllPlans() }

    suspend fun getPlanDays(planId: Int): Result<List<MealPlanDayDto>> = runCatching { api.getPlanDays(planId) }

    suspend fun getProfile(): Result<UserProfileDto> = runCatching {
        // Backend now always returns a profile object; null means no profile yet — treat as default.
        api.getProfile() ?: UserProfileDto()
    }.recoverCatching { e ->
        if (e is retrofit2.HttpException && e.code() == 404) UserProfileDto()
        else throw e
    }

    suspend fun saveProfile(request: UserProfileRequest): Result<UserProfileDto> =
        runCatching { api.saveProfile(request) ?: UserProfileDto() }

    suspend fun deletePlan(id: Int): Result<Unit> = runCatching { api.deletePlan(id) }

    suspend fun clearHistory(): Result<Unit> = runCatching { api.clearHistory() }

    suspend fun resetShoppingList(): Result<MealPlanDto> = runCatching { api.resetShoppingList() }

    suspend fun addShoppingItem(req: AddShoppingItemRequest): Result<MealShoppingItemDto> = runCatching { api.addShoppingItem(req) }

    suspend fun updateActualPrice(id: Int, price: Double?): Result<MealShoppingItemDto> = runCatching { api.updateActualPrice(id, UpdateActualPriceRequest(price)) }

    suspend fun substituteMeal(dayId: Int, mealType: String, preferences: String? = null): Result<SubstituteMealResponse> = runCatching {
        api.substituteMeal(dayId, SubstituteMealRequest(mealType, preferences))
    }
}
