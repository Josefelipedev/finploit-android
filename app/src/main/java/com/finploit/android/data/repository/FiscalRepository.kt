package com.finploit.android.data.repository

import com.finploit.android.data.api.AuthApi
import com.finploit.android.data.api.FiscalApi
import com.finploit.android.data.dto.FiscalObligationsResponse
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

    suspend fun saveProfile(
        country: String,
        regime: String,
        activityStartDate: String,
        fiscalNumber: String?,
    ): Result<Unit> = runCatching {
        authApi.updateProfile(
            UpdateProfileRequest(
                fiscalCountry = country,
                fiscalRegime = regime,
                activityStartDate = activityStartDate,
                fiscalNumber = fiscalNumber?.takeIf { it.isNotBlank() },
            )
        )
        Unit
    }
}
