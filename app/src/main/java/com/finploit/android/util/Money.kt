package com.finploit.android.util

/**
 * Dinheiro escrito à mão, em português.
 *
 * Os campos de valor faziam cada um a sua coisa, e nenhuma delas certa:
 *
 * - uns filtravam a escrita para dígitos e ponto — a **vírgula era deitada
 *   fora**, portanto `1.160,56` virava `1.160` e o `toDoubleOrNull` devolvia
 *   **1,16**, gravado sem aviso;
 * - outros aceitavam a vírgula e faziam `replace(',', '.')` — com `1.160,56`
 *   isso dá `1.160.56`, que não é número nenhum: o `toDoubleOrNull` devolve
 *   `null` e o valor desaparecia (ou o botão ficava desativado sem explicar
 *   porquê).
 *
 * A regra é a do **último separador**: se o que vem depois dele tem 1 ou 2
 * dígitos, é a parte decimal; tudo o resto são milhares e desaparece. É assim
 * que `1.160` dá 1160 (e não 1,16) e `1.16` dá 1,16.
 *
 * É a mesma regra do `parseAmountInput` da web (`utils/money.ts`) — os dois
 * clientes têm de concordar sobre o que a mesma pessoa escreveu.
 */
fun parseAmountInput(raw: String): Double? {
    val cleaned = raw.trim().filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
    if (cleaned.isEmpty()) return null

    val lastSeparator = maxOf(cleaned.lastIndexOf(','), cleaned.lastIndexOf('.'))
    if (lastSeparator == -1) return cleaned.toDoubleOrNull()

    val decimals = cleaned.length - lastSeparator - 1
    return if (decimals in 1..2) {
        val integerPart = cleaned.substring(0, lastSeparator).replace(",", "").replace(".", "")
        "$integerPart.${cleaned.substring(lastSeparator + 1)}".toDoubleOrNull()
    } else {
        // Sem parte decimal plausível (ex.: "1.160"): tudo milhares.
        cleaned.replace(",", "").replace(".", "").toDoubleOrNull()
    }
}

/** O que se deixa escrever num campo de dinheiro (a vírgula inclusive). */
fun filterAmountInput(raw: String): String =
    raw.filter { it.isDigit() || it == '.' || it == ',' }

/** Arredonda a cêntimos, sem o lixo do vírgula flutuante. */
fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
