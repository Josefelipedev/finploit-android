package com.finploit.android.data.dto

data class CreateCategoryRequest(
    val name: String,
    val iconName: String? = null,
    val color: String? = null,
    val description: String? = null,
    val isActive: Boolean? = true,
    /** Receitas desta categoria contam como faturação da atividade (C6). */
    val isBusinessIncome: Boolean? = null,
)

data class UpdateCategoryRequest(
    val name: String? = null,
    val iconName: String? = null,
    val color: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null,
    val isBusinessIncome: Boolean? = null,
)
