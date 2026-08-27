package com.finploit.android.ui.theme

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

data class CurrencyConfig(
    val code: String,
    val symbol: String,
    val flag: String,
    val label: String,
    val locale: Locale,
) {
    fun format(amount: Double): String = "%s %.2f".format(symbol, amount)
    fun formatCompact(amount: Double): String = "%s%.2f".format(symbol, amount)
}

val CURRENCY_OPTIONS = listOf(
    CurrencyConfig("BRL", "R$",  "🇧🇷", "Real Brasileiro",      Locale("pt", "BR")),
    CurrencyConfig("EUR", "€",   "🇪🇺", "Euro",                  Locale("pt", "PT")),
    CurrencyConfig("USD", "$",   "🇺🇸", "Dólar Americano",       Locale.US),
    CurrencyConfig("GBP", "£",   "🇬🇧", "Libra Esterlina",       Locale.UK),
    // O iene faltava aqui e existe na web e no enum do servidor: quem o
    // escolhesse caía no `first()` e via os seus ienes rotulados com R$.
    CurrencyConfig("JPY", "¥",   "🇯🇵", "Iene Japonês",          Locale.JAPAN),
    CurrencyConfig("AOA", "Kz",  "🇦🇴", "Kwanza Angolano",       Locale("pt", "AO")),
    CurrencyConfig("MZN", "MT",  "🇲🇿", "Metical Moçambicano",   Locale("pt", "MZ")),
    CurrencyConfig("CHF", "CHF", "🇨🇭", "Franco Suíço",          Locale("de", "CH")),
)

fun currencyConfigByCode(code: String): CurrencyConfig =
    CURRENCY_OPTIONS.find { it.code == code } ?: CURRENCY_OPTIONS.first()

val LocalCurrencyConfig = compositionLocalOf { CURRENCY_OPTIONS.first() }
