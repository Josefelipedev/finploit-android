package com.finploit.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.MonthForecastDto
import com.finploit.android.data.dto.TransactionDto
import com.finploit.android.ui.components.PeriodChips
import com.finploit.android.ui.rules.RulesCard
import com.finploit.android.ui.theme.currencyConfigByCode
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GradientEnd
import com.finploit.android.ui.theme.GradientStart
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.CurrencyConfig
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.currencyConfigByCode
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddRecurringClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    /** Abre o Orçamento já na aba da Regra — é o que o cartão promete. */
    onRulesClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onScanReceiptClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOverflowMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
    ) {
        TopAppBar(
            title = {
                Text(
                    "FinPloit",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 20.sp,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundDark,
                titleContentColor = TextPrimary,
            ),
            actions = {
                // Pesquisa — ação primária sempre visível
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisa", tint = TextSecondary)
                }
                // Notificações — sempre visível
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "Próximas contas", tint = TextSecondary)
                }
                // Perfil — sempre visível
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Default.Person, contentDescription = "Perfil", tint = TextSecondary)
                }
                // Menu de overflow (⋮) para acções secundárias
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        containerColor = CardBackground,
                    ) {
                        DashboardMenuItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "Calendário",
                            onClick = { showOverflowMenu = false; onCalendarClick() },
                        )
                        DashboardMenuItem(
                            icon = Icons.Default.PieChart,
                            label = "Relatório mensal",
                            onClick = { showOverflowMenu = false; onReportClick() },
                        )
                        // "Orçamento" e já não "Limites de orçamento": o ecrã
                        // passou a ter três andares — os limites do mês, o
                        // plano do ano e a regra. O "Planeamento" que estava
                        // aqui foi dissolvido: era um beco a que só se chegava
                        // por este menu, e as abas dele foram para onde já
                        // havia casa (Metas, Orçamento, Análise).
                        DashboardMenuItem(
                            icon = Icons.Default.Wallet,
                            label = "Orçamento e regras",
                            onClick = { showOverflowMenu = false; onBudgetClick() },
                        )
                        DashboardMenuItem(
                            icon = Icons.Default.CameraAlt,
                            label = "Digitalizar recibo",
                            onClick = { showOverflowMenu = false; onScanReceiptClick() },
                        )
                        DashboardMenuItem(
                            icon = Icons.Default.Refresh,
                            label = "Actualizar",
                            onClick = { showOverflowMenu = false; viewModel.loadDashboard() },
                            tint = GreenPrimary,
                        )
                    }
                }
            }
        )

        PeriodChips(
            selected = uiState.period,
            onSelect = viewModel::setPeriod,
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Não foi possível carregar", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.error ?: "", color = TextDisabled, fontSize = 13.sp)
                }
            }
            uiState.data != null -> {
                val data = uiState.data!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    item {
                        BalanceCard(
                            balance = data.totalBalance,
                            income = data.totalIncome,
                            expense = data.totalExpense,
                        )
                    }

                    // Sem taxa, o total acima soma moeda com moeda pelo valor
                    // nativo. Dizê-lo aqui, e não só no relatório.
                    data.unconvertedCurrencies?.takeIf { it.isNotEmpty() }?.let { moedas ->
                        item {
                            Text(
                                "⚠️ ${moedas.joinToString(", ")} sem taxa de câmbio: o total é aproximado.",
                                color = TextDisabled,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    uiState.forecast?.let { forecast ->
                        item { MonthForecastCard(forecast) }
                    }

                    // A regra não obedece ao período escolhido em cima, e é de
                    // propósito: compara-se sempre com os últimos meses
                    // FECHADOS. Um período de duas semanas daria uma divisão
                    // que não quer dizer nada.
                    item(key = "rules-card") { RulesCard(onOpenRules = onRulesClick) }

                    if (data.stats.revenueLastWeek > 0 || data.stats.expenseLastWeek > 0) {
                        item {
                            WeekStatsRow(
                                weekIncome = data.stats.revenueLastWeek,
                                weekExpense = data.stats.expenseLastWeek,
                            )
                        }
                    }

                    item {
                        Text(
                            "Últimas Transações",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    if (data.transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Nenhuma transação ainda", color = TextSecondary, fontSize = 15.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Adicione sua primeira transação", color = TextDisabled, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(data.transactions) { tx ->
                            TransactionItem(tx)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Double, income: Double, expense: Double) {
    val currencyConfig = LocalCurrencyConfig.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(GradientStart, GradientEnd))
                )
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // "Saldo Total" prometia o dinheiro que se tem; o número é
                // `totalIncome - totalExpense` do período, que é outra coisa. A
                // web mudou o mesmo rótulo (B3) — o saldo das contas bancárias,
                // esse, é um valor informado à mão e não acompanha lançamentos.
                Text("Saldo Líquido", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatCurrency(balance, currencyConfig),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balance >= 0) IncomeGreen else ExpenseRed,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(label = "Receitas", value = income, isIncome = true)
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(TextDisabled.copy(alpha = 0.4f))
                    )
                    StatItem(label = "Despesas", value = expense, isIncome = false)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Double, isIncome: Boolean) {
    val color = if (isIncome) IncomeGreen else ExpenseRed
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val currencyConfig = LocalCurrencyConfig.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(formatCurrency(value, currencyConfig), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/**
 * "O que me sobra depois de pagar o que falta este mês?"
 *
 * Era a pergunta para que as contas a pagar existem, e não tinha resposta fora
 * do ecrã delas: o resumo em cima olha só para o realizado. Os números vêm
 * somados do servidor (`GET /finance/forecast`), já convertidos.
 *
 * Diz no subtítulo que é **sempre o mês corrente** — na web, onde há seletor de
 * período, isso era essencial; aqui mantém-se para os dois clientes contarem a
 * mesma história.
 */
@Composable
private fun MonthForecastCard(forecast: MonthForecastDto) {
    val currencyConfig = forecast.displayCurrency
        ?.let { currencyConfigByCode(it) }
        ?: LocalCurrencyConfig.current
    val nada = forecast.pending.expense == 0.0 && forecast.pending.income == 0.0
    // O realizado e o pendente convertem-se em separado: basta um deles ter
    // ficado por converter para o saldo previsto ser uma soma de moedas.
    val semTaxa = forecast.unconvertedCurrencies.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (semTaxa.isNotEmpty()) {
                Text(
                    "⚠️ ${semTaxa.joinToString(", ")} sem taxa de câmbio: a previsão é aproximada.",
                    color = TextDisabled,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Este mês, até ao fim",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "É sempre o mês corrente",
                        color = TextDisabled,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    forecast.month,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            if (nada) {
                Text(
                    "Não há contas por pagar nem valores a receber até ao fim do mês. " +
                        "O saldo previsto é o realizado: " +
                        formatCurrency(forecast.realized.balance, currencyConfig) + ".",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ForecastFigure("Realizado", forecast.realized.balance, TextPrimary, currencyConfig)
                    ForecastFigure("A pagar", forecast.pending.expense, ExpenseRed, currencyConfig)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ForecastFigure("A receber", forecast.pending.income, IncomeGreen, currencyConfig)
                    ForecastFigure(
                        "Sobra prevista",
                        forecast.projectedBalance,
                        if (forecast.projectedBalance >= 0) GreenPrimary else ExpenseRed,
                        currencyConfig,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ForecastFigure(
    label: String,
    value: Double,
    color: Color,
    currencyConfig: CurrencyConfig,
) {
    Column(Modifier.weight(1f)) {
        Text(label, color = TextDisabled, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            formatCurrency(value, currencyConfig),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun WeekStatsRow(weekIncome: Double, weekExpense: Double) {
    val currencyConfig = LocalCurrencyConfig.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Esta semana", color = TextDisabled, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatCurrency(weekIncome, currencyConfig), color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatCurrency(weekExpense, currencyConfig), color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(tx: TransactionDto) {
    val isIncome = tx.tag == "income"
    val color = if (isIncome) IncomeGreen else ExpenseRed
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    // Os totais em cima vêm convertidos; cada linha continua no valor original,
    // por isso o símbolo é o da moeda do lançamento.
    val currencyConfig = currencyConfigByCode(tx.currency ?: LocalCurrencyConfig.current.code)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                val subtitle = buildString {
                    if (!tx.category.isNullOrBlank()) append("${tx.category} · ")
                    append("${tx.date} · ${tx.time}")
                }
                Text(subtitle, color = TextDisabled, fontSize = 12.sp)
            }
            Text(
                text = formatCurrency(tx.amount, currencyConfig),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

private fun formatCurrency(value: Double, config: CurrencyConfig): String = config.format(value)

@Composable
private fun DashboardMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = TextSecondary,
) {
    DropdownMenuItem(
        text = { Text(label, color = TextPrimary, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
        colors = MenuDefaults.itemColors(textColor = TextPrimary),
    )
}
