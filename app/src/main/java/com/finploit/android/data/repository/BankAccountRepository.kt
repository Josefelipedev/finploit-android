package com.finploit.android.data.repository

import com.finploit.android.data.api.BankAccountApi
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.CreateBankAccountRequest
import com.finploit.android.data.dto.UpdateBankAccountRequest
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankAccountRepository @Inject constructor(
    private val api: BankAccountApi,
) {
    suspend fun getAll(): Result<List<BankAccountDto>> =
        runCatching { api.getAll() }

    suspend fun create(
        bankName: String,
        accountNumber: String?,
        agency: String?,
        balance: Double?,
        currency: String,
        iconName: String? = null,
    ): Result<BankAccountDto> = runCatching {
        api.create(
            CreateBankAccountRequest(
                bankName = bankName,
                accountNumber = accountNumber,
                agency = agency,
                balance = balance,
                currency = currency,
                iconName = iconName,
            )
        )
    }

    suspend fun update(
        id: Int,
        bankName: String? = null,
        accountNumber: String? = null,
        agency: String? = null,
        balance: Double? = null,
        currency: String? = null,
        iconName: String? = null,
    ): Result<BankAccountDto> = runCatching {
        api.update(
            id,
            UpdateBankAccountRequest(
                bankName = bankName,
                accountNumber = accountNumber,
                agency = agency,
                balance = balance,
                currency = currency,
                iconName = iconName,
            )
        )
    }

    suspend fun delete(id: Int): Result<Unit> = runCatching {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
    }
}
