package com.finploit.android.data.repository

import com.finploit.android.data.api.BankAccountApi
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.CreateBankAccountRequest
import com.google.gson.JsonNull
import com.google.gson.JsonObject
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
        creditLimit: Double?,
        creditUsed: Double?,
        currency: String,
        iconName: String? = null,
    ): Result<BankAccountDto> = runCatching {
        api.create(
            CreateBankAccountRequest(
                bankName = bankName,
                accountNumber = accountNumber,
                agency = agency,
                balance = balance,
                creditLimit = creditLimit,
                creditUsed = creditUsed,
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
        creditLimit: Double? = null,
        creditUsed: Double? = null,
        currency: String? = null,
        iconName: String? = null,
    ): Result<BankAccountDto> = runCatching {
        val body = JsonObject().apply {
            bankName?.let { addProperty("bankName", it) }
            accountNumber?.let { addProperty("accountNumber", it) }
            agency?.let { addProperty("agency", it) }
            balance?.let { addProperty("balance", it) }
            currency?.let { addProperty("currency", it) }
            iconName?.let { addProperty("iconName", it) }
            // Ao contrário de uma data class serializada pelo Gson padrão, o
            // JsonObject preserva o null: apagar o campo no formulário volta a
            // pôr o limite em "não informado", em vez de manter o valor antigo.
            if (creditLimit == null) add("creditLimit", JsonNull.INSTANCE)
            else addProperty("creditLimit", creditLimit)
            if (creditUsed == null) add("creditUsed", JsonNull.INSTANCE)
            else addProperty("creditUsed", creditUsed)
        }
        api.update(
            id,
            body,
        )
    }

    suspend fun delete(id: Int): Result<Unit> = runCatching {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
    }
}
