package com.finploit.android.data.dto

data class AnalysisResponse(
    val Daily: TimeAnalysisDto?,
    val Weekly: TimeAnalysisDto?,
    val Monthly: TimeAnalysisDto?,
    val Year: TimeAnalysisDto?,
    val categorySummary: List<CategorySummaryDto>,
    val recurring: List<RecurringTransactionDto>,
    val goals: List<GoalDto>,
    /** Moeda em que os números acima estão — a de quem pergunta. */
    val displayCurrency: String? = null,
    /**
     * Moedas que entraram nos totais SEM conversão, por não haver taxa. O
     * servidor sempre as mandou; este ecrã era o único que as ignorava, e
     * mostrava uma soma de moedas misturadas sem o dizer (C4).
     */
    val unconvertedCurrencies: List<String>? = null,
)

data class TimeAnalysisDto(
    val labels: List<String>,
    val datasets: List<DatasetDto>,
    val summary: TimeSummaryDto,
)

data class DatasetDto(val data: List<Double>)

data class TimeSummaryDto(
    val income: Double,
    val expense: Double,
)

data class CategorySummaryDto(
    val category: String,
    val icon: String?,
    val income: Double,
    val expense: Double,
    /** De quem é este número (C6). Menos de dois = nada a repartir. */
    val byOwner: List<CategoryOwnerSplitDto>? = null,
)

data class CategoryOwnerSplitDto(
    val userId: Int,
    val name: String? = null,
    val income: Double = 0.0,
    val expense: Double = 0.0,
)

data class InsightResponse(val insight: String)

data class ReceiptAnalysisResult(
    val amount: Double?,
    val description: String?,
    val category: String?,
)
