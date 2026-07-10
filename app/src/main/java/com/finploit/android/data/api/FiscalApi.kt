package com.finploit.android.data.api

import com.finploit.android.data.dto.FiscalObligationsResponse
import retrofit2.http.GET

interface FiscalApi {
    @GET("fiscal/obligations")
    suspend fun getObligations(): FiscalObligationsResponse
}
