package com.finploit.android.data.repository

import com.finploit.android.data.api.AnalysisApi
import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.dto.InsightResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepository @Inject constructor(private val api: AnalysisApi) {
    suspend fun getAnalysis(): Result<AnalysisResponse> = runCatching { api.getAnalysis() }
    suspend fun getInsight(): Result<InsightResponse> = runCatching { api.getInsight() }
}
