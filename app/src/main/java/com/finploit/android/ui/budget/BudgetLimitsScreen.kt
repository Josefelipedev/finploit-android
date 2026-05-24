package com.finploit.android.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.finploit.android.data.local.entity.BudgetLimitEntity
import com.finploit.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetLimitsScreen(
    viewModel: BudgetLimitsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("Limites de Orçamento", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = GreenPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        if (state.limits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📊", fontSize = 48.sp)
                    Text("Sem limites definidos", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Toca em + para definir um limite por categoria", color = TextDisabled, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.limits) { limit ->
                    val spent = state.monthlySummary[limit.categoryName] ?: 0.0
                    val progress = if (limit.monthlyLimit > 0) (spent / limit.monthlyLimit).coerceIn(0.0, 1.0) else 0.0
                    val isOverBudget = spent > limit.monthlyLimit
                    val isNearLimit = progress >= limit.alertAt / 100.0

                    BudgetLimitCard(
                        limit = limit,
                        spent = spent,
                        progress = progress.toFloat(),
                        isOverBudget = isOverBudget,
                        isNearLimit = isNearLimit,
                        onEdit = { viewModel.showAddDialog(limit) },
                        onDelete = { viewModel.delete(limit.categoryId) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        if (state.showAddDialog) {
            AddBudgetLimitDialog(
                initial = state.editingLimit,
                onConfirm = { catId, catName, amount -> viewModel.save(catId, catName, amount) },
                onDismiss = viewModel::hideDialog,
            )
        }
    }
}

@Composable
private fun BudgetLimitCard(
    limit: BudgetLimitEntity,
    spent: Double,
    progress: Float,
    isOverBudget: Boolean,
    isNearLimit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = when {
        isOverBudget -> ExpenseRed
        isNearLimit -> Color(0xFFFFA726) // orange
        else -> GreenPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(limit.categoryName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "€%.2f de €%.2f".format(spent, limit.monthlyLimit),
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextDisabled, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${(progress * 100).toInt()}% usado", color = TextDisabled, fontSize = 11.sp)
                if (isOverBudget) {
                    Text("⚠️ Excedido em €%.2f".format(spent - limit.monthlyLimit), color = ExpenseRed, fontSize = 11.sp)
                } else {
                    Text("Restam €%.2f".format(limit.monthlyLimit - spent), color = TextDisabled, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AddBudgetLimitDialog(
    initial: BudgetLimitEntity?,
    onConfirm: (Int, String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryName by remember { mutableStateOf(initial?.categoryName ?: "") }
    var amount by remember { mutableStateOf(initial?.monthlyLimit?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text(if (initial == null) "Novo Limite" else "Editar Limite", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Categoria", color = TextDisabled) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = TextDisabled.copy(alpha = 0.5f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GreenPrimary,
                    ),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Limite mensal (€)", color = TextDisabled) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@TextButton
                    if (categoryName.isNotBlank() && amt > 0) {
                        onConfirm(initial?.categoryId ?: categoryName.hashCode(), categoryName.trim(), amt)
                    }
                },
            ) {
                Text("Guardar", color = GreenPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextDisabled)
            }
        },
    )
}
