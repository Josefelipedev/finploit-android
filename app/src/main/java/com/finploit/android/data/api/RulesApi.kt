package com.finploit.android.data.api

import com.finploit.android.data.dto.RulesOverviewDto
import com.finploit.android.data.dto.RulesSummaryDto
import com.finploit.android.data.dto.SaveSplitRequest
import com.finploit.android.data.dto.SetCategoryBucketsRequest
import com.finploit.android.data.dto.UpsertRuleRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * As regras do dinheiro.
 *
 * **Todas as escritas devolvem a visão inteira**, já refeita pelo servidor —
 * mudar um alvo muda o veredicto de todas as outras regras. É por isso que
 * nenhuma delas devolve só o objecto que mexeu.
 */
interface RulesApi {
    @GET("rules")
    suspend fun getOverview(): RulesOverviewDto

    /** Só os números do cartão do Dashboard. */
    @GET("rules/summary")
    suspend fun getSummary(): RulesSummaryDto

    @PUT("rules/split")
    suspend fun saveSplit(@Body request: SaveSplitRequest): RulesOverviewDto

    @PUT("rules/categories")
    suspend fun setCategoryBuckets(@Body request: SetCategoryBucketsRequest): RulesOverviewDto

    @POST("rules")
    suspend fun create(@Body request: UpsertRuleRequest): RulesOverviewDto

    @PATCH("rules/{id}")
    suspend fun update(@Path("id") id: Int, @Body request: UpsertRuleRequest): RulesOverviewDto

    @DELETE("rules/{id}")
    suspend fun delete(@Path("id") id: Int): RulesOverviewDto
}
