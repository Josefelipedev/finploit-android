package com.finploit.android.data.repository

import com.finploit.android.data.api.BillsApi
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.BillsResponse
import com.finploit.android.data.dto.PayBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillsRepository @Inject constructor(
    private val api: BillsApi,
) {
    suspend fun getBills(month: String?): Result<BillsResponse> =
        runCatching { api.getBills(month) }

    suspend fun pay(id: Int, amount: Double? = null): Result<BillItemDto> =
        runCatching { api.pay(id, PayBody(amount)) }

    suspend fun unpay(id: Int): Result<BillItemDto> =
        runCatching { api.unpay(id) }
}
