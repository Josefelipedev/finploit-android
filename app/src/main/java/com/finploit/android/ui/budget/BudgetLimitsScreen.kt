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
import com.finploit.android.data.dto.BudgetLimitDto
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetLimitsScreen(
    viewModel: BudgetLimitsViewModel,
    onBack: () -> Unit,
    /**
     * `true` quando isto é uma aba do `BudgetHubScreen`, que já desenha a barra
     * de topo e o botão de voltar. Sem isto, embutir o ecrã dava duas barras
     * empilhadas e duas setas de voltar que faziam coisas diferentes.
     */
    embedded: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // O gasto chega convertido para a moeda do utilizador; o ecrã escrevia "€"
    // fixo, que era falso para quem usa outra moeda.
    val currency = currencyConfigByCode(state.displayCurrency ?: LocalCurrencyConfig.current.code)

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (!embedded) {
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
        } else {
            // Embutido, o "+" da barra desapareceu com ela — e sem ele não há
            // como criar o primeiro limite.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { viewModel.showAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Novo limite", color = GreenPrimary, fontSize = 13.sp)
                }
            }
        }

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
                if (state.unconvertedCurrencies.isNotEmpty()) {
                    item {
                        Text(
                            "⚠️ ${state.unconvertedCurrencies.joinToString(", ")} sem taxa de câmbio: o gasto é aproximado.",
                            color = TextDisabled,
                            fontSize = 11.sp,
                        )
                    }
                }
                if (state.error != null) {
                    item {
                        Text("⚠️ ${state.error}", color = ExpenseRed, fontSize = 12.sp)
                    }
                }
                items(state.limits) { limit ->
                    val spent =
                        state.monthlySummary[limit.categoryName.orEmpty().trim().lowercase()] ?: 0.0
                    val progress = if (limit.monthlyLimit > 0) (spent / limit.monthlyLimit).coerceIn(0.0, 1.0) else 0.0
                    val isOverBudget = spent > limit.monthlyLimit
                    val isNearLimit = progress >= limit.alertAt / 100.0

                    BudgetLimitCard(
                        limit = limit,
                        spent = spent,
                        currency = currency,
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
                categories = state.categories,
                // Uma categoria só pode ter um limite (a chave do servidor é ela).
                usedCategoryIds = state.limits.map { it.categoryId }.toSet(),
                currency = currency,
                isSaving = state.isSaving,
                onConfirm = { catId, amount -> viewModel.save(catId, amount) },
                onDismiss = viewModel::hideDialog,
            )
        }
    }
}

@Composable
private fun BudgetLimitCard(
    limit: BudgetLimitDto,
    spent: Double,
    currency: CurrencyConfig,
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
                    Text(
                        limit.categoryName ?: "Categoria",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "${currency.format(spent)} de ${currency.format(limit.monthlyLimit)}",
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    // O limite que o cônjuge escreveu chega já convertido (C4).
                    // Dizer em que moeda nasceu responde à pergunta antes de ela
                    // ser feita: o número está certo, o original explica-o.
                    val moedaOriginal = limit.originalCurrency
                    if (moedaOriginal != null && moedaOriginal != (limit.currency ?: currency.code)) {
                        Text(
                            "definido em ${currencyConfigByCode(moedaOriginal).format(limit.originalMonthlyLimit ?: limit.monthlyLimit)}",
                            color = TextDisabled,
                            fontSize = 11.sp,
                        )
                    }
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
                    Text("⚠️ Excedido em ${currency.format(spent - limit.monthlyLimit)}", color = ExpenseRed, fontSize = 11.sp)
                } else {
                    Text("Restam ${currency.format(limit.monthlyLimit - spent)}", color = TextDisabled, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetLimitDialog(
    initial: BudgetLimitDto?,
    categories: List<FinanceCategoryDto>,
    usedCategoryIds: Set<Int>,
    currency: CurrencyConfig,
    isSaving: Boolean,
    onConfirm: (Int, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    // A categoria deixou de ser texto livre: o limite vive no servidor e a
    // chave é a categoria a sério. Antes usava-se o `hashCode()` do nome, que
    // não correspondia a categoria nenhuma e só existia neste telemóvel.
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var amount by remember { mutableStateOf(initial?.monthlyLimit?.toString() ?: "") }
    var menuExpanded by remember { mutableStateOf(false) }

    val escolhiveis = remember(categories, usedCategoryIds, initial) {
        categories.filter { it.id == initial?.categoryId || it.id !in usedCategoryIds }
    }
    val nomeEscolhido = categories.find { it.id == categoryId }?.name ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text(
                if (initial == null) "Novo Limite" else "Editar Limite",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (escolhiveis.isEmpty() && initial == null) {
                    Text(
                        "Todas as categorias já têm limite. Apague um para definir outro.",
                        color = TextDisabled,
                        fontSize = 13.sp,
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { if (initial == null) menuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = nomeEscolhido,
                        onValueChange = {},
                        readOnly = true,
                        enabled = initial == null,
                        label = { Text("Categoria", color = TextDisabled) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = TextDisabled.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextPrimary,
                            cursorColor = GreenPrimary,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = SurfaceDark,
                    ) {
                        escolhiveis.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = TextPrimary) },
                                onClick = {
                                    categoryId = cat.id
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Limite mensal (${currency.symbol})", color = TextDisabled) },
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
                enabled = !isSaving,
                onClick = {
                    // O mesmo parser do resto da app: o último separador manda.
                    val amt = com.finploit.android.util.parseAmountInput(amount) ?: return@TextButton
                    val id = categoryId ?: return@TextButton
                    if (amt > 0) onConfirm(id, amt)
                },
            ) {
                Text(
                    if (isSaving) "A guardar..." else "Guardar",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextDisabled)
            }
        },
    )
}
