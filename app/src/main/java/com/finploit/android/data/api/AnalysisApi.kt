package com.finploit.android.data.api

import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.dto.InsightResponse
import com.finploit.android.data.dto.ReceiptAnalysisResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AnalysisApi {
    @GET("analysis")
    suspend fun getAnalysis(): AnalysisResponse

    @GET("analysis/insight")
    suspend fun getInsight(): InsightResponse

    @POST("analysis/receipt")
    suspend fun analyseReceipt(@Body body: Map<String, String>): ReceiptAnalysisResult
}
