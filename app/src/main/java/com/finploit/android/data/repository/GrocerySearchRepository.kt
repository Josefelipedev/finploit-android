package com.finploit.android.data.repository

import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.api.EnrichRequest
import com.finploit.android.data.api.GrocerySearchApi
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.GrocerySearchResponseDto
import com.finploit.android.data.dto.NearbyStoreDto
import com.finploit.android.data.local.GrocerySearchCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrocerySearchRepository @Inject constructor(
    private val api: GrocerySearchApi,
    private val cache: GrocerySearchCache,
) {
    // In-memory cache survives ViewModel recreation within the same app session
    private var cachedEnrichedItems: Map<String, EnrichedShoppingItemDto> = emptyMap()

    fun getCachedEnrichedItems(): Map<String, EnrichedShoppingItemDto> = cachedEnrichedItems
    fun clearCachedEnrichedItems() { cachedEnrichedItems = emptyMap() }

    suspend fun search(
        query: String,
        supermarkets: List<String>? = null,
        postalCode: String? = null,
        maxResults: Int = 5,
    ): Result<GrocerySearchResponseDto> {
        val key = cache.buildKey(query, supermarkets, postalCode)
        val cached = cache.get(key)
        if (cached != null) return Result.success(cached)
        return runCatching {
            api.search(query, supermarkets?.joinToString(","), postalCode, maxResults)
        }.also { result ->
            result.onSuccess { if (it.products.isNotEmpty()) cache.put(key, it) }
        }
    }

    suspend fun enrichItems(
        items: List<EnrichItem>,
        supermarkets: List<String>? = null,
        postalCode: String? = null,
    ): Result<List<EnrichedShoppingItemDto>> = runCatching {
        api.enrich(EnrichRequest(items, supermarkets, postalCode))
    }.also { result ->
        result.onSuccess { enriched ->
            cachedEnrichedItems = enriched.associateBy { it.name.trim().lowercase() }
        }
    }

    suspend fun getStoresNearby(
        lat: Double,
        lng: Double,
        radiusKm: Double = 15.0,
        chains: List<String>? = null,
    ): Result<List<NearbyStoreDto>> = runCatching {
        api.storesNearby(lat, lng, radiusKm, chains?.joinToString(","))
    }

    suspend fun getNearestPostalCode(lat: Double, lng: Double): Result<String> = runCatching {
        api.nearestPostalCode(lat, lng).postalCode
    }
}
