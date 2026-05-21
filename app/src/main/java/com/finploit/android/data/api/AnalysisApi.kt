package com.finploit.android.data.api

import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.dto.InsightResponse
import retrofit2.http.GET

interface AnalysisApi {
    @GET("analysis")
    suspend fun getAnalysis(): AnalysisResponse

    @GET("analysis/insight")
    suspend fun getInsight(): InsightResponse
}
