package com.finploit.android.data.api

import com.finploit.android.data.dto.AnalysisResponse
import com.finploit.android.data.dto.InsightResponse
import com.finploit.android.data.dto.ReceiptAnalysisResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AnalysisApi {
    /**
     * As datas são opcionais: sem elas, o servidor devolve o histórico todo —
     * que era o único comportamento que este cliente conhecia. A API passou a
     * aceitar período para a web deixar de recalcular tudo no cliente, e aqui
     * serve o seletor do ecrã de Análise.
     *
     * O formato é `aaaa-mm-dd`: uma data sem hora é o dia inteiro em UTC, tanto
     * aqui como no resto da API, para os dois ecrãs não discordarem sobre onde
     * acaba o dia.
     */
    @GET("analysis")
    suspend fun getAnalysis(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): AnalysisResponse

    /**
     * O período faz parte da chave da cache do texto no servidor: sem ele,
     * trocar de período devolvia o comentário do período anterior.
     */
    @GET("analysis/insight")
    suspend fun getInsight(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): InsightResponse

    @POST("analysis/receipt")
    suspend fun analyseReceipt(@Body body: Map<String, String>): ReceiptAnalysisResult
}
