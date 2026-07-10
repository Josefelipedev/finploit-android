package com.finploit.android.data.dto

data class BankAccountDto(
    val id: Int,
    val currency: String = "BRL",
    val accountNumber: String? = null,
    val bankName: String = "",
    val agency: String? = null,
    val balance: Double = 0.0,
    val iconName: String? = null,
    val isArchived: Boolean = false,
    val userId: Int = 0,
)

data class CreateBankAccountRequest(
    val bankName: String,
    val accountNumber: String? = null,
    val agency: String? = null,
    val balance: Double? = null,
    val currency: String,
    val iconName: String? = null,
)

data class UpdateBankAccountRequest(
    val bankName: String? = null,
    val accountNumber: String? = null,
    val agency: String? = null,
    val balance: Double? = null,
    val currency: String? = null,
    val iconName: String? = null,
)
