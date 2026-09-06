package com.finploit.android.data.dto

data class BillsResponse(
    val month: String = "",
    val items: List<BillItemDto> = emptyList(),
    val totalPending: Double = 0.0,
    val totalPaid: Double = 0.0,
    val displayCurrency: String? = null,
    val rateDate: String? = null,
    val byCurrency: List<BillCurrencyTotalDto>? = null,
    val unconvertedCurrencies: List<String>? = null,
    val expense: BillTypeTotalsDto = BillTypeTotalsDto(),
    val income: BillTypeTotalsDto = BillTypeTotalsDto(),
    val projectedBalance: Double? = null,
    val realizedBalance: Double? = null,
    /**
     * O que fica em cada conta bancária depois de pagar o que falta. Nulo num
     * mês já fechado: a previsão parte do saldo de hoje e não sabe responder
     * pelo passado.
     */
    val accounts: BillsForecastDto? = null,
    /**
     * A despesa do mês repartida pelos baldes da regra — pago e por pagar.
     * O que não tem balde fica em `unclassified`, à parte e não dentro de um
     * deles: é o que impede o total de parecer completo quando não está.
     */
    val byBucket: BillsByBucketDto? = null,
)

/** O mês visto pelos baldes: o que dá para cortar, e o que não dá. */
data class BillsByBucketDto(
    val needs: Double = 0.0,
    val wants: Double = 0.0,
    val savings: Double = 0.0,
    val unclassified: Double = 0.0,
)

/** A previsão por conta bancária + o que ainda não tem conta atribuída. */
data class BillsForecastDto(
    val items: List<AccountForecastDto> = emptyList(),
    val unassigned: UnassignedForecastDto = UnassignedForecastDto(),
    /** Moedas somadas sem conversão — os saldos previstos são aproximados. */
    val unconvertedCurrencies: List<String>? = null,
)

/** A fila dos próximos meses, já convertida pelo servidor. */
data class MonthlyBillsForecastDto(
    val months: List<BillMonthForecastDto> = emptyList(),
    val heaviest: String? = null,
    val relief: String? = null,
    val displayCurrency: String = "BRL",
    val rateDate: String? = null,
    val unconvertedCurrencies: List<String> = emptyList(),
    /** "couple" | "mine" — de quem é a fila que o servidor somou. */
    val scope: String = "couple",
    /** Falso num workspace de uma pessoa só: não há duas vistas a escolher. */
    val isCouple: Boolean = false,
)

data class BillMonthForecastDto(
    val month: String = "",
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val net: Double = 0.0,
)

/**
 * Uma conta bancária e o que lhe vai acontecer até ao fim do mês.
 *
 * Os valores vêm na moeda **da própria conta** (não na de exibição): uma conta
 * em euros fala em euros, e formatá-los com a moeda do perfil escrevia o
 * símbolo errado por cima do número certo.
 */
data class AccountForecastDto(
    val id: Int = 0,
    val bankName: String = "",
    val currency: String = "BRL",
    val iconName: String? = null,
    val ownerId: Int = 0,
    val ownerName: String? = null,
    /** O que o banco tem hoje (saldo derivado). */
    val currentBalance: Double = 0.0,
    /** Entradas que ainda não caíram (salários, sobretudo). */
    val incoming: Double = 0.0,
    /** Contas desta conta por pagar (inclui as atrasadas). */
    val outgoing: Double = 0.0,
    /** currentBalance + incoming − outgoing. */
    val projectedBalance: Double = 0.0,
    val billCount: Int = 0,
)

/** Pendentes sem conta bancária dita — na moeda de exibição. */
data class UnassignedForecastDto(
    val incoming: Double = 0.0,
    val outgoing: Double = 0.0,
    val count: Int = 0,
    val currency: String = "BRL",
)

data class BillTypeTotalsDto(
    val pending: Double = 0.0,
    val paid: Double = 0.0,
)

data class BillCurrencyTotalDto(
    val currency: String = "BRL",
    val amount: Double = 0.0,
)

data class BillItemDto(
    val id: Int = 0,
    /** Dono da conta — o workspace é do casal, e o ecrã filtra por pessoa. */
    val userId: Int? = null,
    val recurringId: Int? = null,
    val description: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double? = null,
    val currency: String = "BRL",
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    /** O balde da regra a que esta conta pertence. Nulo = ninguém sabe. */
    val bucket: String? = null,
    /** "manual" | "guess" | "unknown" — um palpite não é uma decisão. */
    val bucketSource: String = "unknown",
    /** Conta bancária de onde sai (ou onde entra). Nulo = não foi dito. */
    val accountId: Int? = null,
    val dueDate: String = "",
    val status: String = "pending",
    val type: String = "expense",
    val paidAt: String? = null,
    val overdue: Boolean = false,
    val carriedOver: Boolean = false,
    val installment: Int? = null,
    val installments: Int? = null,
    val until: String? = null,
) {
    val isPaid: Boolean get() = status == "paid"
    val isIncome: Boolean get() = type == "income"
}

data class PayBody(
    val amount: Double? = null,
)

data class CreateBillRequest(
    val description: String,
    val amount: Double,
    val dueDate: String,
    val type: String,
    val currency: String? = null,
    val categoryId: Int? = null,
    val accountId: Int? = null,
)

data class UpdateBillRequest(
    val description: String? = null,
    val amount: Double? = null,
    val dueDate: String? = null,
    val categoryId: Int? = null,
    /** `null` desliga a conta bancária (a conta volta ao balde "sem conta"). */
    val accountId: Int? = null,
)
