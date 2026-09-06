package com.finploit.android.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.RulesSummaryDto
import com.finploit.android.ui.theme.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * As regras, em quatro números, na primeira página.
 *
 * O ecrã das regras vive no Orçamento e responde por inteiro; este cartão só
 * responde a "está tudo bem?" e leva lá quem quiser saber mais. É a ligação
 * que faltava — sem ela, a regra era uma coisa que só existia para quem se
 * lembrasse de a ir ver.
 *
 * Mostra a **pior** regra e não a primeira: um cartão que dissesse "reserva de
 * emergência: cumprida" enquanto os desejos estão 40% acima do alvo estaria a
 * mentir por omissão.
 */
@Composable
fun RulesCard(
    viewModel: RulesSummaryViewModel = hiltViewModel(),
    onOpenRules: () -> Unit,
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val resumo = summary ?: return
    val currency = currencyConfigByCode(resumo.displayCurrency)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenRules),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "A TUA REGRA",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${resumo.split.needsPct}/${resumo.split.wantsPct}/${resumo.split.savingsPct} →",
                    color = GreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (!resumo.verdict.hasIncome) {
                Text(
                    "Sem receita registada nos últimos meses fechados — a regra é uma " +
                        "proporção do que entra, e ainda não há de quê.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            } else {
                resumo.verdict.buckets.forEach { balde ->
                    val pct = balde.actualPct ?: 0.0
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Buckets.label(balde.bucket), color = TextSecondary, fontSize = 12.sp)
                            Text(
                                "${pct.roundToInt()}% de ${balde.targetPct}%",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(CardElevated),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth((min(100.0, pct.coerceAtLeast(0.0)) / 100.0).toFloat())
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Buckets.color(balde.bucket)),
                            )
                            // A marca do alvo: sem ela, a percentagem só se
                            // compara com o alvo lendo os dois números.
                            Box(
                                Modifier.fillMaxWidth(min(100, balde.targetPct) / 100f).fillMaxHeight(),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Box(Modifier.width(2.dp).fillMaxHeight().background(TextPrimary.copy(alpha = 0.5f)))
                            }
                        }
                    }
                }
            }

            resumo.worst?.let { pior ->
                HorizontalDividerThin()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(pior.label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    RuleStatusChip(pior.status)
                }
                Text(pior.message, color = TextDisabled, fontSize = 11.sp)
                if (resumo.total > 1) {
                    Text(
                        "${resumo.ok} de ${resumo.total} regras cumpridas.",
                        color = TextDisabled,
                        fontSize = 11.sp,
                    )
                }
            }

            // Um veredicto incompleto tem de o dizer aqui também: quem só olha
            // para o Dashboard nunca saberia que ele está a contar por alto.
            if (resumo.pendingCategories > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningAmber.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "${resumo.pendingCategories} " +
                            (if (resumo.pendingCategories == 1) "categoria" else "categorias") +
                            " por confirmar — até lá estes números contam por alto.",
                        color = WarningAmber,
                        fontSize = 11.sp,
                    )
                }
            }

            if (resumo.verdict.hasIncome && resumo.verdict.leftover < -0.005) {
                Text(
                    "Sai mais do que entra: ${currency.format(abs(resumo.verdict.leftover))} por mês.",
                    color = ExpenseRed,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun HorizontalDividerThin() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CardElevated))
}

/** O que o cartão precisa de saber, sem pagar o pedido inteiro das regras. */
typealias RulesSummary = RulesSummaryDto
