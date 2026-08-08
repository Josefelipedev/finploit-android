package com.finploit.android.ui.recurring

import com.finploit.android.data.dto.RecurringTransactionDto
import java.util.Calendar
import java.util.Date

/**
 * Quanto uma recorrente pesa por mês.
 *
 * As semanais usam 52/12 e não 4: doze meses de quatro semanas são 48 semanas,
 * e um encargo semanal contado assim ficava um mês inteiro mais barato por ano.
 * Mesma regra do `monthlyEquivalent` do servidor (`planning/projection.ts`) e
 * da web — dois números diferentes para o mesmo compromisso é o que não pode
 * acontecer.
 *
 * Frequências que o formulário não grava (a diária, só do bot) valem zero:
 * melhor não pesar do que pesar por um valor inventado.
 */
fun monthlyEquivalent(frequency: String, amount: Double): Double = when (frequency) {
    "monthly" -> amount
    "weekly" -> amount * 52 / 12
    "yearly" -> amount / 12
    else -> 0.0
}

/** Hoje como "yyyy-MM-dd", no fuso do aparelho. */
private fun todayKey(now: Date): String {
    val cal = Calendar.getInstance().apply { time = now }
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

/**
 * A recorrente já acabou — não há mais nada a pagar nem a receber por ela.
 *
 * Duas formas de acabar: a data de fim passou, ou já se pagaram todas as
 * parcelas contratadas (`executedCount` vem do servidor a contar as contas
 * mesmo pagas, não é o contador da coluna, que nunca foi incrementado).
 *
 * A data compara-se como texto: em ISO a ordem alfabética é a ordem
 * cronológica, e assim não se converte um "2026-08-31" para instante nenhum —
 * que é onde um fuso à frente transforma o último dia no primeiro do seguinte.
 */
fun RecurringTransactionDto.isFinished(now: Date = Date()): Boolean {
    val fim = endDate?.take(10)
    if (fim != null && fim.length == 10 && fim < todayKey(now)) return true
    val total = occurrences ?: 0
    if (total > 0 && (executedCount ?: 0) >= total) return true
    return false
}

/**
 * O que ainda falta pagar de um parcelamento, na moeda da recorrente.
 *
 * `null` para uma assinatura sem fim: não há total contratado, e mostrar zero
 * diria que já não se deve nada — quando na verdade se paga para sempre.
 *
 * Trava em zero porque quem pagou acima do previsto (o ecrã de Contas
 * permite-o) não fica com um "falta pagar" negativo a descontar do resto.
 */
fun RecurringTransactionDto.remainingTotal(): Double? {
    val total = contractedTotal ?: return null
    return (total - (paidTotal ?: 0.0)).coerceAtLeast(0.0)
}

/**
 * Dá para dizer "paguei tudo" a esta recorrente?
 *
 * Só faz sentido onde existe um fim: uma subscrição sem número de parcelas
 * paga-se para sempre e não há "tudo" nenhum a liquidar. E não se quita o que
 * já está quitado — nem uma recorrente que já terminou. Mesma regra da web
 * (`utils/recurring.ts`).
 */
fun RecurringTransactionDto.canSettle(now: Date = Date()): Boolean {
    val falta = remainingTotal() ?: return false
    return falta > 0 && !isFinished(now)
}

/** O somatório das recorrentes ativas de UMA moeda. */
data class RecurringTotals(
    val currency: String,
    val expenseMonthly: Double,
    val incomeMonthly: Double,
    val remaining: Double,
    val contracted: Double,
    val paid: Double,
    val installments: Int,
    val openEnded: Int,
) {
    val leftoverMonthly: Double get() = incomeMonthly - expenseMonthly
}

/**
 * Os totais das recorrentes, **agrupados por moeda**.
 *
 * O Android não tem taxas de câmbio (a web busca-as em `GET /currency/rates`),
 * por isso não há como somar reais com euros sem inventar um número. Uma linha
 * por moeda diz a verdade sem precisar de taxa nenhuma — e um casal de uma só
 * moeda, que é o caso normal, vê exatamente uma linha.
 *
 * As já terminadas ficam de fora: um financiamento pago não é compromisso, e
 * mantê-lo no total mensal fazia a app pedir dinheiro que já ninguém deve.
 */
fun recurringTotals(
    transactions: List<RecurringTransactionDto>,
    fallbackCurrency: String,
): List<RecurringTotals> {
    val ativas = transactions.filterNot { it.isFinished() }
    if (ativas.isEmpty()) return emptyList()

    return ativas
        .groupBy { it.currency ?: fallbackCurrency }
        .map { (currency, list) ->
            var despesa = 0.0
            var receita = 0.0
            var falta = 0.0
            var contratado = 0.0
            var pago = 0.0
            var parcelamentos = 0
            var semFim = 0

            for (tx in list) {
                val mensal = monthlyEquivalent(tx.frequency, tx.amount)
                if (tx.type == "income") receita += mensal else despesa += mensal
                if (tx.type == "income") continue

                val resto = tx.remainingTotal()
                if (resto == null) {
                    // Assinatura sem fim: não há total a somar, mas há que
                    // dizer que existe — senão o "falta pagar" parece o
                    // compromisso todo.
                    semFim++
                    continue
                }
                parcelamentos++
                falta += resto
                contratado += tx.contractedTotal ?: 0.0
                pago += tx.paidTotal ?: 0.0
            }

            RecurringTotals(
                currency = currency,
                expenseMonthly = despesa,
                incomeMonthly = receita,
                remaining = falta,
                contracted = contratado,
                paid = pago,
                installments = parcelamentos,
                openEnded = semFim,
            )
        }
        .sortedBy { it.currency }
}
