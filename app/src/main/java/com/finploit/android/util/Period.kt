package com.finploit.android.util

import java.time.LocalDate

/**
 * O recorte de tempo que um ecrã está a ver.
 *
 * Vivia dentro do `AnalysisViewModel`, mas o Dashboard precisa do mesmo — e
 * precisava por um motivo concreto: ele chamava `/finance/dashboard` **sem
 * datas**, e sem datas o `periodWhere` do servidor não filtra nada. O cartão do
 * telemóvel era "de sempre" e o da web era o do período escolhido: dois números
 * diferentes com o mesmo nome, no mesmo sítio dos dois clientes (B4).
 *
 * O default é 30 dias, que é o `defaultDateRange()` da web — os dois têm de
 * partir do mesmo sítio para o número bater.
 */
enum class Period(val label: String) {
    Last30Days("30 dias"),
    ThisMonth("Este mês"),
    ThisYear("Este ano"),
    All("Tudo");

    /** `null` no início e no fim significa "sem filtro", como a API espera. */
    fun range(today: LocalDate = LocalDate.now()): Pair<String?, String?> = when (this) {
        Last30Days -> today.minusDays(30).toString() to today.toString()
        ThisMonth -> today.withDayOfMonth(1).toString() to today.toString()
        ThisYear -> today.withDayOfYear(1).toString() to today.toString()
        All -> null to null
    }
}
