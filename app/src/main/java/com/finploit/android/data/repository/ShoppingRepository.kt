package com.finploit.android.data.repository

import com.finploit.android.data.api.ShoppingApi
import com.finploit.android.data.dto.CreateListWithItemsRequest
import com.finploit.android.data.dto.CreateOrUpdateShoppingItemRequest
import com.finploit.android.data.dto.EnrichPricesResponse
import com.finploit.android.data.dto.MonthlyPlanRequest
import com.finploit.android.data.dto.MonthlyPlanResponse
import com.finploit.android.data.dto.ShoppingItemDto
import com.finploit.android.data.dto.ShoppingItemImportDto
import com.finploit.android.data.dto.ShoppingListDto
import com.finploit.android.data.dto.StorePriceDto
import com.finploit.android.data.dto.UpdateItemStatusRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(private val api: ShoppingApi) {
    suspend fun getLists(): Result<List<ShoppingListDto>> = runCatching { api.getLists() }

    suspend fun getListById(id: Int): Result<ShoppingListDto> =
        runCatching { api.getListById(id) }

    suspend fun createList(name: String): Result<ShoppingListDto> =
        runCatching { api.createList(mapOf("name" to name)) }

    suspend fun updateList(id: Int, name: String): Result<ShoppingListDto> =
        runCatching { api.updateList(id, mapOf("name" to name)) }

    suspend fun deleteList(id: Int): Result<Unit> = runCatching { api.deleteList(id) }

    suspend fun createOrUpdateItem(request: CreateOrUpdateShoppingItemRequest): Result<ShoppingItemDto> =
        runCatching { api.createOrUpdateItem(request) }

    suspend fun toggleItem(itemId: Int, purchased: Boolean): Result<ShoppingItemDto> =
        runCatching { api.updateItemStatus(itemId, UpdateItemStatusRequest(purchased)) }

    suspend fun deleteItem(id: Int): Result<Unit> = runCatching { api.deleteItem(id) }

    suspend fun createListWithItems(name: String, items: List<ShoppingItemImportDto>): Result<ShoppingListDto> =
        runCatching { api.createListWithItems(CreateListWithItemsRequest(name, items)) }

    suspend fun enrichPrices(listId: Int): Result<EnrichPricesResponse> =
        runCatching { api.enrichPrices(listId) }

    suspend fun getPricesByStore(itemName: String): Result<List<StorePriceDto>> =
        runCatching { api.getPricesByStore(itemName) }

    suspend fun generateMonthlyPlan(budget: Double?, notes: String?): Result<MonthlyPlanResponse> =
        runCatching { api.generateMonthlyPlan(MonthlyPlanRequest(budget, notes)) }
}
