package com.finploit.android.data.repository

import com.finploit.android.data.api.RulesApi
import com.finploit.android.data.dto.CategoryBucketItem
import com.finploit.android.data.dto.RulesOverviewDto
import com.finploit.android.data.dto.RulesSummaryDto
import com.finploit.android.data.dto.SaveSplitRequest
import com.finploit.android.data.dto.SetCategoryBucketsRequest
import com.finploit.android.data.dto.UpsertRuleRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * As regras vêm do servidor, inteiras, a cada escrita.
 *
 * Não há cache local de propósito: o veredicto depende do histórico e das
 * taxas de câmbio, que só o servidor tem, e uma cópia local seria uma segunda
 * resposta à mesma pergunta — foi exactamente o que aconteceu aos limites de
 * orçamento antes de o Room sair daqui.
 */
@Singleton
class RulesRepository @Inject constructor(
    private val api: RulesApi,
) {
    suspend fun getOverview(): Result<RulesOverviewDto> = runCatching { api.getOverview() }

    suspend fun getSummary(): Result<RulesSummaryDto> = runCatching { api.getSummary() }

    suspend fun saveSplit(request: SaveSplitRequest): Result<RulesOverviewDto> =
        runCatching { api.saveSplit(request) }

    suspend fun setCategoryBuckets(items: List<CategoryBucketItem>): Result<RulesOverviewDto> =
        runCatching { api.setCategoryBuckets(SetCategoryBucketsRequest(items)) }

    suspend fun createRule(request: UpsertRuleRequest): Result<RulesOverviewDto> =
        runCatching { api.create(request) }

    suspend fun updateRule(id: Int, request: UpsertRuleRequest): Result<RulesOverviewDto> =
        runCatching { api.update(id, request) }

    suspend fun deleteRule(id: Int): Result<RulesOverviewDto> = runCatching { api.delete(id) }
}
