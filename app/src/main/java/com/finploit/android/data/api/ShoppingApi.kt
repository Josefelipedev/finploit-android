package com.finploit.android.data.api

import com.finploit.android.data.dto.ClosePurchaseRequest
import com.finploit.android.data.dto.CreateListWithItemsRequest
import com.finploit.android.data.dto.CreateOrUpdateShoppingItemRequest
import com.finploit.android.data.dto.EnrichPricesResponse
import com.finploit.android.data.dto.MonthlyPlanRequest
import com.finploit.android.data.dto.MonthlyPlanResponse
import com.finploit.android.data.dto.ShoppingItemDto
import com.finploit.android.data.dto.ShoppingListDto
import com.finploit.android.data.dto.StorePriceDto
import com.finploit.android.data.dto.UpdateItemStatusRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ShoppingApi {
    @GET("shopping")
    suspend fun getLists(): List<ShoppingListDto>

    @GET("shopping/list/{id}")
    suspend fun getListById(@Path("id") id: Int): ShoppingListDto

    @POST("shopping/list")
    suspend fun createList(@Body body: Map<String, String>): ShoppingListDto

    @POST("shopping/list-with-items")
    suspend fun createListWithItems(@Body body: CreateListWithItemsRequest): ShoppingListDto

    @PUT("shopping/list/{id}")
    suspend fun updateList(@Path("id") id: Int, @Body body: Map<String, String>): ShoppingListDto

    @DELETE("shopping/list/{id}")
    suspend fun deleteList(@Path("id") id: Int)

    /** Fecha a compra: os itens comprados viram uma despesa. */
    @POST("shopping/list/{id}/close")
    suspend fun closePurchase(
        @Path("id") id: Int,
        @Body body: ClosePurchaseRequest,
    ): ShoppingListDto

    /** Reabre a compra e apaga a despesa criada. */
    @POST("shopping/list/{id}/reopen")
    suspend fun reopenPurchase(@Path("id") id: Int): ShoppingListDto

    @POST("shopping/shopping-item")
    suspend fun createOrUpdateItem(@Body request: CreateOrUpdateShoppingItemRequest): ShoppingItemDto

    @PATCH("shopping/item/{itemId}")
    suspend fun updateItemStatus(
        @Path("itemId") itemId: Int,
        @Body body: UpdateItemStatusRequest,
    ): ShoppingItemDto

    @DELETE("shopping/item/{id}")
    suspend fun deleteItem(@Path("id") id: Int)

    @POST("shopping/enrich-prices/{listId}")
    suspend fun enrichPrices(@Path("listId") listId: Int): EnrichPricesResponse

    @GET("shopping/prices/{itemName}")
    suspend fun getPricesByStore(@Path("itemName") itemName: String): List<StorePriceDto>

    @POST("shopping/monthly-plan")
    suspend fun generateMonthlyPlan(@Body request: MonthlyPlanRequest): MonthlyPlanResponse
}
