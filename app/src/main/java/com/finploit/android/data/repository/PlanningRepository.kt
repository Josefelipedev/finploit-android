package com.finploit.android.data.repository

import com.finploit.android.data.api.PlanningApi
import com.finploit.android.data.dto.PlanEventDto
import com.finploit.android.data.dto.PlanEventRequest
import com.finploit.android.data.dto.PlanScenarioDto
import com.finploit.android.data.dto.PlanningOverviewDto
import com.finploit.android.data.dto.SaveYearPlanRequest
import com.finploit.android.data.dto.ScenarioRequest
import com.finploit.android.data.dto.YearPlanDto
import com.finploit.android.data.dto.YearPlanItemRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanningRepository @Inject constructor(private val api: PlanningApi) {

    suspend fun getOverview(scenarioId: Int? = null): Result<PlanningOverviewDto> =
        runCatching { api.getOverview(scenarioId) }

    suspend fun createScenario(request: ScenarioRequest): Result<PlanScenarioDto> =
        runCatching { api.createScenario(request) }

    suspend fun updateScenario(id: Int, request: ScenarioRequest): Result<PlanScenarioDto> =
        runCatching { api.updateScenario(id, request) }

    suspend fun deleteScenario(id: Int): Result<Unit> =
        runCatching { api.deleteScenario(id) }

    suspend fun createEvent(scenarioId: Int, request: PlanEventRequest): Result<PlanEventDto> =
        runCatching { api.createEvent(scenarioId, request) }

    suspend fun updateEvent(id: Int, request: PlanEventRequest): Result<PlanEventDto> =
        runCatching { api.updateEvent(id, request) }

    suspend fun deleteEvent(id: Int): Result<Unit> =
        runCatching { api.deleteEvent(id) }

    suspend fun getYearPlan(year: Int): Result<YearPlanDto> =
        runCatching { api.getYearPlan(year) }

    suspend fun saveYearPlan(year: Int, items: List<YearPlanItemRequest>): Result<YearPlanDto> =
        runCatching { api.saveYearPlan(year, SaveYearPlanRequest(items)) }

    suspend fun deleteYearPlanItem(year: Int, categoryId: Int): Result<Unit> =
        runCatching { api.deleteYearPlanItem(year, categoryId) }
}
