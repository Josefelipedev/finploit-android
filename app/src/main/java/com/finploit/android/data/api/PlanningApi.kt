package com.finploit.android.data.api

import com.finploit.android.data.dto.PlanEventDto
import com.finploit.android.data.dto.PlanEventRequest
import com.finploit.android.data.dto.PlanScenarioDto
import com.finploit.android.data.dto.PlanningOverviewDto
import com.finploit.android.data.dto.SaveYearPlanRequest
import com.finploit.android.data.dto.ScenarioRequest
import com.finploit.android.data.dto.YearPlanDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PlanningApi {
    /** Cenários, projeção e metas numa só ida — evita três esperas ao abrir o ecrã. */
    @GET("planning/overview")
    suspend fun getOverview(@Query("scenarioId") scenarioId: Int? = null): PlanningOverviewDto

    @POST("planning/scenarios")
    suspend fun createScenario(@Body request: ScenarioRequest): PlanScenarioDto

    @PUT("planning/scenarios/{id}")
    suspend fun updateScenario(@Path("id") id: Int, @Body request: ScenarioRequest): PlanScenarioDto

    @DELETE("planning/scenarios/{id}")
    suspend fun deleteScenario(@Path("id") id: Int)

    @POST("planning/scenarios/{id}/events")
    suspend fun createEvent(@Path("id") scenarioId: Int, @Body request: PlanEventRequest): PlanEventDto

    @PUT("planning/events/{id}")
    suspend fun updateEvent(@Path("id") id: Int, @Body request: PlanEventRequest): PlanEventDto

    @DELETE("planning/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Int)

    @GET("planning/years/{year}")
    suspend fun getYearPlan(@Path("year") year: Int): YearPlanDto

    @PUT("planning/years/{year}")
    suspend fun saveYearPlan(@Path("year") year: Int, @Body request: SaveYearPlanRequest): YearPlanDto

    @DELETE("planning/years/{year}/categories/{categoryId}")
    suspend fun deleteYearPlanItem(@Path("year") year: Int, @Path("categoryId") categoryId: Int)
}
