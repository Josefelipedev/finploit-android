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

    /** O que vira despesa ao fechar: só os comprados, preço unitário × quantidade. */
    val purchasedTotal: Double
        get() = items.filter { it.purchased }.sumOf { it.lineTotal }
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
    /** Moeda em que a loja publicou o preço — é do país dela, não de quem olha. */
    val scrapedCurrency: String? = null,
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

/**
 * O que a linha vale: preço unitário × quantidade.
 *
 * A `quantity` não entrava em conta nenhuma até 27/ago/2026 — três pães a
 * 10,00 € somavam 10,00 €. Quantidade ausente ou sem sentido conta como 1.
 */
val ShoppingItemDto.lineTotal: Double
    get() = itemPrice * (quantity.takeIf { it > 0 } ?: 1.0)

/**
 * A partir de quantos dias o preço de um supermercado deixa de dizer alguma
 * coisa sobre hoje. As promoções mudam à semana.
 */
const val MAX_SCRAPED_AGE_DAYS = 7

/** Há quantos dias este preço foi lido na loja; null se nunca foi. */
val ShoppingItemDto.scrapedAgeInDays: Long?
    get() = scrapedAt?.let {
        runCatching {
            val lido = java.time.Instant.parse(if (it.endsWith("Z")) it else it + "Z")
            java.time.Duration.between(lido, java.time.Instant.now()).toDays()
        }.getOrNull()
    }

/**
 * true quando o valor desta linha é um preço de loja velho de mais para se
 * apresentar como se fosse de hoje. Continua a contar — é a única estimativa
 * que há —, mas quem o mostra tem de o dizer.
 */
val ShoppingItemDto.isStalePrice: Boolean
    get() = isEstimatedPrice && (scrapedAgeInDays ?: 0L) > MAX_SCRAPED_AGE_DAYS

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
