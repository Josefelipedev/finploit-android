package com.finploit.android.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.*
import com.finploit.android.ui.theme.*
import com.finploit.android.util.filterAmountInput
import com.finploit.android.util.parseAmountInput
import java.util.Calendar
import kotlin.math.abs

/**
 * Planeamento dos próximos anos — a paridade do ecrã que a web ganhou primeiro.
 *
 * As quatro abas respondem a quatro perguntas diferentes: onde é que isto vai
 * dar (projeção), e se mudar alguma coisa (cenários), as metas cabem no que
 * sobra (metas) e quanto quero gastar em cada categoria no ano que vem (plano
 * anual). O servidor é que faz as contas todas; aqui só se desenha e se
 * recarrega o que voltou.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableStateOf(0) }

    // A moeda vem da resposta, não do ambiente: em casal, quem lê pode não ser
    // quem regista, e o servidor já converteu tudo para uma só.
    val currency = currencyConfigByCode(
        state.overview?.projection?.displayCurrency
            ?: state.yearPlan?.displayCurrency
            ?: LocalCurrencyConfig.current.code,
    )

    // O plano do ano é o único separador que não vem na visão geral.
    LaunchedEffect(tabIndex) {
        if (tabIndex == 3 && state.yearPlan == null) viewModel.loadYear(state.year)
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("Planeamento", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = GreenPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        val tabs = listOf("Projeção", "Cenários", "Metas", "Plano anual")
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = BackgroundDark,
            contentColor = GreenPrimary,
            indicator = { positions ->
                SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tabIndex]), color = GreenPrimary)
            },
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (tabIndex == index) GreenPrimary else TextDisabled,
                        fontSize = 13.sp,
                        fontWeight = if (tabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        state.error?.let { Banner(it, ExpenseRed) { viewModel.clearMessage() } }
        state.message?.let { Banner(it, GreenPrimary) { viewModel.clearMessage() } }

        if (state.isLoading && state.overview == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else {
            when (tabIndex) {
                0 -> ProjectionTab(state, currency)
                1 -> ScenariosTab(state, currency, viewModel)
                2 -> GoalsPaceTab(state, currency, viewModel)
                else -> YearPlanTab(state, currency, viewModel)
            }
        }
    }
}

@Composable
private fun Banner(text: String, color: androidx.compose.ui.graphics.Color, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = color, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("fechar", color = color.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

// ── Projeção ────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionTab(state: PlanningUiState, currency: CurrencyConfig) {
    val projection = state.overview?.projection ?: return
    val summary = projection.summary

    // Sem contas bancárias registadas o património de partida não é zero — é
    // desconhecido. Mostrar "0,00" fazia a projeção parecer a de quem não tem
    // nada; o que se projeta nesse caso é o que se acumula a partir de hoje.
    val netWorthUnknown =
        !projection.baseline.netWorthKnown && projection.scenario.startingNetWorth == null

    // O horizonte começa no mês seguinte, portanto o primeiro e o último ano
    // são pedaços de ano. Sem o dizer, um parecia render um terço do outro.
    val monthsPerYear = projection.months.groupingBy { it.month.take(4) }.eachCount()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(projection.scenario.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    projection.scenario.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = TextDisabled, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric(
                            "Hoje",
                            if (netWorthUnknown) "Por saber" else currency.format(summary.startBalance),
                            if (netWorthUnknown) TextDisabled else TextPrimary,
                        )
                        Metric(
                            if (netWorthUnknown) "Acumula em ${projection.scenario.horizonYears} anos"
                            else "Daqui a ${projection.scenario.horizonYears} anos",
                            currency.format(summary.endBalance),
                            if (summary.endBalance >= summary.startBalance) IncomeGreen else ExpenseRed,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric(
                            "Sobra por mês",
                            currency.format(summary.monthlySurplus),
                            if (summary.monthlySurplus >= 0) IncomeGreen else ExpenseRed,
                        )
                        Metric("Inflação", "${projection.scenario.inflationPct}% ao ano", TextSecondary)
                    }
                }
            }
        }

        if (netWorthUnknown) {
            item {
                Text(
                    "A app não sabe com quanto é que partes: não há contas bancárias registadas. " +
                        "Os saldos abaixo são o que se acumula daqui para a frente, não o teu património.",
                    color = TextDisabled,
                    fontSize = 11.sp,
                )
            }
        }

        if (projection.unconvertedCurrencies.isNotEmpty()) {
            item {
                Text(
                    "⚠️ ${projection.unconvertedCurrencies.joinToString(", ")} sem taxa de câmbio: os totais são aproximados.",
                    color = TextDisabled,
                    fontSize = 11.sp,
                )
            }
        }

        item { SectionTitle("Ano a ano") }

        val maxBalance = projection.years.maxOfOrNull { abs(it.endBalance) } ?: 0.0
        items(projection.years) { year ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val meses = monthsPerYear["${'$'}{year.year}"] ?: 12
                        Text(
                            if (meses < 12) "${'$'}{year.year}  ·  ${'$'}meses meses" else "${'$'}{year.year}",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            currency.format(year.endBalance),
                            color = if (year.endBalance >= 0) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Barra proporcional ao maior saldo do horizonte: dá a
                    // forma da curva sem precisar de um gráfico a sério.
                    LinearProgressIndicator(
                        progress = {
                            if (maxBalance > 0) (abs(year.endBalance) / maxBalance).toFloat().coerceIn(0f, 1f) else 0f
                        },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (year.endBalance >= 0) GreenPrimary else ExpenseRed,
                        trackColor = TextDisabled.copy(alpha = 0.15f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Entra ${currency.format(year.income)} · Sai ${currency.format(year.expense)} · Sobra ${currency.format(year.net)}",
                        color = TextDisabled,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (projection.baseline.commitments.isNotEmpty()) {
            item { SectionTitle("Contratado (recorrentes)") }
            item {
                Text(
                    "O que se sabe de certeza. Conta pelo valor cheio enquanto durar.",
                    color = TextDisabled,
                    fontSize = 11.sp,
                )
            }
            items(projection.baseline.commitments) { commitment ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(commitment.name, color = TextPrimary, fontSize = 13.sp)
                        Text(
                            commitment.endsAfter?.let { "até ${'$'}it" } ?: "sem fim",
                            color = TextDisabled,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        (if (commitment.type == "income") "+" else "−") +
                            currency.format(commitment.monthlyAmount),
                        color = if (commitment.type == "income") IncomeGreen else ExpenseRed,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (projection.baseline.lines.isNotEmpty()) {
            item { SectionTitle("O que varia (média do histórico)") }
            items(projection.baseline.lines) { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(line.name, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        (if (line.type == "income") "+" else "−") + currency.format(line.monthlyAmount),
                        color = TextDisabled,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        item {
            val window = projection.baseline.window
            Text(
                if (window != null)
                    "Base: ${'$'}{projection.baseline.commitments.size} contratos + média de " +
                        "${'$'}{projection.baseline.monthsCovered} meses de histórico " +
                        "(${'$'}{window.start} a ${'$'}{window.end})."
                else "Sem histórico suficiente para uma média.",
                color = TextDisabled,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun Metric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, color = TextDisabled, fontSize = 11.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

// ── Cenários ────────────────────────────────────────────────────────────────

@Composable
private fun ScenariosTab(
    state: PlanningUiState,
    currency: CurrencyConfig,
    viewModel: PlanningViewModel,
) {
    val scenarios = state.overview?.scenarios ?: emptyList()
    var editingScenario by remember { mutableStateOf<PlanScenarioDto?>(null) }
    var creatingScenario by remember { mutableStateOf(false) }
    var eventScenarioId by remember { mutableStateOf<Int?>(null) }
    var editingEvent by remember { mutableStateOf<PlanEventDto?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(
                onClick = { creatingScenario = true },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = BackgroundDark),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Novo cenário", fontWeight = FontWeight.Bold) }
        }

        if (scenarios.isEmpty()) {
            item {
                Text(
                    "Ainda não há cenários. Um cenário é um conjunto de premissas — inflação, crescimento — mais as alterações que se quer experimentar, sem mexer em lançamento nenhum.",
                    color = TextDisabled,
                    fontSize = 12.sp,
                )
            }
        }

        items(scenarios) { scenario ->
            val isSelected = state.selectedScenarioId == scenario.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.selectScenario(scenario.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) GreenPrimary.copy(alpha = 0.12f) else CardBackground,
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                scenario.name + if (scenario.isBaseline) "  ·  referência" else "",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                "${scenario.horizonYears} anos · inflação ${scenario.inflationPct}% · rendimento ${scenario.savingsReturnPct}%",
                                color = TextDisabled,
                                fontSize = 11.sp,
                            )
                        }
                        IconButton(onClick = { editingScenario = scenario }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextDisabled, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.deleteScenario(scenario.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }

                    if (scenario.events.isEmpty()) {
                        Text("Sem alterações — igual ao rumo actual.", color = TextDisabled, fontSize = 11.sp)
                    } else {
                        scenario.events.forEach { event ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(event.name, color = TextPrimary, fontSize = 13.sp)
                                    Text(
                                        "${frequencyLabel(event.frequency)} · desde ${event.startMonth}" +
                                            (event.endMonth?.let { " até $it" } ?: ""),
                                        color = TextDisabled,
                                        fontSize = 11.sp,
                                    )
                                }
                                Text(
                                    (if (event.type == "income") "+" else "−") + currency.format(event.amount),
                                    color = if (event.type == "income") IncomeGreen else ExpenseRed,
                                    fontSize = 13.sp,
                                )
                                IconButton(onClick = { editingEvent = event; eventScenarioId = scenario.id }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextDisabled, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteEvent(event.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    TextButton(onClick = { editingEvent = null; eventScenarioId = scenario.id }) {
                        Text("+ Alteração \"e se\"", color = GreenPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    if (creatingScenario || editingScenario != null) {
        ScenarioDialog(
            initial = editingScenario,
            onConfirm = { request ->
                viewModel.saveScenario(editingScenario?.id, request)
                creatingScenario = false
                editingScenario = null
            },
            onDismiss = { creatingScenario = false; editingScenario = null },
        )
    }

    eventScenarioId?.let { scenarioId ->
        EventDialog(
            initial = editingEvent,
            categories = state.categories,
            currency = currency,
            onConfirm = { request ->
                viewModel.saveEvent(scenarioId, editingEvent?.id, request)
                eventScenarioId = null
                editingEvent = null
            },
            onDismiss = { eventScenarioId = null; editingEvent = null },
        )
    }
}

private fun frequencyLabel(frequency: String): String = when (frequency) {
    "yearly" -> "Uma vez por ano"
    "once" -> "Uma única vez"
    else -> "Todos os meses"
}

@Composable
private fun ScenarioDialog(
    initial: PlanScenarioDto?,
    onConfirm: (ScenarioRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var horizon by remember { mutableStateOf((initial?.horizonYears ?: 5).toString()) }
    var inflation by remember { mutableStateOf((initial?.inflationPct ?: 2.0).toString()) }
    var incomeGrowth by remember { mutableStateOf((initial?.incomeGrowthPct ?: 0.0).toString()) }
    var savingsReturn by remember { mutableStateOf((initial?.savingsReturnPct ?: 0.0).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(if (initial == null) "Novo cenário" else "Editar cenário", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanningField(name, { name = it }, "Nome")
                PlanningField(description, { description = it }, "Descrição (opcional)")
                PlanningField(horizon, { horizon = it.filter { c -> c.isDigit() } }, "Horizonte (anos)", KeyboardType.Number)
                PlanningField(inflation, { inflation = filterAmountInput(it) }, "Inflação % ao ano", KeyboardType.Decimal)
                PlanningField(incomeGrowth, { incomeGrowth = filterAmountInput(it) }, "Crescimento do rendimento %", KeyboardType.Decimal)
                PlanningField(savingsReturn, { savingsReturn = filterAmountInput(it) }, "Rendimento das poupanças %", KeyboardType.Decimal)
                error?.let { Text(it, color = ExpenseRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { error = "O cenário tem de ter nome."; return@TextButton }
                val years = horizon.toIntOrNull()
                if (years == null || years < 1 || years > 30) {
                    error = "O horizonte tem de estar entre 1 e 30 anos."
                    return@TextButton
                }
                onConfirm(
                    ScenarioRequest(
                        name = name.trim(),
                        description = description.trim().ifBlank { null },
                        horizonYears = years,
                        inflationPct = parseAmountInput(inflation) ?: 0.0,
                        incomeGrowthPct = parseAmountInput(incomeGrowth) ?: 0.0,
                        savingsReturnPct = parseAmountInput(savingsReturn) ?: 0.0,
                    ),
                )
            }) { Text("Guardar", color = GreenPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDisabled) } },
    )
}

@Composable
private fun EventDialog(
    initial: PlanEventDto?,
    categories: List<FinanceCategoryDto>,
    currency: CurrencyConfig,
    onConfirm: (PlanEventRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(initial?.frequency ?: "monthly") }
    var startMonth by remember { mutableStateOf(initial?.startMonth ?: currentMonth()) }
    var endMonth by remember { mutableStateOf(initial?.endMonth ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var growsWithInflation by remember { mutableStateOf(initial?.growsWithInflation ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(if (initial == null) "Nova alteração" else "Editar alteração", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanningField(name, { name = it }, "O que é")
                FlowTypeSelector(type) { type = it }
                PlanningField(amount, { amount = filterAmountInput(it) }, "Valor (${currency.symbol})", KeyboardType.Decimal)
                CategoryPicker(
                    categories = categories,
                    selectedId = categoryId,
                    label = "Categoria (opcional)",
                    onSelect = { categoryId = it },
                )
                OptionRow(
                    label = "Com que frequência",
                    options = listOf("monthly" to "Todos os meses", "yearly" to "Uma vez por ano", "once" to "Uma única vez"),
                    selected = frequency,
                    onSelect = { frequency = it },
                )
                PlanningField(startMonth, { startMonth = it }, "Começa em (AAAA-MM)")
                PlanningField(endMonth, { endMonth = it }, "Acaba em (AAAA-MM, opcional)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = growsWithInflation,
                        onCheckedChange = { growsWithInflation = it },
                        colors = CheckboxDefaults.colors(checkedColor = GreenPrimary, uncheckedColor = TextDisabled),
                    )
                    Text("Acompanha a inflação", color = TextSecondary, fontSize = 12.sp)
                }
                error?.let { Text(it, color = ExpenseRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = parseAmountInput(amount)
                when {
                    name.isBlank() -> error = "A alteração tem de ter nome."
                    value == null || value <= 0 -> error = "Escreva um valor."
                    !isMonth(startMonth) -> error = "O mês de início tem de ser AAAA-MM."
                    endMonth.isNotBlank() && !isMonth(endMonth) -> error = "O mês de fim tem de ser AAAA-MM."
                    else -> onConfirm(
                        PlanEventRequest(
                            name = name.trim(),
                            type = type,
                            amount = value,
                            frequency = frequency,
                            startMonth = startMonth.trim(),
                            endMonth = endMonth.trim().ifBlank { null },
                            categoryId = categoryId,
                            growsWithInflation = growsWithInflation,
                        ),
                    )
                }
            }) { Text("Guardar", color = GreenPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDisabled) } },
    )
}

// ── Metas ───────────────────────────────────────────────────────────────────

@Composable
private fun GoalsPaceTab(
    state: PlanningUiState,
    currency: CurrencyConfig,
    viewModel: PlanningViewModel,
) {
    val plan = state.overview?.goalPlan ?: return
    var editing by remember { mutableStateOf<GoalPaceDto?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (plan.feasible) "As metas cabem no que sobra" else "As metas não cabem no que sobra",
                        color = if (plan.feasible) IncomeGreen else ExpenseRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Sobra por mês: ${currency.format(plan.monthlySurplus)}", color = TextSecondary, fontSize = 12.sp)
                    Text("Preciso por mês: ${currency.format(plan.totalRequiredMonthly)}", color = TextSecondary, fontSize = 12.sp)
                    Text("Por prometer: ${currency.format(plan.unallocatedMonthly)}", color = TextDisabled, fontSize = 12.sp)
                }
            }
        }

        if (plan.goals.isEmpty()) {
            item { Text("Ainda não há metas de longo prazo.", color = TextDisabled, fontSize = 12.sp) }
        }

        items(plan.goals) { goal ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { editing = goal },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(
                            when {
                                goal.overdue -> "fora de prazo"
                                goal.funded -> "no ritmo"
                                else -> "em falta"
                            },
                            color = when {
                                goal.overdue -> ExpenseRed
                                goal.funded -> IncomeGreen
                                else -> TextDisabled
                            },
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Falta ${currency.format(goal.remaining)} de ${currency.format(goal.targetValue)}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                    goal.requiredMonthly?.let {
                        Text("Precisa de ${currency.format(it)} por mês", color = TextDisabled, fontSize = 11.sp)
                    }
                    Text(
                        "Prometido: ${currency.format(goal.allocatedMonthly)} · prioridade ${goal.priority}",
                        color = TextDisabled,
                        fontSize = 11.sp,
                    )
                    if (goal.shortfallMonthly > 0) {
                        Text("Faltam ${currency.format(goal.shortfallMonthly)} por mês", color = ExpenseRed, fontSize = 11.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }

    editing?.let { goal ->
        GoalPaceDialog(
            goal = goal,
            currency = currency,
            onConfirm = { monthly, priority ->
                viewModel.saveGoalPace(goal.id, monthly, priority)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun GoalPaceDialog(
    goal: GoalPaceDto,
    currency: CurrencyConfig,
    onConfirm: (Double, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var monthly by remember { mutableStateOf(goal.plannedMonthly?.toString() ?: "") }
    var priority by remember { mutableStateOf(goal.priority.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(goal.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanningField(monthly, { monthly = filterAmountInput(it) }, "Quanto por mês (${currency.symbol})", KeyboardType.Decimal)
                PlanningField(priority, { priority = it.filter { c -> c.isDigit() } }, "Prioridade (maior = servida primeiro)", KeyboardType.Number)
                Text(
                    "Quanto maior o número, mais cedo a meta é servida quando o dinheiro não chega para todas. Prometer a uma meta não é despesa — é excedente reservado.",
                    color = TextDisabled,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(parseAmountInput(monthly) ?: 0.0, priority.toIntOrNull() ?: 1)
            }) { Text("Guardar", color = GreenPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDisabled) } },
    )
}

// ── Plano anual ─────────────────────────────────────────────────────────────

@Composable
private fun YearPlanTab(
    state: PlanningUiState,
    currency: CurrencyConfig,
    viewModel: PlanningViewModel,
) {
    var editing by remember { mutableStateOf<YearPlanItemDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    val plan = state.yearPlan

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.loadYear(state.year - 1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Ano anterior", tint = TextSecondary)
            }
            Text("${state.year}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = { viewModel.loadYear(state.year + 1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Ano seguinte", tint = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { creating = true }) {
                Text("+ Planear", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (state.isLoadingYear && plan == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                plan?.let { yearPlan ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Metric("Receita planeada", currency.format(yearPlan.totals.plannedIncome), IncomeGreen)
                            Metric("Despesa planeada", currency.format(yearPlan.totals.plannedExpense), ExpenseRed)
                        }
                    }
                    val planned = yearPlan.items.filter { it.plannedAmount != null }
                    val unplanned = yearPlan.items.filter { it.plannedAmount == null }

                    if (planned.isEmpty()) {
                        item {
                            Text(
                                "Nada planeado para ${yearPlan.year}. O que não tiver plano entra na projeção pela média do histórico.",
                                color = TextDisabled,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    items(planned) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { editing = item },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.categoryName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(
                                            if (item.type == "income") "receita" else "despesa",
                                            color = if (item.type == "income") IncomeGreen else ExpenseRed,
                                            fontSize = 11.sp,
                                        )
                                        item.note?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, color = TextDisabled, fontSize = 11.sp)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(currency.format(item.plannedAmount ?: 0.0), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        item.monthlyPlanned?.let {
                                            Text("${currency.format(it)} por mês", color = TextDisabled, fontSize = 11.sp)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteYearPlanItem(item.categoryId) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Já feito ${currency.format(item.realizedAmount)} · média ${currency.format(item.baselineAmount)}",
                                    color = TextDisabled,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }

                    if (unplanned.isNotEmpty()) {
                        item { SectionTitle("Sem plano (entram pela média)") }
                        items(unplanned) { item ->
                            Row(
                                Modifier.fillMaxWidth().clickable { editing = item }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.categoryName, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(currency.format(item.baselineAmount), color = TextDisabled, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }

    if (creating || editing != null) {
        YearPlanItemDialog(
            initial = editing,
            year = state.year,
            categories = state.categories,
            currency = currency,
            onConfirm = { categoryId, type, amount, note ->
                viewModel.saveYearPlanItem(categoryId, type, amount, note)
                creating = false
                editing = null
            },
            onDismiss = { creating = false; editing = null },
        )
    }
}

@Composable
private fun YearPlanItemDialog(
    initial: YearPlanItemDto?,
    year: Int,
    categories: List<FinanceCategoryDto>,
    currency: CurrencyConfig,
    onConfirm: (Int, String, Double, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var amount by remember { mutableStateOf(initial?.plannedAmount?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text(
                if (initial == null) "Planear categoria em $year" else "Editar o plano de $year",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowTypeSelector(type, enabled = initial == null) { type = it }
                CategoryPicker(
                    categories = categories,
                    selectedId = categoryId,
                    label = "Categoria",
                    // A linha é identificada por ano+categoria: trocá-la a meio
                    // de uma edição criaria outra e deixaria a antiga no plano.
                    enabled = initial == null,
                    onSelect = { categoryId = it },
                )
                PlanningField(amount, { amount = filterAmountInput(it) }, "Total do ano (${currency.symbol})", KeyboardType.Decimal)
                parseAmountInput(amount)?.takeIf { it > 0 }?.let {
                    Text("${currency.format(it / 12)} por mês", color = TextDisabled, fontSize = 11.sp)
                }
                PlanningField(note, { note = it }, "Observação (opcional)")
                error?.let { Text(it, color = ExpenseRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val id = categoryId
                val value = parseAmountInput(amount)
                when {
                    id == null -> error = "Escolha uma categoria."
                    value == null || value < 0 -> error = "Escreva um valor."
                    // Vazio vai como string vazia, não como nulo: o Gson não
                    // serializa nulos e a observação nunca chegaria a limpar.
                    else -> onConfirm(id, type, value, note.trim())
                }
            }) { Text("Guardar", color = GreenPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDisabled) } },
    )
}

// ── Peças partilhadas ───────────────────────────────────────────────────────

@Composable
private fun PlanningField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextDisabled) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenPrimary,
            unfocusedBorderColor = TextDisabled.copy(alpha = 0.5f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = GreenPrimary,
        ),
    )
}

@Composable
private fun FlowTypeSelector(
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    OptionRow(
        label = "Entra ou sai",
        options = listOf("expense" to "Sai (despesa)", "income" to "Entra (receita)"),
        selected = selected,
        enabled = enabled,
        onSelect = onSelect,
    )
}

@Composable
private fun OptionRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(label, color = TextDisabled, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            options.forEach { (value, text) ->
                val isSelected = value == selected
                Text(
                    text,
                    color = if (isSelected) BackgroundDark else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GreenPrimary else TextDisabled.copy(alpha = 0.15f))
                        .clickable(enabled = enabled) { onSelect(value) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * O tipo (receita/despesa) escolhe-se à parte porque a categoria não o traz:
 * o `FinanceCategoryDto` não tem campo `type`, tal como na web.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    categories: List<FinanceCategoryDto>,
    selectedId: Int?,
    label: String,
    enabled: Boolean = true,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedId }?.name ?: "Sem categoria"

    Column {
        Text(label, color = TextDisabled, fontSize = 11.sp)
        Box {
            Text(
                selectedName,
                color = if (enabled) TextPrimary else TextDisabled,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TextDisabled.copy(alpha = 0.12f))
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = CardBackground,
            ) {
                DropdownMenuItem(
                    text = { Text("Sem categoria", color = TextDisabled, fontSize = 13.sp) },
                    onClick = { onSelect(null); expanded = false },
                )
                categories.filter { it.isActive }.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name, color = TextPrimary, fontSize = 13.sp) },
                        onClick = { onSelect(category.id); expanded = false },
                    )
                }
            }
        }
        if (!enabled) {
            Text(
                "Para planear outra categoria, remova esta linha e crie uma nova.",
                color = TextDisabled,
                fontSize = 10.sp,
            )
        }
    }
}

private fun currentMonth(): String {
    val calendar = Calendar.getInstance()
    return "%04d-%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
}

private fun isMonth(value: String): Boolean = Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(value.trim())
