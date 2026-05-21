package com.finploit.android.data.dto

data class ShoppingListDto(
    val id: Int,
    val name: String,
    val userId: Int?,
    val items: List<ShoppingItemDto> = emptyList(),
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
