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
    val description: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double? = null,
    val currency: String = "BRL",
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val dueDate: String = "",
    val status: String = "pending",
    val type: String = "expense",
    val paidAt: String? = null,
    val overdue: Boolean = false,
    val carriedOver: Boolean = false,
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
)

data class UpdateBillRequest(
    val description: String? = null,
    val amount: Double? = null,
    val dueDate: String? = null,
    val categoryId: Int? = null,
)
