package com.finploit.android.data.dto

data class PantryItemDto(
    val id: Int,
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val category: String? = null,
)

data class UpsertPantryItemRequest(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val category: String? = null,
)
