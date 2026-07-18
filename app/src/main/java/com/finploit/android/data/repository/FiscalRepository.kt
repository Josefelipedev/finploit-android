package com.finploit.android.data.repository

import com.finploit.android.data.api.AuthApi
import com.finploit.android.data.api.FiscalApi
import com.finploit.android.data.dto.AskRequest
import com.finploit.android.data.dto.ChatMessageDto
import com.finploit.android.data.dto.FiscalObligationsResponse
import com.finploit.android.data.dto.FiscalProfileSettings
import com.finploit.android.data.dto.UpdateProfileRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FiscalRepository @Inject constructor(
    private val api: FiscalApi,
    private val authApi: AuthApi,
) {
    suspend fun getObligations(): Result<FiscalObligationsResponse> =
        runCatching { api.getObligations() }

    suspend fun ask(
        question: String,
        history: List<ChatMessageDto>,
    ): Result<String> = runCatching {
        api.ask(AskRequest(question = question, history = history.ifEmpty { null })).answer
    }

    suspend fun saveProfile(settings: FiscalProfileSettings): Result<Unit> = runCatching {
        authApi.updateProfile(
            UpdateProfileRequest(
                fiscalCountry = "PT",
                fiscalRegime = if (settings.accountingRegime == "organized") {
                    "PT_CONTABILIDADE_ORGANIZADA"
                } else {
                    "PT_REGIME_SIMPLIFICADO"
                },
                activityStartDate = settings.activityStartDate,
                fiscalNumber = settings.fiscalNumber?.takeIf { it.isNotBlank() },
                fiscalAccountingRegime = settings.accountingRegime,
                fiscalVatRegime = settings.vatRegime,
                fiscalWithholdingMode = settings.withholdingMode,
                fiscalSocialSecurityStatus = settings.socialSecurityStatus,
                fiscalActivityCode = settings.activityCode?.takeIf { it.isNotBlank() },
                fiscalAnnualRevenue = settings.annualRevenue,
                fiscalHasEuB2bClients = settings.hasEuB2bClients,
                fiscalHasNonEuClients = settings.hasNonEuClients,
                fiscalHasPaymentsOnAccount = settings.hasPaymentsOnAccount,
                fiscalHasWorkAccidentInsurance = settings.hasWorkAccidentInsurance,
                fiscalUsesPortalInvoices = settings.usesPortalInvoices,
                fiscalHasEmployees = settings.hasEmployees,
            )
        )
        Unit
    }
}
