package com.finploit.android.data.api

import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.BillsResponse
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface BillsApi {
    @GET("bills")
    suspend fun getBills(@Query("month") month: String?): BillsResponse

    @PATCH("bills/{id}/pay")
    suspend fun pay(@Path("id") id: Int): BillItemDto

    @PATCH("bills/{id}/unpay")
    suspend fun unpay(@Path("id") id: Int): BillItemDto
}
