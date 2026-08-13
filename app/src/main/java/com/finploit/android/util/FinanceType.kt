package com.finploit.android.util

/**
 * Os dois únicos tipos que se somam.
 *
 * Os ecrãs faziam `type == "income"` para os ganhos e mandavam **todo o resto**
 * para as despesas — ou seja, um tipo desconhecido entrava nas despesas. O
 * servidor faz o contrário: filtra `type in ('income','expense')` e deixa de
 * fora o que não sabe somar. Com os dois critérios em vigor ao mesmo tempo, os
 * mesmos dados davam números diferentes conforme o ecrã, e o do telemóvel era
 * sempre o das despesas infladas.
 *
 * Um lançamento com um tipo estranho não é uma despesa: é um lançamento que não
 * se sabe somar. Fica de fora, como no servidor e como na web
 * (`utils/finance-type.ts`).
 */
fun countableType(type: String?): String? =
    if (type == "income" || type == "expense") type else null

/** Sinal do lançamento para quem soma; `null` para o que não se sabe somar. */
fun signedForTotals(type: String?, amount: Double): Double? = when (countableType(type)) {
    "income" -> amount
    "expense" -> -amount
    else -> null
}

/**
 * Transferência: dinheiro que saiu do bolso disponível **sem ter sido gasto** —
 * um depósito numa meta, uma passagem entre contas próprias.
 *
 * O `countableType` já a deixa de fora dos totais, como deixa qualquer tipo que
 * não saiba somar. A diferença é que esta a app cria de propósito e tem de
 * saber mostrar: sem isto, um depósito de 250 € aparecia na lista a vermelho,
 * rotulado "Despesa", que é a leitura que se quis evitar (T6.1).
 */
fun isTransfer(type: String?): Boolean = type == "transfer"

/** O rótulo de um lançamento na lista. */
fun typeLabel(type: String?): String = when (type) {
    "income" -> "Receita"
    "transfer" -> "Transferência"
    else -> "Despesa"
}
