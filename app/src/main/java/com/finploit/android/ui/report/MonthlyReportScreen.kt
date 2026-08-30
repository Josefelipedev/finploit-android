package com.finploit.android.ui.report

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.*

private val MONTH_NAMES = arrayOf("Janeiro","Fevereiro","Março","Abril","Maio","Junho",
    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: MonthlyReportViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Os totais chegam já convertidos para a moeda do utilizador; o símbolo tem
    // de ser o dela — este ecrã escrevia "€" fixo mesmo para quem usa R$.
    val currency = currencyConfigByCode(state.displayCurrency ?: LocalCurrencyConfig.current.code)

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("Relatório Mensal", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary)
                }
            },
            actions = {
                IconButton(onClick = {
                    val text = buildReportText(state, currency)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, "Relatório FinPloit — ${MONTH_NAMES[state.selectedMonth-1]} ${state.selectedYear}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Partilhar relatório"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Partilhar", tint = GreenPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior", tint = TextSecondary)
            }
            Text(
                "${MONTH_NAMES[state.selectedMonth-1]} ${state.selectedYear}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês", tint = TextSecondary)
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Erro", color = ExpenseRed)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Summary card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Resumo do Mês", color = TextSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Receitas", color = TextDisabled, fontSize = 12.sp)
                                    Text(currency.format(state.totalIncome), color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Despesas", color = TextDisabled, fontSize = 12.sp)
                                    Text(currency.format(state.totalExpense), color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Balanço", color = TextDisabled, fontSize = 12.sp)
                                    val balance = state.totalIncome - state.totalExpense
                                    Text(currency.format(balance), color = if (balance >= 0) IncomeGreen else ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            // De quem é este número (C6). As partes somam os
                            // totais acima — é a mesma soma, guardada por dono.
                            if (state.byOwner.size > 1) {
                                Spacer(Modifier.height(10.dp))
                                state.byOwner.forEach { dono ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(primeiroNome(dono.name, dono.userId), color = TextDisabled, fontSize = 12.sp)
                                        Text(
                                            "${currency.format(dono.ganhos)}  ·  ${currency.format(dono.despesas)}",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("${state.transactionCount} transações", color = TextDisabled, fontSize = 12.sp)
                            if (state.unconvertedCurrencies.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "⚠️ ${state.unconvertedCurrencies.joinToString(", ")} sem taxa de câmbio: somadas pelo valor original.",
                                    color = TextDisabled,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                // Category breakdown
                if (state.byCategory.isNotEmpty()) {
                    item {
                        Text("Despesas por categoria", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    items(state.byCategory) { cat ->
                        val amount = cat.despesas
                        val pct = if (state.totalExpense > 0) (amount / state.totalExpense * 100).toInt() else 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.categoryName, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("$pct% das despesas", color = TextDisabled, fontSize = 11.sp)
                                    val donos = (cat.byOwner ?: emptyList()).filter { it.despesas > 0 }
                                    if (donos.size > 1) {
                                        Text(
                                            donos.joinToString("  ·  ") {
                                                "${primeiroNome(it.name, it.userId)} ${currency.format(it.despesas)}"
                                            },
                                            color = TextDisabled,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                                Text(currency.format(amount), color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

private fun buildReportText(state: MonthlyReportState, currency: CurrencyConfig): String = buildString {
    val monthName = MONTH_NAMES[state.selectedMonth - 1]
    appendLine("📊 Relatório FinPloit — $monthName ${state.selectedYear}")
    appendLine("=".repeat(40))
    appendLine("✅ Receitas: ${currency.format(state.totalIncome)}")
    appendLine("❌ Despesas: ${currency.format(state.totalExpense)}")
    val balance = state.totalIncome - state.totalExpense
    appendLine("💰 Balanço: ${currency.format(balance)}")
    appendLine()
    appendLine("📁 Por categoria:")
    state.byCategory.forEach { cat ->
        appendLine("  • ${cat.categoryName}: ${currency.format(cat.despesas)}")
    }
    appendLine()
    appendLine("${state.transactionCount} transações no período")
}

/** "Maria Silva" → "Maria". O apelido não cabe na linha e não desambigua nada. */
private fun primeiroNome(nome: String?, userId: Int): String =
    nome?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "#$userId"
