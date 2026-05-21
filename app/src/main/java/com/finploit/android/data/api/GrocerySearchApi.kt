package com.finploit.android.data.api

import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.GrocerySearchResponseDto
import com.finploit.android.data.dto.NearbyStoreDto
import com.finploit.android.data.dto.NearestPostalCodeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GrocerySearchApi {

    @GET("grocery-search/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("supermarkets") supermarkets: String? = null,
        @Query("postalCode") postalCode: String? = null,
        @Query("maxResults") maxResults: Int = 5,
    ): GrocerySearchResponseDto

    @POST("grocery-search/enrich")
    suspend fun enrich(@Body request: EnrichRequest): List<EnrichedShoppingItemDto>

    @GET("grocery-search/stores-nearby")
    suspend fun storesNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 15.0,
        @Query("chains") chains: String? = null,
    ): List<NearbyStoreDto>

    @GET("grocery-search/nearest-postal-code")
    suspend fun nearestPostalCode(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): NearestPostalCodeDto

}

data class EnrichRequest(
    val items: List<EnrichItem>,
    val supermarkets: List<String>? = null,
    val postalCode: String? = null,
)

data class EnrichItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val estimatedPrice: Double,
)
