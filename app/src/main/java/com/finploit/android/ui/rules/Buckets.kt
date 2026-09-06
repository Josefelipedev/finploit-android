package com.finploit.android.ui.rules

import androidx.compose.ui.graphics.Color
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.WarningAmber

/**
 * O vocabulário dos baldes, num sítio só.
 *
 * É partilhado por três ecrãs — as regras, as contas a pagar e o cartão do
 * Dashboard — e uma cor que só combina em dois é pior do que não ter cor
 * nenhuma. Os nomes vêm do servidor em inglês (`needs`/`wants`/`savings`)
 * porque são chaves; só aqui é que viram português.
 */
object Buckets {
    val ALL = listOf("needs", "wants", "savings")

    fun label(bucket: String?): String = when (bucket) {
        "needs" -> "Necessidades"
        "wants" -> "Desejos"
        "savings" -> "Poupança"
        else -> "Por classificar"
    }

    /** O singular, para caber num chip de linha ou num botão. */
    fun short(bucket: String?): String = when (bucket) {
        "needs" -> "Necessidade"
        "wants" -> "Desejo"
        "savings" -> "Poupança"
        else -> "?"
    }

    fun color(bucket: String?): Color = when (bucket) {
        "needs" -> GreenPrimary
        "wants" -> WarningAmber
        "savings" -> IncomeGreen
        // O que não tem balde é cinzento de propósito: não é uma quarta
        // categoria, é a ausência de resposta.
        else -> TextDisabled
    }

    fun hint(bucket: String?): String = when (bucket) {
        "needs" -> "O que não se evita: casa, comida, transporte, saúde, prestações."
        "wants" -> "O que se escolhe: restaurantes, viagens, subscrições, lazer."
        "savings" -> "O que fica: metas, investimentos e o que sobra na conta."
        else -> ""
    }
}

/** As cores do estado de uma regra. Verde cumprida, âmbar por um triz, vermelho quebrada. */
object RuleStatusStyle {
    fun color(status: String): Color = when (status) {
        "ok" -> IncomeGreen
        "close" -> WarningAmber
        "broken" -> com.finploit.android.ui.theme.ExpenseRed
        else -> TextDisabled
    }

    fun word(status: String): String = when (status) {
        "ok" -> "Cumprida"
        "close" -> "Por um triz"
        "broken" -> "Quebrada"
        // "Sem dados" e não "Falhou": não saber avaliar é um veredicto, não um
        // erro — e dizer "0%, quebrada" seria uma acusação inventada.
        else -> "Sem dados"
    }
}
