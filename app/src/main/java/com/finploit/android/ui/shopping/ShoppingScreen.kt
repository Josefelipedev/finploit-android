package com.finploit.android.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.MonthlyPlanResponse
import com.finploit.android.data.dto.ShoppingListDto
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    onListClick: (ShoppingListDto) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthlyDialog by remember { mutableStateOf(false) }

    if (showMonthlyDialog) {
        MonthlyPlanDialog(
            isLoading = uiState.isGeneratingMonthly,
            onDismiss = { showMonthlyDialog = false },
            onConfirm = { budget, notes ->
                viewModel.generateMonthlyPlan(budget, notes)
                showMonthlyDialog = false
            },
        )
    }

    // Resultado do plano mensal
    uiState.monthlyPlan?.let { plan ->
        MonthlyPlanResultDialog(
            plan = plan,
            onDismiss = { viewModel.clearMonthlyPlan() },
            onOpenList = { list ->
                viewModel.clearMonthlyPlan()
                viewModel.selectList(list)
                onListClick(list)
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Listas de Compras", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
            ),
            actions = {
                IconButton(onClick = viewModel::loadLists) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Green80)
                }
            }
        )

        // Botão Plano Mensal
        Button(
            onClick = { showMonthlyDialog = true },
            enabled = !uiState.isGeneratingMonthly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A2A)),
        ) {
            if (uiState.isGeneratingMonthly) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("A gerar plano mensal…", color = GreenPrimary, fontSize = 14.sp)
            } else {
                Text("📅", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("Gerar Plano de Compras Mensal", color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green80)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Erro", color = ExpenseRed)
            }
            uiState.lists.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma lista criada", color = Color.Gray)
                    Text("Use o + para criar uma lista", color = Color.Gray, fontSize = 13.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(uiState.lists, key = { it.id }) { list ->
                    ShoppingListCard(
                        list = list,
                        onClick = { onListClick(list) },
                        onDelete = { viewModel.deleteList(list.id) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MonthlyPlanDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double?, String?) -> Unit,
) {
    var budget by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Plano de Compras Mensal", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A IA irá gerar uma lista completa para 1 mês com preços reais dos supermercados.", color = Color.Gray, fontSize = 13.sp)
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Orçamento mensal (€) — opcional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green80, focusedLabelColor = Green80,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f), unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    ),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Preferências (ex: vegetariano, sem glúten)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green80, focusedLabelColor = Green80,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f), unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(budget.toDoubleOrNull(), notes.ifBlank { null }) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Gerar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
    )
}

@Composable
private fun MonthlyPlanResultDialog(
    plan: MonthlyPlanResponse,
    onDismiss: () -> Unit,
    onOpenList: (ShoppingListDto) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("✅ Plano Mensal Criado!", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.list.name, color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${plan.list.items.size} itens gerados", color = Color.Gray, fontSize = 13.sp)
                plan.totalEstimate?.let {
                    Text("Total estimado: ${LocalCurrencyConfig.current.format(it)}", color = Green80, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                plan.weeklyBudget?.let {
                    Text("Orçamento semanal: ${LocalCurrencyConfig.current.format(it)}", color = Color.Gray, fontSize = 12.sp)
                }
                plan.savingsSummary?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = Color.LightGray, fontSize = 12.sp)
                }
                plan.tips?.let {
                    Text("💡 $it", color = Color.Gray, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onOpenList(plan.list) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            ) { Text("Ver Lista", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar", color = Color.Gray) }
        },
    )
}

@Composable
private fun ShoppingListCard(
    list: ShoppingListDto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val total = list.items.sumOf { (it.price ?: 0.0) * it.quantity }
    val purchased = list.items.count { it.purchased }
    val totalItems = list.items.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(list.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$purchased/$totalItems itens comprados",
                    color = Color.Gray,
                    fontSize = 13.sp,
                )
                if (total > 0) {
                    Text(
                        text = "Total estimado: ${LocalCurrencyConfig.current.format(total)}",
                        color = Green80,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir lista", tint = ExpenseRed.copy(alpha = 0.7f))
            }
        }
    }
}
