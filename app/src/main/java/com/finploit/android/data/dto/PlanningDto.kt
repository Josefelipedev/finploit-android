package com.finploit.android.data.dto

/**
 * Planeamento plurianual. Espelha as rotas `planning` — o servidor é que faz as
 * contas todas (média do histórico, câmbio, projeção), o cliente só desenha.
 *
 * Os campos anuláveis são-no de propósito: uma meta sem data-alvo não tem
 * ritmo exigido, e uma categoria sem plano não tem valor planeado. Pôr zero no
 * lugar do nulo faria "não se sabe" parecer "é zero".
 */

data class PlanEventDto(
    val id: Int,
    val scenarioId: Int,
    val name: String,
    val type: String,
    val amount: Double,
    val currency: String?,
    val frequency: String,
    val startMonth: String,
    val endMonth: String?,
    val categoryId: Int?,
    val isActive: Boolean,
    val growsWithInflation: Boolean,
)

data class PlanScenarioDto(
    val id: Int,
    val name: String,
    val description: String?,
    val isBaseline: Boolean,
    val horizonYears: Int,
    val inflationPct: Double,
    val incomeGrowthPct: Double,
    val savingsReturnPct: Double,
    val startingNetWorth: Double?,
    val events: List<PlanEventDto> = emptyList(),
)

/** O cabeçalho da projeção: pode não ter `id` (o rumo actual, sem cenário gravado). */
data class ProjectionScenarioDto(
    val id: Int?,
    val name: String,
    val description: String?,
    val isBaseline: Boolean,
    val horizonYears: Int,
    val inflationPct: Double,
    val incomeGrowthPct: Double,
    val savingsReturnPct: Double,
    val startingNetWorth: Double?,
)

data class ProjectionMonthDto(
    val month: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val balance: Double,
)

data class ProjectionYearDto(
    val year: Int,
    val income: Double,
    val expense: Double,
    val net: Double,
    val endBalance: Double,
)

data class ProjectionSummaryDto(
    val startBalance: Double,
    val endBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalNet: Double,
    val monthlySurplus: Double,
)

data class BaselineLineDto(
    val categoryId: Int?,
    val name: String,
    val type: String,
    val monthlyAmount: Double,
)

data class EndingCommitmentDto(
    val name: String,
    val type: String,
    val monthlyAmount: Double,
    val endsAfter: String,
)

data class BaselineWindowDto(
    val start: String,
    val end: String,
)

data class ProjectionBaselineDto(
    val lines: List<BaselineLineDto> = emptyList(),
    val endingCommitments: List<EndingCommitmentDto> = emptyList(),
    val window: BaselineWindowDto?,
    val monthsCovered: Int,
    val netWorth: Double,
)

data class ProjectionDto(
    val scenario: ProjectionScenarioDto,
    val months: List<ProjectionMonthDto> = emptyList(),
    val years: List<ProjectionYearDto> = emptyList(),
    val summary: ProjectionSummaryDto,
    val baseline: ProjectionBaselineDto,
    val displayCurrency: String?,
    val rateDate: String?,
    val unconvertedCurrencies: List<String> = emptyList(),
)

data class GoalPaceDto(
    val id: Int,
    val name: String,
    val targetValue: Double,
    val currentValue: Double,
    val priority: Int,
    val remaining: Double,
    val monthsRemaining: Int?,
    val requiredMonthly: Double?,
    val plannedMonthly: Double?,
    val monthsAtPlannedPace: Int?,
    val allocatedMonthly: Double,
    val funded: Boolean,
    val shortfallMonthly: Double,
    val overdue: Boolean,
)

data class GoalPlanDto(
    val goals: List<GoalPaceDto> = emptyList(),
    val totalRequiredMonthly: Double,
    val monthlySurplus: Double,
    val unallocatedMonthly: Double,
    val feasible: Boolean,
    val scenarioId: Int?,
    val scenarioName: String?,
    val displayCurrency: String?,
)

data class PlanningOverviewDto(
    val scenarios: List<PlanScenarioDto> = emptyList(),
    val projection: ProjectionDto,
    val goalPlan: GoalPlanDto,
)

data class YearPlanItemDto(
    val categoryId: Int,
    val categoryName: String,
    val color: String?,
    val iconName: String?,
    val type: String,
    val plannedAmount: Double?,
    val monthlyPlanned: Double?,
    val realizedAmount: Double,
    val baselineAmount: Double,
    val difference: Double?,
    val progressPct: Double?,
    val note: String?,
)

data class YearPlanTotalsDto(
    val plannedIncome: Double,
    val plannedExpense: Double,
    val realizedIncome: Double,
    val realizedExpense: Double,
    val baselineIncome: Double,
    val baselineExpense: Double,
)

data class YearPlanDto(
    val year: Int,
    val items: List<YearPlanItemDto> = emptyList(),
    val totals: YearPlanTotalsDto,
    val displayCurrency: String?,
    val rateDate: String?,
    val unconvertedCurrencies: List<String> = emptyList(),
)

// ── Pedidos ────────────────────────────────────────────────────────────────

data class ScenarioRequest(
    val name: String,
    val description: String? = null,
    val isBaseline: Boolean? = null,
    val horizonYears: Int? = null,
    val inflationPct: Double? = null,
    val incomeGrowthPct: Double? = null,
    val savingsReturnPct: Double? = null,
)

data class PlanEventRequest(
    val name: String,
    val type: String,
    val amount: Double,
    val frequency: String = "monthly",
    val startMonth: String,
    val endMonth: String? = null,
    val isActive: Boolean? = null,
    val growsWithInflation: Boolean? = null,
)

data class YearPlanItemRequest(
    val categoryId: Int,
    val type: String,
    val plannedAmount: Double,
    val note: String? = null,
)

data class SaveYearPlanRequest(
    val items: List<YearPlanItemRequest>,
)
