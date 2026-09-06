package com.finploit.android.data.dto

import com.google.gson.annotations.SerializedName

/**
 * As regras do dinheiro (`/rules`).
 *
 * A divisão em três (50/30/20) é a espinha: reparte **todo** o rendimento em
 * necessidades, desejos e poupança. As outras quatro famílias vigiam um número
 * cada, são quantas se quiser, e podem contradizer-se sem ficarem inválidas —
 * um tecto de 30% na habitação vive dentro de um alvo de 50% nas necessidades.
 *
 * O servidor devolve **a visão inteira em todas as escritas**: mudar um alvo
 * ou arrumar uma categoria muda o veredicto de todas as outras regras, e ir
 * buscar o resultado num segundo pedido deixava o ecrã a contradizer-se.
 */
data class SpendingSplitDto(
    val preset: String = "50-30-20",
    val needsPct: Int = 50,
    val wantsPct: Int = 30,
    val savingsPct: Int = 20,
    /** Ninguém escolheu ainda — o ecrã convida em vez de afirmar. */
    val isDefault: Boolean = true,
)

data class RulePresetDto(
    val key: String = "",
    val name: String = "",
    val description: String = "",
    val needsPct: Int = 0,
    val wantsPct: Int = 0,
    val savingsPct: Int = 0,
)

data class BucketVerdictDto(
    val bucket: String = "needs",
    val targetPct: Int = 0,
    val targetAmount: Double = 0.0,
    val actualAmount: Double = 0.0,
    /** Nulo quando não há receita: não há com que comparar. */
    val actualPct: Double? = null,
    val deltaAmount: Double = 0.0,
)

data class RuleVerdictDto(
    val income: Double = 0.0,
    val buckets: List<BucketVerdictDto> = emptyList(),
    val unclassifiedAmount: Double = 0.0,
    val leftover: Double = 0.0,
    val hasIncome: Boolean = false,
)

/** O veredicto de UMA regra, no mesmo formato para as cinco famílias. */
data class RuleCheckDto(
    val kind: String = "",
    val label: String = "",
    /** "ok" | "close" | "broken" | "unknown" */
    val status: String = "unknown",
    val actual: Double = 0.0,
    val target: Double = 0.0,
    /** "pct" | "months" | "money" */
    val unit: String = "pct",
    val message: String = "",
)

data class StoredRuleDto(
    val id: Int = 0,
    val kind: String = "",
    val bucket: String? = null,
    val categoryId: Int? = null,
    val target: Double = 0.0,
    val unit: String = "pct",
    val isActive: Boolean = true,
    /** Nulo quando a regra está em pausa — uma regra em pausa não se julga. */
    val check: RuleCheckDto? = null,
)

data class RuleCategoryDto(
    val categoryId: Int? = null,
    val name: String = "",
    val color: String? = null,
    val monthlyAmount: Double = 0.0,
    /** "needs" | "wants" | "savings", ou null quando ninguém sabe. */
    val bucket: String? = null,
    /** "manual" | "guess" | "unknown" — um palpite não é uma decisão. */
    val source: String = "unknown",
)

data class RuleMonthDto(
    val month: String = "",
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0,
)

data class RuleBasisDto(
    val monthsCovered: Int = 0,
    val lookbackMonths: Int = 3,
    val window: RuleWindowDto = RuleWindowDto(),
    /** A janela caiu no mês corrente, que ainda vai a meio. */
    val partialMonth: Boolean = false,
    val netWorth: Double = 0.0,
    val netWorthKnown: Boolean = false,
    val monthlyExpense: Double = 0.0,
)

data class RuleWindowDto(val start: String = "", val end: String = "")

data class RulesOverviewDto(
    val split: SpendingSplitDto = SpendingSplitDto(),
    val presets: List<RulePresetDto> = emptyList(),
    val verdict: RuleVerdictDto = RuleVerdictDto(),
    val rules: List<StoredRuleDto> = emptyList(),
    val checks: List<RuleCheckDto> = emptyList(),
    /** A pior de todas — a que o Dashboard mostra quando só cabe uma. */
    val worst: RuleCheckDto? = null,
    val categories: List<RuleCategoryDto> = emptyList(),
    val uncategorizedAmount: Double = 0.0,
    /** Mês a mês, para se ver se isto está a melhorar ou a piorar. */
    val history: List<RuleMonthDto> = emptyList(),
    val basis: RuleBasisDto = RuleBasisDto(),
    val displayCurrency: String = "BRL",
    val rateDate: String? = null,
    val unconvertedCurrencies: List<String> = emptyList(),
    val outOfRangeDates: Boolean = false,
)

/** O que cabe num cartão do Dashboard, sem pagar o pedido inteiro. */
data class RulesSummaryDto(
    val split: SpendingSplitDto = SpendingSplitDto(),
    val verdict: RuleVerdictDto = RuleVerdictDto(),
    val total: Int = 0,
    val ok: Int = 0,
    val broken: Int = 0,
    val worst: RuleCheckDto? = null,
    val pendingCategories: Int = 0,
    val displayCurrency: String = "BRL",
)

// ── Corpos de escrita ───────────────────────────────────────────────────────

data class SaveSplitRequest(
    val preset: String? = null,
    val needsPct: Int? = null,
    val wantsPct: Int? = null,
    val savingsPct: Int? = null,
)

/**
 * `bucket` a `null` é um valor legítimo: desfaz a escolha e devolve a categoria
 * ao palpite. Por isso o campo é anulável e vai sempre no corpo — omiti-lo
 * seria "não mexer", que é outra coisa.
 */
data class CategoryBucketItem(
    val categoryId: Int,
    @SerializedName("bucket") val bucket: String?,
)

data class SetCategoryBucketsRequest(val items: List<CategoryBucketItem>)

/**
 * Uma regra das outras quatro famílias.
 *
 * A `unit` **não vai aqui de propósito**: cada família tem a sua e é o servidor
 * que a impõe. Mandá-la deixava gravar "6% de despesa guardados", que o motor
 * leria como percentagem do rendimento e julgaria contra o número errado.
 */
data class UpsertRuleRequest(
    val kind: String? = null,
    val target: Double? = null,
    val bucket: String? = null,
    val categoryId: Int? = null,
    val isActive: Boolean? = null,
)
