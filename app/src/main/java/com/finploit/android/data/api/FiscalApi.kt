package com.finploit.android.data.api

import com.finploit.android.data.dto.AskRequest
import com.finploit.android.data.dto.AskResponse
import com.finploit.android.data.dto.FiscalObligationsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FiscalApi {
    @GET("fiscal/obligations")
    suspend fun getObligations(): FiscalObligationsResponse

    @POST("fiscal/ask")
    suspend fun ask(@Body body: AskRequest): AskResponse
}
