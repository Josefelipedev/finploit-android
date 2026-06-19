package com.finploit.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.finploit.android.data.api.AnalysisApi
import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.dto.InsightResponse
import com.finploit.android.data.dto.ReceiptAnalysisResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
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
        val rawBytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw IllegalArgumentException("Não foi possível ler a imagem")

        // Comprime a imagem antes de enviar para evitar erro 413
        val compressedBytes = compressImage(rawBytes, maxSidePixels = 1024, quality = 75)
        val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        api.analyseReceipt(mapOf("image" to base64, "mimeType" to "image/jpeg"))
    }

    /**
     * Redimensiona e recomprime a imagem para reduzir o payload.
     * Uma foto de câmara (3–5 MB) fica abaixo de ~400 KB após este passo.
     */
    private fun compressImage(bytes: ByteArray, maxSidePixels: Int, quality: Int): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return bytes  // fallback: devolve original se decode falhar

        val (width, height) = original.width to original.height
        val scale = maxSidePixels.toFloat() / maxOf(width, height)

        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true,
            )
        } else {
            original
        }

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }
}
