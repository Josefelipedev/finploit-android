package com.finploit.android.data.dto

/** Um limite de orçamento vindo do servidor (C1). */
data class BudgetLimitDto(
    val id: Int,
    val categoryId: Int,
    val categoryName: String?,
    /** Já convertido pelo servidor para a moeda de quem está a ler (C4). */
    val monthlyLimit: Double,
    val alertAt: Int = 80,
    /** Moeda em que o `monthlyLimit` acima vem — a de quem lê. */
    val currency: String? = null,
    /** O que foi mesmo escrito, na moeda em que foi escrito. */
    val originalMonthlyLimit: Double? = null,
    val originalCurrency: String? = null,
    /**
     * O que já saiu contra este tecto no mês, somado NO SERVIDOR à taxa do dia
     * de cada lançamento. Nulo num servidor antigo — e aí vale o recuo local.
     *
     * Havia três somas para a mesma pergunta (esta, a da web e a das dívidas).
     * Três sítios a somar é três sítios onde divergir.
     */
    val spent: Double? = null,
    /**
     * `manual` ou `food_budget`. Um tecto que nasceu da meta de alimentação não
     * se edita nem se apaga aqui — o servidor recusa, e oferecer o lápis seria
     * oferecer um caminho que acaba num erro.
     */
    val source: String? = null,
)

/** Quanto se gastou em comida este mês, contra a meta pessoal. */
data class FoodSpendDto(
    val budget: Double? = null,
    val spent: Double = 0.0,
    val delta: Double? = null,
    val usedPct: Double? = null,
    val status: String = "no_budget",
    val currency: String? = null,
)

data class SetBudgetLimitRequest(
    val monthlyLimit: Double,
    val alertAt: Int = 80,
)
