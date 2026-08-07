package com.finploit.android.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.GoalDto
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.util.parseAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Metas", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
            ),
            actions = {
                IconButton(onClick = viewModel::loadGoals) {
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
            uiState.goals.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma meta criada", color = Color.Gray)
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
                items(uiState.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onDelete = { viewModel.deleteGoal(goal.id) },
                        onDeposit = { viewModel.startDeposit(goal) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        uiState.depositMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.padding(12.dp),
                action = {
                    TextButton(onClick = viewModel::clearDepositMessage) {
                        Text("OK", color = Green80)
                    }
                },
            ) { Text(msg) }
        }
    }

    uiState.depositingGoal?.let { goal ->
        DepositDialog(
            goalName = goal.name,
            isSaving = uiState.isDepositing,
            onConfirm = { amount, ledger -> viewModel.deposit(amount, ledger) },
            onDismiss = viewModel::cancelDeposit,
        )
    }
}

/**
 * Depositar numa meta.
 *
 * A caixa "registar como despesa" é a decisão do C2 à vista: por omissão o
 * dinheiro sai também no livro-razão, porque saiu da conta de verdade. Desligar
 * é para transferências entre contas próprias.
 */
@Composable
private fun DepositDialog(
    goalName: String,
    isSaving: Boolean,
    onConfirm: (Double, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val currency = LocalCurrencyConfig.current
    var amount by remember { mutableStateOf("") }
    var ledger by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Depositar em $goalName", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Valor (${currency.symbol})", color = TextDisabled) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green80,
                        unfocusedBorderColor = TextDisabled.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Green80,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = ledger,
                        onCheckedChange = { ledger = it },
                        colors = CheckboxDefaults.colors(checkedColor = Green80),
                    )
                    Column {
                        Text("Registar como despesa", color = Color.White, fontSize = 13.sp)
                        Text(
                            "Na categoria Metas. Desligue só se for transferência entre contas suas.",
                            color = TextDisabled,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val valor = parseAmountInput(amount) ?: return@TextButton
                    if (valor > 0) onConfirm(valor, ledger)
                },
            ) {
                Text(
                    if (isSaving) "A depositar..." else "Depositar",
                    color = Green80,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDisabled) }
        },
    )
}

@Composable
private fun GoalCard(goal: GoalDto, onDelete: () -> Unit, onDeposit: () -> Unit) {
    val currency = LocalCurrencyConfig.current
    val current = goal.currentValue ?: 0.0
    val progress = if (goal.targetValue > 0) (current / goal.targetValue).toFloat().coerceIn(0f, 1f) else 0f
    val percent = (progress * 100).toInt()
    val statusColor = when (goal.status) {
        "COMPLETED" -> Green80
        "CANCELLED" -> ExpenseRed
        else -> Color(0xFFFFAB40)
    }
    val statusLabel = when (goal.status) {
        "COMPLETED" -> "Concluída"
        "CANCELLED" -> "Cancelada"
        else -> "Em andamento"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (!goal.description.isNullOrBlank()) {
                        Text(goal.description, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                // Depositar é o que faz a meta chegar ao livro-razão (C2); o
                // telemóvel não tinha por onde o fazer.
                IconButton(onClick = onDeposit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Depositar", tint = Green80)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = ExpenseRed.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(currency.format(current), color = Green80, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(currency.format(goal.targetValue), color = Color.Gray, fontSize = 14.sp)
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Green80,
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$percent% concluído", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
