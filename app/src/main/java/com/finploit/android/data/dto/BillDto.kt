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
)

data class BillCurrencyTotalDto(
    val currency: String = "BRL",
    val amount: Double = 0.0,
)

data class BillItemDto(
    val id: Int = 0,
    val description: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double? = null,
    val currency: String = "BRL",
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val dueDate: String = "",
    val status: String = "pending",
    val paidAt: String? = null,
    val overdue: Boolean = false,
    val carriedOver: Boolean = false,
) {
    val isPaid: Boolean get() = status == "paid"
}

data class PayBody(
    val amount: Double? = null,
)
