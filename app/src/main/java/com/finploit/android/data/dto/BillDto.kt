package com.finploit.android.data.dto

data class BillsResponse(
    val month: String = "",
    val items: List<BillItemDto> = emptyList(),
    val totalPending: Double = 0.0,
    val totalPaid: Double = 0.0,
)

data class BillItemDto(
    val id: Int = 0,
    val description: String = "",
    val amount: Double = 0.0,
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
