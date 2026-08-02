package com.finploit.android.ui.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CURRENCY_OPTIONS
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.ui.theme.WarningAmber
import com.finploit.android.ui.theme.currencyConfigByCode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import com.finploit.android.util.filterAmountInput
import com.finploit.android.util.parseAmountInput

private val ptBR = Locale("pt", "BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: BillsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var payTarget by remember { mutableStateOf<BillItemDto?>(null) }
    var editTarget by remember { mutableStateOf<BillItemDto?>(null) }
    var deleteTarget by remember { mutableStateOf<BillItemDto?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        val text = uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearError()
        }
    }

    val expenses = remember(uiState.items) {
        uiState.items.filter { !it.isIncome }.sortedBy { if (it.overdue && !it.isPaid) 0 else 1 }
    }
    val incomes = remember(uiState.items) {
        uiState.items.filter { it.isIncome }.sortedBy { if (it.overdue && !it.isPaid) 0 else 1 }
    }

    Scaffold(
        containerColor = SurfaceDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Contas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = GreenPrimary,
                contentColor = BackgroundDark,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nova conta")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthNavigator(
                month = uiState.month,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )
            SummaryHeader(state = uiState)

            when {
                uiState.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GreenPrimary)
                }

                uiState.items.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Nenhuma conta para este mês. Toque no + para criar.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (expenses.isNotEmpty()) {
                        item(key = "header-expense") { SectionHeader("A Pagar", ExpenseRed) }
                        items(expenses, key = { "e-${it.id}" }) { item ->
                            BillCard(
                                item = item,
                                onToggle = {
                                    if (item.isPaid) viewModel.togglePaid(item) else payTarget = item
                                },
                                onEdit = { editTarget = item },
                                onDelete = { deleteTarget = item },
                            )
                        }
                    }
                    if (incomes.isNotEmpty()) {
                        item(key = "header-income") { SectionHeader("A Receber", IncomeGreen) }
                        items(incomes, key = { "i-${it.id}" }) { item ->
                            BillCard(
                                item = item,
                                onToggle = {
                                    if (item.isPaid) viewModel.togglePaid(item) else payTarget = item
                                },
                                onEdit = { editTarget = item },
                                onDelete = { deleteTarget = item },
                            )
                        }
                    }
                }
            }
        }
    }

    payTarget?.let { target ->
        PayAmountDialog(
            item = target,
            onDismiss = { payTarget = null },
            onConfirm = { amount ->
                viewModel.togglePaid(target, amount)
                payTarget = null
            },
        )
    }

    if (showCreate) {
        BillFormDialog(
            existing = null,
            isSaving = uiState.isSaving,
            categories = uiState.categories,
            defaultCurrency = LocalCurrencyConfig.current.code,
            defaultDueDate = defaultDueDate(uiState.month),
            onDismiss = { showCreate = false },
            onConfirm = { description, amount, dueDate, type, currency, categoryId ->
                viewModel.createBill(description, amount, dueDate, type, currency, categoryId)
                showCreate = false
            },
        )
    }

    editTarget?.let { target ->
        BillFormDialog(
            existing = target,
            isSaving = uiState.isSaving,
            categories = uiState.categories,
            defaultCurrency = target.currency,
            defaultDueDate = target.dueDate.take(10),
            onDismiss = { editTarget = null },
            onConfirm = { description, amount, dueDate, _, _, categoryId ->
                viewModel.updateBill(target.id, description, amount, dueDate, categoryId)
                editTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            containerColor = CardBackground,
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir conta", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tem a certeza de que deseja excluir \"${target.description}\"? Esta ação não pode ser desfeita.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBill(target.id)
                    deleteTarget = null
                }) {
                    Text("Excluir", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color) {
    Text(
        title,
        color = accent,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun PayAmountDialog(
    item: BillItemDto,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val itemCurrency = currencyConfigByCode(item.currency)
    val income = item.isIncome
    var text by remember { mutableStateOf("%.2f".format(item.amount)) }
    val parsed = parseAmountInput(text)
    val canConfirm = parsed != null && parsed >= 0.0

    AlertDialog(
        containerColor = CardBackground,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (income) "Registrar recebimento" else "Marcar como paga",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    item.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Previsto ${itemCurrency.format(item.amount)}",
                    color = TextDisabled,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("${if (income) "Valor recebido" else "Valor pago"} (${itemCurrency.code})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = editorFieldColors(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (canConfirm) onConfirm(parsed!!) },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = BackgroundDark,
                ),
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillFormDialog(
    existing: BillItemDto?,
    isSaving: Boolean,
    categories: List<FinanceCategoryDto>,
    defaultCurrency: String,
    defaultDueDate: String,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        amount: Double,
        dueDate: String,
        type: String,
        currency: String?,
        categoryId: Int?,
    ) -> Unit,
) {
    val editing = existing != null
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var dueDate by remember { mutableStateOf(defaultDueDate) }
    var type by remember { mutableStateOf(existing?.type ?: "expense") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }

    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val amount = parseAmountInput(amountText)
    val validDate = runCatching { LocalDate.parse(dueDate.trim()) }.isSuccess
    val canSave = description.trim().isNotEmpty() && amount != null && amount > 0.0 && validDate && !isSaving

    val selectedCurrency = currencyConfigByCode(currency)
    val selectedCategory = categories.find { it.id == categoryId }

    AlertDialog(
        containerColor = CardBackground,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editing) "Editar conta" else "Nova conta",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = editorFieldColors(),
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = filterAmountInput(it) },
                    label = { Text("Valor (${selectedCurrency.code})") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = editorFieldColors(),
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Vencimento (AAAA-MM-DD)") },
                    placeholder = { Text("2026-07-15") },
                    singleLine = true,
                    isError = dueDate.isNotBlank() && !validDate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = editorFieldColors(),
                )
                Spacer(Modifier.height(12.dp))

                if (!editing) {
                    // Type toggle: A pagar / A receber
                    Text("Tipo", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeChip(
                            label = "A pagar",
                            selected = type == "expense",
                            accent = ExpenseRed,
                            modifier = Modifier.weight(1f),
                            onClick = { type = "expense" },
                        )
                        TypeChip(
                            label = "A receber",
                            selected = type == "income",
                            accent = IncomeGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { type = "income" },
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // Currency picker (only on create; PATCH does not change currency)
                    ExposedDropdownMenuBox(
                        expanded = currencyMenuExpanded,
                        onExpandedChange = { currencyMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = "${selectedCurrency.flag} ${selectedCurrency.code} — ${selectedCurrency.label}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Moeda") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = editorFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false },
                            containerColor = SurfaceDark,
                        ) {
                            CURRENCY_OPTIONS.forEach { cfg ->
                                DropdownMenuItem(
                                    text = { Text("${cfg.flag} ${cfg.code} — ${cfg.label}", color = Color.White) },
                                    onClick = {
                                        currency = cfg.code
                                        currencyMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Category picker (optional)
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Sem categoria",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria (opcional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = editorFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        containerColor = SurfaceDark,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem categoria", color = Color.White) },
                            onClick = {
                                categoryId = null
                                categoryMenuExpanded = false
                            },
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = Color.White) },
                                onClick = {
                                    categoryId = cat.id
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onConfirm(
                            description.trim(),
                            amount!!,
                            dueDate.trim(),
                            type,
                            if (editing) null else currency,
                            categoryId,
                        )
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = BackgroundDark,
                ),
            ) {
                Text(if (isSaving) "A guardar..." else "Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.copy(alpha = 0.2f),
                contentColor = accent,
            ),
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MonthNavigator(
    month: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior", tint = TextPrimary)
        }
        Text(
            monthLabel(month),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês", tint = TextPrimary)
        }
    }
}

@Composable
private fun SummaryHeader(state: BillsUiState) {
    val currency = LocalCurrencyConfig.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                label = "A Pagar",
                value = currency.format(state.expensePending),
                accent = ExpenseRed,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "A Receber",
                value = currency.format(state.incomePending),
                accent = IncomeGreen,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                label = "Saldo Previsto",
                value = currency.format(state.projectedBalance),
                accent = if (state.projectedBalance >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "Saldo Realizado",
                value = currency.format(state.realizedBalance),
                accent = if (state.realizedBalance >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BillCard(
    item: BillItemDto,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val paid = item.isPaid
    val income = item.isIncome
    val overdue = !paid && item.overdue
    val cardColor = if (overdue) ExpenseRed.copy(alpha = 0.1f) else CardBackground
    val itemCurrency = currencyConfigByCode(item.currency)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = paid,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (income) IncomeGreen else GreenPrimary,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = SurfaceDark,
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.description,
                    color = if (paid) TextDisabled else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textDecoration = if (paid) TextDecoration.LineThrough else TextDecoration.None,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.categoryName?.let { name ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(parseColor(item.categoryColor) ?: GreenPrimary),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(name, color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Vence ${formatDueDate(item.dueDate)}", color = TextSecondary, fontSize = 12.sp)
                }
                if (overdue || item.carriedOver) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (overdue) BillChip("Em atraso", ExpenseRed)
                        if (item.carriedOver) BillChip("Mês anterior", WarningAmber)
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            val paidDiffers = paid && item.paidAmount != null && item.paidAmount != item.amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    itemCurrency.format(if (paidDiffers) item.paidAmount!! else item.amount),
                    color = when {
                        paid -> TextDisabled
                        overdue -> ExpenseRed
                        income -> IncomeGreen
                        else -> TextPrimary
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textDecoration = if (paid && !paidDiffers) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (paidDiffers) {
                    Text(
                        "previsto ${itemCurrency.format(item.amount)}",
                        color = TextDisabled,
                        fontSize = 11.sp,
                    )
                }
                Row {
                    if (!paid) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BillChip(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    focusedLabelColor = GreenPrimary,
    cursorColor = GreenPrimary,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)

private fun monthLabel(month: String): String = runCatching {
    val ym = YearMonth.parse(month)
    val name = ym.month.getDisplayName(TextStyle.FULL, ptBR)
        .replaceFirstChar { it.uppercase(ptBR) }
    "$name ${ym.year}"
}.getOrElse { month }

/** Default due date for a new bill: today if within the displayed month, else the 1st of that month. */
private fun defaultDueDate(month: String): String = runCatching {
    val ym = YearMonth.parse(month)
    val today = LocalDate.now()
    if (YearMonth.from(today) == ym) today.toString() else ym.atDay(1).toString()
}.getOrElse { LocalDate.now().toString() }

private fun parseColor(hex: String?): Color? {
    val value = hex?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        val normalized = if (value.startsWith("#")) value else "#$value"
        Color(android.graphics.Color.parseColor(normalized))
    }.getOrNull()
}

/** "2026-07-10T00:00:00.000Z" -> "10/07/2026" */
private fun formatDueDate(iso: String): String = try {
    val d = java.time.LocalDate.parse(iso.take(10))
    "%02d/%02d/%d".format(d.dayOfMonth, d.monthValue, d.year)
} catch (e: Exception) {
    iso.take(10)
}
