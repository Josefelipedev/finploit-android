package com.finploit.android.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.finploit.android.data.api.AnalysisApi
import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.dto.InsightResponse
import com.finploit.android.data.dto.ReceiptAnalysisResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepository @Inject constructor(
    private val api: AnalysisApi,
    @ApplicationContext private val context: Context,
) {
    suspend fun getAnalysis(): Result<AnalysisResponse> = runCatching { api.getAnalysis() }
    suspend fun getInsight(): Result<InsightResponse> = runCatching { api.getInsight() }

    suspend fun analyseReceiptImage(uri: Uri): Result<ReceiptAnalysisResult> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw IllegalArgumentException("Não foi possível ler a imagem")
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        api.analyseReceipt(mapOf("image" to base64, "mimeType" to "image/jpeg"))
    }
}
