package com.finploit.android.data.repository

import com.finploit.android.data.api.BillsApi
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.BillsResponse
import com.finploit.android.data.dto.CreateBillRequest
import com.finploit.android.data.dto.MonthlyBillsForecastDto
import com.finploit.android.data.dto.PayBody
import com.finploit.android.data.dto.UpdateBillRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillsRepository @Inject constructor(
    private val api: BillsApi,
) {
    suspend fun getBills(month: String?): Result<BillsResponse> =
        runCatching { api.getBills(month) }

    suspend fun getForecast(months: Int = 10): Result<MonthlyBillsForecastDto> =
        runCatching { api.getForecast(months) }

    suspend fun createBill(request: CreateBillRequest): Result<BillItemDto> =
        runCatching { api.create(request) }

    suspend fun updateBill(id: Int, request: UpdateBillRequest): Result<BillItemDto> =
        runCatching { api.update(id, request) }

    suspend fun deleteBill(id: Int): Result<Unit> =
        runCatching { api.delete(id) }

    suspend fun pay(id: Int, amount: Double? = null): Result<BillItemDto> =
        runCatching { api.pay(id, PayBody(amount)) }

    suspend fun unpay(id: Int): Result<BillItemDto> =
        runCatching { api.unpay(id) }
}
