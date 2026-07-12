package com.finploit.android.data.dto

import com.google.gson.JsonElement

data class FiscalObligationsResponse(
    val configured: Boolean = false,
    val country: String? = null,
    val profile: FiscalProfileDto? = null,
    val status: FiscalStatusDto? = null,
    val obligations: List<FiscalObligationDto> = emptyList(),
    val upcoming: List<FiscalDeadlineDto> = emptyList(),
    val nextDeadline: FiscalDeadlineDto? = null,
    val disclaimer: String? = null,
)

data class FiscalProfileDto(
    val regime: String? = null,
    val regimeLabel: String? = null,
    val ivaStatus: String? = null,
    val ivaLabel: String? = null,
    val activityStartDate: String? = null,
    val fiscalNumber: String? = null,
    val thresholdEur: Double? = null,
    val currency: String? = null,
)

data class FiscalStatusDto(
    val socialSecurityFirstYearExempt: Boolean = false,
    val socialSecurityExemptUntil: String? = null,
)

data class FiscalObligationDto(
    val key: String? = null,
    val title: String? = null,
    val description: String? = null,
    val frequency: String? = null,
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
