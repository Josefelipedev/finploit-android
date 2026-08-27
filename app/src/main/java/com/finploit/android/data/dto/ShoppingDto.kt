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

    /** O que vira despesa ao fechar: só os comprados, pela regra do `itemPrice`. */
    val purchasedTotal: Double
        get() = items.filter { it.purchased }.sumOf { it.itemPrice }
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

/**
 * Quanto conta um item — o espelho do `item-price.ts` da API.
 *
 * O preço do scraper ganhava ao escrito à mão (`scrapedPrice ?: price`), aqui e
 * em mais sete sítios entre os três clientes. A linha e o total discordavam: o
 * ecrã mostrava o "pão" a 10,00 € — o que a pessoa escreveu — e o total dizia
 * 0,15 €, o preço que o Pingo Doce tinha no site cinco semanas antes. Era esse
 * que fechar a compra lançava no livro-razão.
 *
 * Manda o preço **escrito à mão**: é uma afirmação da pessoa sobre o dinheiro
 * dela. O do scraper fica à vista na linha, como referência, e preenche quando
 * não há preço escrito — o formulário grava **0** (não `null`) quando não se
 * toca no campo, por isso o teste é `> 0`.
 */
val ShoppingItemDto.itemPrice: Double
    get() = price?.takeIf { it > 0 } ?: scrapedPrice ?: price ?: 0.0

/** true quando o valor deste item veio do scraper, não da pessoa. */
val ShoppingItemDto.isEstimatedPrice: Boolean
    get() = (price == null || price <= 0) && (scrapedPrice ?: 0.0) > 0

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
