package com.finploit.android.data.api

import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.BillsResponse
import com.finploit.android.data.dto.CreateBillRequest
import com.finploit.android.data.dto.PayBody
import com.finploit.android.data.dto.UpdateBillRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BillsApi {
    @GET("bills")
    suspend fun getBills(@Query("month") month: String?): BillsResponse

    @POST("bills")
    suspend fun create(@Body body: CreateBillRequest): BillItemDto

    @PATCH("bills/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: UpdateBillRequest): BillItemDto

    @DELETE("bills/{id}")
    suspend fun delete(@Path("id") id: Int)

    @PATCH("bills/{id}/pay")
    suspend fun pay(@Path("id") id: Int, @Body body: PayBody): BillItemDto

    @PATCH("bills/{id}/unpay")
    suspend fun unpay(@Path("id") id: Int): BillItemDto
}
