package com.finploit.android.data.repository

import com.finploit.android.data.api.RecurringApi
import com.finploit.android.data.dto.CreateRecurringRequest
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.data.dto.SettleRecurringResultDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(private val api: RecurringApi) {
    suspend fun getAll(): Result<List<RecurringTransactionDto>> = runCatching { api.getAll() }

    suspend fun create(request: CreateRecurringRequest): Result<RecurringTransactionDto> =
        runCatching { api.create(request) }

    suspend fun update(id: Int, request: CreateRecurringRequest): Result<RecurringTransactionDto> =
        runCatching { api.update(id, request) }

    /** "Paguei tudo": liquida de uma vez o que falta do parcelamento. */
    suspend fun settle(id: Int): Result<SettleRecurringResultDto> =
        runCatching { api.settle(id) }

    suspend fun delete(id: Int): Result<Unit> = runCatching {
        api.delete(id)
        Unit
    }
}
