package com.finploit.android.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.BucketVerdictDto
import com.finploit.android.data.dto.RuleCategoryDto
import com.finploit.android.data.dto.RuleCheckDto
import com.finploit.android.data.dto.RuleMonthDto
import com.finploit.android.data.dto.StoredRuleDto
import com.finploit.android.ui.theme.*
import com.finploit.android.util.parseAmountInput
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * As regras do dinheiro.
 *
 * Os limites por categoria, ao lado, dizem quanto se pode gastar em cada
 * sítio. Isto está um andar acima e faz a pergunta que somar tectos nunca faz:
 * *o que entra está a ser dividido como se quis?*. Nenhum limite olha para o
 * rendimento, e é o rendimento que torna a pergunta respondível.
 *
 * Ordem do ecrã, e é deliberada: a regra escolhida, o veredicto (o que se veio
 * ver), o que ainda falta arrumar, as outras regras — e só no fim o trabalho
 * de classificar categorias. Quem abre isto vem ver como está, não vem arrumar.
 */
@Composable
fun RulesScreen(viewModel: RulesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overview = state.overview
    val currency = currencyConfigByCode(overview?.displayCurrency ?: LocalCurrencyConfig.current.code)

    if (overview == null) {
        Box(Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            if (state.isLoading) {
                CircularProgressIndicator(color = GreenPrimary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error ?: "Não foi possível carregar as regras.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Tentar de novo", color = GreenPrimary)
                    }
                }
            }
        }
        return
    }

    var showCustom by remember { mutableStateOf(false) }
    var draftNeeds by remember(overview.split) { mutableStateOf(overview.split.needsPct) }
    var draftWants by remember(overview.split) { mutableStateOf(overview.split.wantsPct) }
    var newRuleKind by remember { mutableStateOf<String?>(null) }

    val pendentes = overview.categories.filter { it.source != "manual" }
    val palpites = pendentes.count { it.source == "guess" }
    val porClassificar = pendentes.count { it.source == "unknown" }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.error?.let { erro ->
            Card(colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(erro, color = ExpenseRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = ExpenseRed)
                    }
                }
            }
        }

        if (overview.unconvertedCurrencies.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))) {
                Text(
                    "Faltam taxas de câmbio para ${overview.unconvertedCurrencies.joinToString(", ")}. " +
                        "Somar moedas sem converter dá um número plausível e errado.",
                    color = WarningAmber,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // ── Como divides o que entra ───────────────────────────────────────
        SectionCard("Como divides o que entra") {
            if (overview.split.isDefault) {
                Text(
                    "Ainda ninguém escolheu — está a valer a clássica.",
                    color = TextDisabled,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
            }
            overview.presets.forEach { preset ->
                val activo = !showCustom && overview.split.preset == preset.key
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activo) GreenPrimary.copy(alpha = 0.12f) else CardElevated)
                        .clickable(enabled = !state.isSaving) {
                            showCustom = false
                            viewModel.choosePreset(preset.key)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            preset.name,
                            color = if (activo) GreenPrimary else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(preset.description, color = TextSecondary, fontSize = 12.sp)
                    }
                    if (activo) {
                        Text("✓", color = GreenPrimary, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            TextButton(onClick = { showCustom = !showCustom }) {
                Text(
                    if (showCustom) "Esconder" else "Escrever as minhas percentagens",
                    color = GreenPrimary,
                    fontSize = 13.sp,
                )
            }

            if (showCustom) {
                val savings = 100 - draftNeeds - draftWants
                PercentSlider("Necessidades", draftNeeds) { valor ->
                    draftNeeds = valor
                    // As três somam 100 sempre. Empurrar a outra fatia para
                    // baixo evita um estado inválido que só o servidor
                    // recusaria, depois de já se ter arrastado.
                    if (draftNeeds + draftWants > 100) draftWants = 100 - draftNeeds
                }
                PercentSlider("Desejos", draftWants) { valor ->
                    draftWants = valor
                    if (draftNeeds + draftWants > 100) draftNeeds = 100 - draftWants
                }
                Text(
                    "Poupança: $savings% — é o que sobra das outras duas.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.saveCustomSplit(draftNeeds, draftWants)
                        showCustom = false
                    },
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = BackgroundDark),
                ) { Text("Guardar esta regra", fontWeight = FontWeight.SemiBold) }
            }
        }

        // ── O veredicto ────────────────────────────────────────────────────
        SectionCard("Como o teu dinheiro se divide") {
            Text(
                if (overview.basis.monthsCovered > 0) {
                    val n = overview.basis.monthsCovered
                    "Média de $n ${if (n == 1) "mês" else "meses"} " +
                        "(${mesPorExtenso(overview.basis.window.start)} a ${mesPorExtenso(overview.basis.window.end)})"
                } else {
                    "Sem histórico ainda"
                },
                color = TextDisabled,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(10.dp))

            if (!overview.verdict.hasIncome) {
                Text(
                    "Sem receita registada neste período. A regra é uma proporção do que " +
                        "entra: sem saber quanto entra, os valores abaixo são o que sai e mais " +
                        "nada — nenhuma percentagem seria verdadeira.",
                    color = WarningAmber,
                    fontSize = 12.sp,
                )
            } else {
                Text(
                    "Entram ${currency.format(overview.verdict.income)} por mês. É esta a base de " +
                        "que as percentagens falam.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))

            overview.verdict.buckets.forEach { balde ->
                BucketBar(balde, currency)
                Spacer(Modifier.height(12.dp))
            }

            if (overview.basis.partialMonth) {
                Text(
                    "Ainda não há um mês fechado, por isso a conta usa o mês corrente — que " +
                        "vai a meio. Vai parecer que se gasta menos do que se gasta.",
                    color = WarningAmber,
                    fontSize = 11.sp,
                )
            }

            if (overview.verdict.leftover < -0.005) {
                Text(
                    "Sai mais do que entra: ${currency.format(abs(overview.verdict.leftover))} por mês. " +
                        "Enquanto isto durar, a poupança acima é só o que se depositou de propósito.",
                    color = ExpenseRed,
                    fontSize = 12.sp,
                )
            }

            if (overview.history.size > 1) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "MÊS A MÊS — ESTÁ A MELHORAR?",
                    color = TextDisabled,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                History(overview.history, currency)
            }
        }

        // ── O que falta arrumar ────────────────────────────────────────────
        if (palpites > 0 || porClassificar > 0 || overview.uncategorizedAmount > 0.005) {
            SectionCard("O veredicto ainda não está completo", border = WarningAmber) {
                if (palpites > 0) {
                    Text(
                        "$palpites ${if (palpites == 1) "categoria entrou" else "categorias entraram"} " +
                            "por palpite do nome. Estão a contar, mas ninguém confirmou.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.acceptGuesses() },
                        enabled = !state.isSaving,
                    ) { Text("Aceitar os $palpites palpites", color = GreenPrimary, fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                }
                if (porClassificar > 0) {
                    Text(
                        "$porClassificar ${if (porClassificar == 1) "categoria não está" else "categorias não estão"} " +
                            "em balde nenhum — ficam de fora dos três acima, não escondidas dentro de um deles.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                if (overview.uncategorizedAmount > 0.005) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${currency.format(overview.uncategorizedAmount)}/mês saíram sem categoria " +
                            "nenhuma. Esses só se arrumam pondo-lhes uma categoria nos lançamentos.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // ── As outras regras ───────────────────────────────────────────────
        SectionCard("As tuas outras regras") {
            Text(
                "A divisão em três reparte tudo o que entra. Estas vigiam um número cada, e " +
                    "podem ser quantas quiseres.",
                color = TextDisabled,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(10.dp))

            overview.rules.forEach { regra ->
                StoredRuleRow(
                    regra = regra,
                    isSaving = state.isSaving,
                    onToggle = { viewModel.toggleRule(regra.id, !regra.isActive) },
                    onDelete = { viewModel.deleteRule(regra.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                RULE_FAMILIES.forEach { familia ->
                    OutlinedButton(onClick = { newRuleKind = familia.kind }, enabled = !state.isSaving) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(familia.name, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // ── Onde cada categoria cai ────────────────────────────────────────
        SectionCard("Onde cada categoria cai") {
            Text(
                "Toca no balde que já está escolhido para o desfazer e voltar ao palpite.",
                color = TextDisabled,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(10.dp))
            val lista = (if (pendentes.isEmpty()) overview.categories else pendentes)
                .sortedByDescending { it.monthlyAmount }
            if (lista.isEmpty()) {
                Text("Não há categorias de despesa ainda.", color = TextSecondary, fontSize = 13.sp)
            }
            lista.forEach { categoria ->
                CategoryRow(
                    categoria = categoria,
                    currency = currency,
                    isSaving = state.isSaving,
                    onPick = { balde -> categoria.categoryId?.let { viewModel.setBucket(it, balde) } },
                )
                HorizontalDivider(color = CardElevated)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    newRuleKind?.let { kind ->
        NewRuleDialog(
            kind = kind,
            currency = currency,
            onDismiss = { newRuleKind = null },
            onConfirm = { target, bucket ->
                viewModel.createRule(kind, target, bucket)
                newRuleKind = null
            },
        )
    }
}

// ── Peças ───────────────────────────────────────────────────────────────────

private data class RuleFamily(
    val kind: String,
    val name: String,
    val question: String,
    /** "pct" | "months" | "money" — a unidade que o servidor impõe a esta família. */
    val unit: String,
    val default: Double,
)

private val RULE_FAMILIES = listOf(
    RuleFamily("ceiling", "Tecto", "Isto não pode passar de uma fatia do que entra.", "pct", 30.0),
    RuleFamily("reserve", "Reserva", "Quantos meses de despesa quero ter guardados.", "months", 6.0),
    RuleFamily("savings_rate", "Taxa de poupança", "Que fatia do que entra quero poupar.", "pct", 20.0),
    RuleFamily("pay_yourself_first", "Pagar-me primeiro", "Quanto vai para a poupança antes de gastar.", "money", 200.0),
)

@Composable
private fun SectionCard(
    title: String,
    border: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                color = border ?: TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * Um balde: quanto devia ser, quanto é, e a diferença em dinheiro.
 *
 * A barra desenha o **real** contra o rendimento, com uma marca no alvo.
 * Enchê-la até ao alvo escondia exactamente o caso que interessa ver, que é o
 * de estar por cima dele.
 */
@Composable
private fun BucketBar(verdict: BucketVerdictDto, currency: CurrencyConfig) {
    val pct = verdict.actualPct
    val largura = min(100.0, (pct ?: 0.0).coerceAtLeast(0.0)) / 100f
    // Gastar acima do alvo é mau; poupar acima do alvo é bom. A mesma seta
    // para cima não pode ser vermelha nos dois casos.
    val bomQuandoAcima = verdict.bucket == "savings"
    val acima = verdict.deltaAmount > 0.005
    val abaixo = verdict.deltaAmount < -0.005

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                Buckets.label(verdict.bucket),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                buildString {
                    append(currency.format(verdict.actualAmount))
                    if (pct != null) append("  ${pct.roundToInt()}%")
                    append("  · alvo ${verdict.targetPct}%")
                },
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CardElevated),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(largura.toFloat())
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Buckets.color(verdict.bucket)),
            )
            // A marca do alvo por cima da barra. Sem ela, "54%" só se compara
            // com o alvo lendo o número — e o ponto de uma barra é não ter de
            // o ler.
            Box(
                Modifier
                    .fillMaxWidth(min(100, verdict.targetPct) / 100f)
                    .height(8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.width(2.dp).height(8.dp).background(TextPrimary.copy(alpha = 0.6f)))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            when {
                pct == null -> Buckets.hint(verdict.bucket)
                acima -> "${currency.format(abs(verdict.deltaAmount))} " +
                    if (bomQuandoAcima) "acima do alvo" else "a mais do que a regra pede"
                abaixo -> "${currency.format(abs(verdict.deltaAmount))} " +
                    if (bomQuandoAcima) "abaixo do alvo" else "de folga"
                else -> "Em cima do alvo."
            },
            color = when {
                pct == null -> TextDisabled
                acima -> if (bomQuandoAcima) IncomeGreen else ExpenseRed
                abaixo -> if (bomQuandoAcima) ExpenseRed else IncomeGreen
                else -> TextSecondary
            },
            fontSize = 11.sp,
        )
    }
}

/**
 * O que entrou e saiu em cada mês da janela.
 *
 * A média responde a "como está"; não responde a "está a melhorar?". É de
 * propósito o movimento de cada mês e não a regra de cada mês: o veredicto
 * precisa da média para não saltar com um mês atípico, mas o movimento é
 * factual e não precisa de suavização nenhuma.
 */
@Composable
private fun History(history: List<RuleMonthDto>, currency: CurrencyConfig) {
    val pico = history.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        history.forEach { mes ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.height(64.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        Modifier
                            .width(12.dp)
                            .fillMaxHeight((mes.income / pico).toFloat().coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(IncomeGreen.copy(alpha = 0.7f)),
                    )
                    Box(
                        Modifier
                            .width(12.dp)
                            .fillMaxHeight((mes.expense / pico).toFloat().coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(ExpenseRed.copy(alpha = 0.7f)),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(mesPorExtenso(mes.month), color = TextSecondary, fontSize = 10.sp)
                Text(
                    (if (mes.net >= 0) "+" else "") + currency.format(mes.net),
                    color = if (mes.net >= 0) IncomeGreen else ExpenseRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StoredRuleRow(
    regra: StoredRuleDto,
    isSaving: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardElevated)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    regra.check?.label ?: "Em pausa — não está a ser avaliada.",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                regra.check?.let {
                    Text(it.message, color = TextSecondary, fontSize = 11.sp)
                }
            }
            regra.check?.let { RuleStatusChip(it.status) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onToggle, enabled = !isSaving) {
                Text(if (regra.isActive) "Pausar" else "Retomar", color = TextSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = onDelete, enabled = !isSaving) {
                Text("Apagar", color = ExpenseRed, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RuleStatusChip(status: String) {
    val cor = RuleStatusStyle.color(status)
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(RuleStatusStyle.word(status), color = cor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CategoryRow(
    categoria: RuleCategoryDto,
    currency: CurrencyConfig,
    isSaving: Boolean,
    onPick: (String?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(categoria.name, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            when (categoria.source) {
                "guess" -> Text("palpite", color = TextDisabled, fontSize = 10.sp)
                "unknown" -> Text("por classificar", color = WarningAmber, fontSize = 10.sp)
            }
        }
        Text(
            if (categoria.monthlyAmount > 0.005) "${currency.format(categoria.monthlyAmount)}/mês"
            else "sem movimento no período",
            color = TextSecondary,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Buckets.ALL.forEach { balde ->
                val escolhido = categoria.bucket == balde
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (escolhido) Buckets.color(balde).copy(alpha = 0.2f) else CardElevated)
                        // Tocar no balde que já está escolhido desfá-lo: é a
                        // única maneira de voltar ao palpite sem um quarto
                        // botão a dizer "limpar".
                        .clickable(enabled = !isSaving) { onPick(if (escolhido) null else balde) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Buckets.short(balde),
                        color = if (escolhido) Buckets.color(balde) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (escolhido) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun PercentSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 13.sp)
            Text("$value%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = GreenPrimary,
                activeTrackColor = GreenPrimary,
                inactiveTrackColor = CardElevated,
            ),
        )
    }
}

@Composable
private fun NewRuleDialog(
    kind: String,
    currency: CurrencyConfig,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit,
) {
    val familia = RULE_FAMILIES.first { it.kind == kind }
    var texto by remember { mutableStateOf(familia.default.roundToInt().toString()) }
    var balde by remember { mutableStateOf("needs") }
    val valor = parseAmountInput(texto)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(familia.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(familia.question, color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                if (kind == "ceiling") {
                    Text("O que é que isto vigia?", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Buckets.ALL.forEach { b ->
                            val escolhido = balde == b
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (escolhido) GreenPrimary.copy(alpha = 0.2f) else CardElevated)
                                    .clickable { balde = b }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    Buckets.short(b),
                                    color = if (escolhido) GreenPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = {
                        Text(
                            when (familia.unit) {
                                "pct" -> "Percentagem do que entra"
                                "months" -> "Meses de despesa"
                                else -> "Valor por mês (${currency.symbol})"
                            },
                            color = TextSecondary,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = CardElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { valor?.let { onConfirm(it, if (kind == "ceiling") balde else null) } },
                // Uma percentagem acima de 100 é recusada pelo servidor; barrar
                // aqui poupa a ida e a mensagem de erro.
                enabled = valor != null && valor > 0 && (familia.unit != "pct" || valor <= 100),
            ) { Text("Guardar", color = GreenPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
    )
}

/** `2026-08` → `ago`. A chave atravessa a API; a legenda é para ler. */
internal fun mesPorExtenso(chave: String): String {
    val nomes = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    val mes = chave.split("-").getOrNull(1)?.toIntOrNull() ?: return chave
    return nomes.getOrNull(mes - 1) ?: chave
}
