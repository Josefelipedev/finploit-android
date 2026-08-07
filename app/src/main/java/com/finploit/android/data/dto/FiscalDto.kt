package com.finploit.android.data.dto

import com.google.gson.JsonElement

data class FiscalObligationsResponse(
    val configured: Boolean = false,
    val country: String? = null,
    val profile: FiscalProfileDto? = null,
    val status: FiscalStatusDto? = null,
    val warnings: List<FiscalWarningDto> = emptyList(),
    val obligations: List<FiscalObligationDto> = emptyList(),
    val upcoming: List<FiscalDeadlineDto> = emptyList(),
    val nextDeadline: FiscalDeadlineDto? = null,
    val disclaimer: String? = null,
)

data class FiscalProfileDto(
    val regime: String? = null,
    val regimeLabel: String? = null,
    val accountingRegime: String? = null,
    val ivaStatus: String? = null,
    val ivaLabel: String? = null,
    val withholdingMode: String? = null,
    val withholdingLabel: String? = null,
    val socialSecurityStatus: String? = null,
    val socialSecurityLabel: String? = null,
    val activityStartDate: String? = null,
    val fiscalNumber: String? = null,
    val activityCode: String? = null,
    val annualRevenue: Double = 0.0,
    /** De onde saiu o volume acima: dos lançamentos ou do campo do perfil (C6). */
    val revenueSource: String? = null,
    val ledgerRevenueEur: Double? = null,
    val manualRevenue: Double? = null,
    val revenueYear: Int? = null,
    val thresholdEur: Double? = null,
    val immediateExitThresholdEur: Double? = null,
    val currency: String? = null,
    val hasEuB2bClients: Boolean = false,
    val hasNonEuClients: Boolean = false,
    val hasPaymentsOnAccount: Boolean = false,
    val hasWorkAccidentInsurance: Boolean = false,
    val usesPortalInvoices: Boolean = true,
    val hasEmployees: Boolean = false,
)

data class FiscalStatusDto(
    val socialSecurityFirstYearExempt: Boolean = false,
    val socialSecurityExemptUntil: String? = null,
    val socialSecurityContributing: Boolean = false,
    val revenueProgressPercent: Double = 0.0,
    val profileCompleteness: Int = 0,
)

data class FiscalWarningDto(
    val key: String? = null,
    val severity: String? = null,
    val title: String? = null,
    val description: String? = null,
    val sourceUrl: String? = null,
)

data class FiscalProfileSettings(
    val activityStartDate: String,
    val fiscalNumber: String? = null,
    val accountingRegime: String = "simplified",
    val vatRegime: String = "exempt_art53",
    val withholdingMode: String = "exempt_art101b",
    val socialSecurityStatus: String = "auto",
    val activityCode: String? = null,
    val annualRevenue: Double = 0.0,
    val hasEuB2bClients: Boolean = false,
    val hasNonEuClients: Boolean = false,
    val hasPaymentsOnAccount: Boolean = false,
    val hasWorkAccidentInsurance: Boolean = false,
    val usesPortalInvoices: Boolean = true,
    val hasEmployees: Boolean = false,
)

data class FiscalObligationDto(
    val key: String? = null,
    val title: String? = null,
    val description: String? = null,
    val frequency: String? = null,
    val category: String? = null,
    val sourceUrl: String? = null,
    val conditional: Boolean = false,
    // meta is an optional, free-form payload; kept as JsonElement so any shape deserializes safely
    val meta: JsonElement? = null,
)

data class FiscalDeadlineDto(
    val date: String? = null,
    val endDate: String? = null,
    val title: String? = null,
    val description: String? = null,
    val tag: String? = null,
    val daysUntil: Int = 0,
    val sourceUrl: String? = null,
)

data class ChatMessageDto(
    val role: String,
    val content: String,
)

data class AskRequest(
    val question: String,
    val history: List<ChatMessageDto>? = null,
)

data class AskResponse(
    val answer: String = "",
)
