package com.finploit.android.data.dto

data class ShoppingListDto(
    val id: Int,
    val name: String,
    val userId: Int?,
    /** Compra fechada (já virou despesa). Null = ainda aberta. */
    val closedAt: String? = null,
    /** Lançamento criado ao fechar a compra. */
    val financeId: Int? = null,
    val items: List<ShoppingItemDto> = emptyList(),
) {
    val isClosed: Boolean get() = closedAt != null

    /** O que vira despesa ao fechar: só os comprados, scraper à frente do manual. */
    val purchasedTotal: Double
        get() = items.filter { it.purchased }.sumOf { it.scrapedPrice ?: it.price ?: 0.0 }
}

/** Corpo do fecho: o valor é somado no servidor, o cliente só escolhe onde e quando. */
data class ClosePurchaseRequest(
    val categoryId: Int? = null,
    /** YYYY-MM-DD; ausente = hoje. */
    val referenceDate: String? = null,
)

data class ShoppingItemDto(
    val id: Int,
    val name: String,
    val quantity: Double,
    val unit: String?,
    val price: Double?,
    val purchased: Boolean,
    val shoppingListId: Int,
    val supermarket: String? = null,
    val scrapedPrice: Double? = null,
    val scrapedAt: String? = null,
)

data class CreateShoppingListRequest(val name: String)

data class CreateShoppingItemRequest(
    val listId: Int,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
)

data class CreateOrUpdateShoppingItemRequest(
    val itemId: Int? = null,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val shoppingListId: Int,
)

data class UpdateItemStatusRequest(val purchased: Boolean)

data class ShoppingItemImportDto(
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double? = null,
)

data class CreateListWithItemsRequest(
    val name: String,
    val items: List<ShoppingItemImportDto>,
)

data class EnrichPricesResponse(
    val list: ShoppingListDto,
    val enriched: Int,
    val failed: List<String>,
)

data class MonthlyPlanRequest(
    val budget: Double? = null,
    val notes: String? = null,
)

data class MonthlyPlanResponse(
    val list: ShoppingListDto,
    val totalEstimate: Double?,
    val weeklyBudget: Double?,
    val savingsSummary: String?,
    val tips: String?,
)

data class StorePriceDto(
    val supermarket: String,
    val name: String,
    val brand: String?,
    val price: Double,
    val unit: String?,
    val availability: String,
)
