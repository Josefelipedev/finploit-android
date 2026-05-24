package com.finploit.android.data.dto

data class AnalysisResponse(
    val Daily: TimeAnalysisDto?,
    val Weekly: TimeAnalysisDto?,
    val Monthly: TimeAnalysisDto?,
    val Year: TimeAnalysisDto?,
    val categorySummary: List<CategorySummaryDto>,
    val recurring: List<RecurringTransactionDto>,
    val goals: List<GoalDto>,
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
)

data class InsightResponse(val insight: String)

data class ReceiptAnalysisResult(
    val amount: Double?,
    val description: String?,
    val category: String?,
)
