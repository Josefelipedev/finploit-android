package com.finploit.android.data.dto

import com.google.gson.annotations.SerializedName

data class GroceryProductDto(
    val source: String,
    val url: String,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val currency: String = "EUR",
    @SerializedName("unit_price") val unitPrice: Double? = null,
    val unit: String? = null,
    val quantity: Double? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val availability: String = "unknown",
)

data class GrocerySearchResponseDto(
    val query: String,
    val products: List<GroceryProductDto> = emptyList(),
    val cached: Boolean = false,
    val total: Int = 0,
)

data class EnrichedShoppingItemDto(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "un",
    val estimatedPrice: Double = 0.0,
    val products: List<GroceryProductDto> = emptyList(),
    val bestPrice: Double? = null,
    val bestSource: String? = null,
)

data class NearbyStoreDto(
    val id: String,
    val chain: String,
    val name: String,
    val address: String,
    val city: String,
    val region: String,
    val postalCode: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
)

data class NearestPostalCodeDto(
    val postalCode: String,
)
