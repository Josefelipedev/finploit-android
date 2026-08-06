package com.finploit.android.ui.recurring

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.ui.components.OwnerChip
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.currencyConfigByCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(viewModel: RecurringViewModel, onBack: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Tocar num cartão abre o mesmo formulário da criação, em modo de edição.
    var editing by remember { mutableStateOf<RecurringTransactionDto?>(null) }
    editing?.let { alvo ->
        AddRecurringScreen(
            viewModel = viewModel,
            onDismiss = { editing = null },
            existing = alvo,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Recorrentes", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
            ),
            actions = {
                IconButton(onClick = viewModel::loadAll) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Green80)
                }
            }
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green80)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Erro", color = ExpenseRed)
            }
            uiState.transactions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma transação recorrente", color = Color.Gray)
                    Text("Use o + para adicionar", color = Color.Gray, fontSize = 13.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                // Quanto isto tudo custa por mês e quanto falta pagar até ao
                // fim dos contratos: a conta que a lista deixava para a cabeça
                // de quem lê, parcela a parcela.
                item { RecurringSummaryCard(uiState.transactions) }
                items(uiState.transactions, key = { it.id }) { tx ->
                    SwipeToDeleteRecurring(
                        tx = tx,
                        isDeleting = tx.id in uiState.deletingIds,
                        onDelete = { viewModel.delete(tx.id) },
                        onEdit = { editing = tx },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRecurring(
    tx: RecurringTransactionDto,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        }
    )

    LaunchedEffect(isDeleting) {
        if (!isDeleting) dismissState.reset()
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ExpenseRed.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White)
                }
            }
        },
    ) {
        RecurringCard(tx, onEdit)
    }
}

@Composable
private fun RecurringCard(tx: RecurringTransactionDto, onEdit: () -> Unit) {
    val isIncome = tx.type == "income"
    val color = if (isIncome) IncomeGreen else ExpenseRed
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    // Moeda NATIVA da recorrência; cai na moeda de exibição só se o backend não mandar
    val currency = tx.currency?.let { currencyConfigByCode(it) } ?: LocalCurrencyConfig.current

    val frequencyLabel = when (tx.frequency) {
        "daily" -> "Diário"
        "weekly" -> "Semanal"
        "monthly" -> "Mensal"
        "yearly" -> "Anual"
        else -> tx.frequency
    }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description ?: tx.type.replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FrequencyBadge(frequencyLabel)
                    // Quem lançou — só aparece no workspace do casal.
                    OwnerChip(tx.userId)
                    tx.dueDay?.takeIf { it > 0 }?.let {
                        Text("Vence dia $it", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currency.format(tx.amount),
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                // Num parcelamento, a parcela sozinha não diz de quanto é a
                // dívida: o total contratado vem calculado da API.
                tx.contractedTotal?.takeIf { (tx.occurrences ?: 0) > 0 }?.let { total ->
                    Text(
                        text = "${tx.executedCount ?: 0}/${tx.occurrences} · ${currency.format(total)}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                    )
                }
                // O que já foi pago vem somado do servidor, não de
                // parcela × pagamentos — que mente quando as parcelas diferem.
                tx.paidTotal?.takeIf { it > 0 }?.let { pago ->
                    Text(
                        text = "pago ${currency.format(pago)}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                    )
                }
                // O que ainda falta desembolsar. Sem isto, quem quer saber
                // quanto deve tinha de subtrair de cabeça o pago do contratado.
                tx.remainingTotal()?.let { falta ->
                    val quitada = if (isIncome) "recebida" else "quitada"
                    Text(
                        text = if (falta > 0) "falta ${currency.format(falta)}" else quitada,
                        color = if (falta > 0) TextSecondary else IncomeGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/**
 * O que as recorrentes custam, somado.
 *
 * Por moeda, e não convertido: o Android não tem taxas de câmbio, e somar
 * reais com euros dá um número plausível e errado. Um casal de uma só moeda,
 * que é o caso normal, vê exatamente um bloco.
 */
@Composable
private fun RecurringSummaryCard(transactions: List<RecurringTransactionDto>) {
    val totals = recurringTotals(transactions, LocalCurrencyConfig.current.code)
    if (totals.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            totals.forEachIndexed { index, t ->
                val currency = currencyConfigByCode(t.currency)

                if (totals.size > 1) {
                    Text(
                        "${currency.flag} ${t.currency}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCell("Despesa/mês", currency.format(t.expenseMonthly), ExpenseRed)
                    SummaryCell("Receita/mês", currency.format(t.incomeMonthly), IncomeGreen)
                    SummaryCell(
                        "Sobra/mês",
                        currency.format(t.leftoverMonthly),
                        if (t.leftoverMonthly < 0) ExpenseRed else IncomeGreen,
                    )
                }

                Column {
                    Text("Falta pagar", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        if (t.installments > 0) currency.format(t.remaining) else "—",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        buildString {
                            if (t.installments > 0) {
                                append("${currency.format(t.paid)} de ${currency.format(t.contracted)} pagos")
                            }
                            if (t.openEnded > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("${t.openEnded} sem fim definido")
                            }
                            if (isEmpty()) append("nenhum parcelamento em curso")
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                }

                if (index < totals.lastIndex) {
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.SummaryCell(label: String, value: String, color: Color) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun FrequencyBadge(label: String) {
    Text(
        text = label,
        color = Green80,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Green80.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
