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
                    val text = buildReportText(state)
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
                                    Text("€%.2f".format(state.totalIncome), color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Despesas", color = TextDisabled, fontSize = 12.sp)
                                    Text("€%.2f".format(state.totalExpense), color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Balanço", color = TextDisabled, fontSize = 12.sp)
                                    val balance = state.totalIncome - state.totalExpense
                                    Text("€%.2f".format(balance), color = if (balance >= 0) IncomeGreen else ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("${state.transactions.size} transações", color = TextDisabled, fontSize = 12.sp)
                        }
                    }
                }

                // Category breakdown
                if (state.byCategory.isNotEmpty()) {
                    item {
                        Text("Despesas por categoria", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    items(state.byCategory.entries.toList()) { (cat, amount) ->
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
                                    Text(cat, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("$pct% das despesas", color = TextDisabled, fontSize = 11.sp)
                                }
                                Text("€%.2f".format(amount), color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

private fun buildReportText(state: MonthlyReportState): String = buildString {
    val monthName = MONTH_NAMES[state.selectedMonth - 1]
    appendLine("📊 Relatório FinPloit — $monthName ${state.selectedYear}")
    appendLine("=".repeat(40))
    appendLine("✅ Receitas: €%.2f".format(state.totalIncome))
    appendLine("❌ Despesas: €%.2f".format(state.totalExpense))
    val balance = state.totalIncome - state.totalExpense
    appendLine("💰 Balanço: €%.2f".format(balance))
    appendLine()
    appendLine("📁 Por categoria:")
    state.byCategory.forEach { (cat, amt) ->
        appendLine("  • $cat: €%.2f".format(amt))
    }
    appendLine()
    appendLine("${state.transactions.size} transações no período")
}
