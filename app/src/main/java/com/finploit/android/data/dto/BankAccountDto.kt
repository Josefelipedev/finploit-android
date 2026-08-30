package com.finploit.android.data.dto

/**
 * O que a conta é. Antes disto o único sinal de "isto é um cartão" era ter
 * limite preenchido, e o formulário não tinha onde marcar o tipo.
 */
enum class AccountType(val apiValue: String, val label: String) {
    CHECKING("CHECKING", "Conta corrente"),
    SAVINGS("SAVINGS", "Poupança"),
    CREDIT("CREDIT", "Cartão de crédito");

    companion object {
        fun from(value: String?): AccountType =
            entries.firstOrNull { it.apiValue == value } ?: CHECKING
    }
}

data class BankAccountDto(
    val id: Int,
    val currency: String = "BRL",
    val accountNumber: String? = null,
    val bankName: String = "",
    val agency: String? = null,
    /** Corrente, poupança ou cartão. O servidor devolve sempre um. */
    val accountType: String? = null,
    /**
     * Saldo INICIAL — o ponto de partida escrito à mão (C5). O que se mostra é
     * o `currentBalance`; este campo sozinho nunca acompanhou movimento nenhum.
     */
    val balance: Double = 0.0,
    /** O mesmo que `balance`, com o nome que diz o que é. */
    val initialBalance: Double? = null,
    /** Ponto de partida + o que entrou − o que saiu, calculado no servidor. */
    val currentBalance: Double? = null,
    /** Limite informado. Nunca participa do saldo. */
    val creditLimit: Double? = null,
    /** Quanto do limite já está em dívida. */
    val creditUsed: Double? = null,
    /** `creditLimit − creditUsed`, quando os dois se sabem. */
    val creditAvailable: Double? = null,
    /** O que os lançamentos ligados a esta conta somam. */
    val movements: AccountMovementsDto? = null,
    val iconName: String? = null,
    val isArchived: Boolean = false,
    val userId: Int = 0,
)

data class AccountMovementsDto(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val count: Int = 0,
)

data class CreateBankAccountRequest(
    val bankName: String,
    val accountNumber: String? = null,
    val agency: String? = null,
    val accountType: String? = null,
    val balance: Double? = null,
    val creditLimit: Double? = null,
    /** Quanto do limite já está em dívida. */
    val creditUsed: Double? = null,
    /** `creditLimit − creditUsed`, quando os dois se sabem. */
    val creditAvailable: Double? = null,
    val currency: String,
    val iconName: String? = null,
)

data class UpdateBankAccountRequest(
    val bankName: String? = null,
    val accountNumber: String? = null,
    val agency: String? = null,
    val accountType: String? = null,
    val balance: Double? = null,
    val creditLimit: Double? = null,
    /** Quanto do limite já está em dívida. */
    val creditUsed: Double? = null,
    /** `creditLimit − creditUsed`, quando os dois se sabem. */
    val creditAvailable: Double? = null,
    val currency: String? = null,
    val iconName: String? = null,
)
