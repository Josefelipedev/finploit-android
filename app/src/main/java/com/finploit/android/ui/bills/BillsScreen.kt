package com.finploit.android.ui.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.data.dto.BillItemDto
import com.finploit.android.data.dto.BillsForecastDto
import com.finploit.android.data.dto.FinanceCategoryDto
import com.finploit.android.data.dto.MonthlyBillsForecastDto
import com.finploit.android.ui.components.OwnerChip
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CURRENCY_OPTIONS
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.LocalOwnerNaming
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

    // As secções desenham o que passa nos filtros; os totais em cima continuam a
    // ser os do mês inteiro, que é o que eles sempre foram.
    val visible = uiState.visibleItems
    val expenses = remember(visible) {
        visible.filter { !it.isIncome }.sortedBy { if (it.overdue && !it.isPaid) 0 else 1 }
    }
    val incomes = remember(visible) {
        visible.filter { it.isIncome }.sortedBy { if (it.overdue && !it.isPaid) 0 else 1 }
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
            // Os totais só se mostram quando são de facto os do mês. Depois de
            // uma falha eram quatro zeros com ar de resposta.
            if (!uiState.loadFailed) SummaryHeader(state = uiState)

            when {
                uiState.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GreenPrimary)
                }

                // Não se conseguiu perguntar. Dizê-lo — e continuar a dizê-lo
                // depois de o snackbar desaparecer — em vez de mostrar um mês
                // vazio que o utilizador não tem como distinguir do verdadeiro.
                uiState.loadFailed -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Não foi possível carregar as contas deste mês.",
                            color = ExpenseRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Os totais ficam escondidos para não mostrarem um saldo que não é o seu.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.load(uiState.month) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        ) {
                            Text("Tentar outra vez", color = BackgroundDark)
                        }
                    }
                }

                uiState.items.isEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "empty-month") {
                        Text(
                            "Nenhuma conta para este mês. Toque no + para criar.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                    item(key = "monthly-forecast") {
                        MonthlyForecastSection(
                            forecast = uiState.monthlyForecast,
                            loading = uiState.isForecastLoading,
                            failed = uiState.forecastError,
                            onRetry = viewModel::loadMonthlyForecast,
                        )
                    }
                    if (uiState.bankAccounts.any { it.creditLimit != null }) {
                        item(key = "credit-limits") { CreditLimitsSection(uiState.bankAccounts) }
                    }
                    uiState.accountsForecast?.let { forecast ->
                        item(key = "account-forecast") { AccountForecastSection(forecast) }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "filters") {
                        BillFiltersBar(
                            state = uiState,
                            onChange = viewModel::setFilters,
                            onClear = viewModel::clearFilters,
                        )
                    }

                    if (visible.isEmpty()) {
                        item(key = "empty-filtered") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "Nenhuma conta com estes filtros.",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = viewModel::clearFilters) {
                                    Text("Limpar filtros", color = GreenPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (expenses.isNotEmpty()) {
                        item(key = "header-expense") { SectionHeader("A Pagar", ExpenseRed) }
                        items(expenses, key = { "e-${it.id}" }) { item ->
                            BillCard(
                                item = item,
                                accountName = uiState.accountName(item.accountId),
                                hasBankAccounts = uiState.hasBankAccounts,
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
                                accountName = uiState.accountName(item.accountId),
                                hasBankAccounts = uiState.hasBankAccounts,
                                onToggle = {
                                    if (item.isPaid) viewModel.togglePaid(item) else payTarget = item
                                },
                                onEdit = { editTarget = item },
                                onDelete = { deleteTarget = item },
                            )
                        }
                    }

                    item(key = "month-total") { CurrentMonthTotal(uiState) }
                    item(key = "monthly-forecast") {
                        MonthlyForecastSection(
                            forecast = uiState.monthlyForecast,
                            loading = uiState.isForecastLoading,
                            failed = uiState.forecastError,
                            onRetry = viewModel::loadMonthlyForecast,
                        )
                    }
                    if (uiState.bankAccounts.any { it.creditLimit != null }) {
                        item(key = "credit-limits") { CreditLimitsSection(uiState.bankAccounts) }
                    }
                    uiState.accountsForecast?.let { forecast ->
                        item(key = "account-forecast") { AccountForecastSection(forecast) }
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
            bankAccounts = uiState.bankAccounts,
            defaultCurrency = LocalCurrencyConfig.current.code,
            defaultDueDate = defaultDueDate(uiState.month),
            onDismiss = { showCreate = false },
            onConfirm = { description, amount, dueDate, type, currency, categoryId, accountId ->
                viewModel.createBill(description, amount, dueDate, type, currency, categoryId, accountId)
                showCreate = false
            },
        )
    }

    editTarget?.let { target ->
        BillFormDialog(
            existing = target,
            isSaving = uiState.isSaving,
            categories = uiState.categories,
            bankAccounts = uiState.bankAccounts,
            defaultCurrency = target.currency,
            defaultDueDate = target.dueDate.take(10),
            onDismiss = { editTarget = null },
            onConfirm = { description, amount, dueDate, _, _, categoryId, accountId ->
                viewModel.updateBill(target.id, description, amount, dueDate, categoryId, accountId)
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

/**
 * Os filtros da lista do mês.
 *
 * O que enche o ecrã são as atrasadas de meses anteriores, e por isso elas têm
 * um botão só para si, com a contagem à vista. Os outros quatro são menus.
 *
 * O contador diz "N de M" e mostra o subtotal do que está à vista — **só quando
 * todas as linhas filtradas estão na mesma moeda**, porque aqui não há taxas de
 * câmbio à mão e somar moedas diferentes é precisamente o erro que os totais do
 * servidor existem para evitar.
 */
@Composable
private fun BillFiltersBar(
    state: BillsUiState,
    onChange: (BillFilters) -> Unit,
    onClear: () -> Unit,
) {
    val filters = state.filters
    val visible = state.visibleItems
    val subtotal = state.visibleTotal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.carriedOverCount > 0) {
            val on = filters.showCarriedOver
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) WarningAmber.copy(alpha = 0.18f) else SurfaceDark)
                    .clickable { onChange(filters.copy(showCarriedOver = !on)) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    "Atrasadas de meses anteriores (${state.carriedOverCount})",
                    color = if (on) WarningAmber else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterMenu(
                modifier = Modifier.weight(1f),
                current = filters.type.label,
                options = BillTypeFilter.entries.map { it.label to it },
                onPick = { onChange(filters.copy(type = it)) },
            )
            FilterMenu(
                modifier = Modifier.weight(1f),
                current = filters.status.label,
                options = BillStatusFilter.entries.map { it.label to it },
                onPick = { onChange(filters.copy(status = it)) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.categoriesInMonth.size > 1) {
                FilterMenu(
                    modifier = Modifier.weight(1f),
                    current = filters.category ?: "Todas as categorias",
                    options = listOf<Pair<String, String?>>("Todas as categorias" to null) +
                        state.categoriesInMonth.map { it to it },
                    onPick = { onChange(filters.copy(category = it)) },
                )
            }
            // Num workspace de uma pessoa só, este filtro seria ruído.
            if (state.hasMultipleOwners) {
                FilterMenu(
                    modifier = Modifier.weight(1f),
                    current = filters.owner.label,
                    options = BillOwnerFilter.entries.map { it.label to it },
                    onPick = { onChange(filters.copy(owner = it)) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildString {
                    append("${visible.size} de ${state.items.size}")
                    subtotal?.let { (total, currency) ->
                        append(" · ${currencyConfigByCode(currency).format(total)}")
                    }
                },
                color = TextDisabled,
                fontSize = 12.sp,
            )
            if (filters.isActive) {
                Text(
                    "Limpar filtros",
                    color = GreenPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** Menu de uma escolha só, com o valor actual à vista. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterMenu(
    modifier: Modifier = Modifier,
    current: String,
    options: List<Pair<String, T>>,
    onPick: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = CardBackground,
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        expanded = false
                        onPick(value)
                    },
                )
            }
        }
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
    bankAccounts: List<BankAccountDto>,
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
        accountId: Int?,
    ) -> Unit,
) {
    val editing = existing != null
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var dueDate by remember { mutableStateOf(defaultDueDate) }
    var type by remember { mutableStateOf(existing?.type ?: "expense") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var accountId by remember { mutableStateOf(existing?.accountId) }

    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val amount = parseAmountInput(amountText)
    val validDate = runCatching { LocalDate.parse(dueDate.trim()) }.isSuccess
    val canSave = description.trim().isNotEmpty() && amount != null && amount > 0.0 && validDate && !isSaving

    val selectedCurrency = currencyConfigByCode(currency)
    val selectedCategory = categories.find { it.id == categoryId }
    val selectedAccount = bankAccounts.find { it.id == accountId }

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

                // Conta bancária: é ela que responde a "quanto me fica na
                // conta". Só aparece quando há contas registadas — um seletor
                // com uma opção só ("Sem conta") não ajuda ninguém.
                if (bankAccounts.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.bankName} · ${it.currency}" } ?: "Sem conta",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    if (type == "income") "Entra na conta (opcional)"
                                    else "Sai da conta (opcional)",
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = editorFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false },
                            containerColor = SurfaceDark,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sem conta", color = Color.White) },
                                onClick = {
                                    accountId = null
                                    accountMenuExpanded = false
                                },
                            )
                            bankAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${account.bankName} · ${account.currency}", color = Color.White)
                                    },
                                    onClick = {
                                        accountId = account.id
                                        accountMenuExpanded = false
                                    },
                                )
                            }
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
                            accountId,
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
    val safe = state.unconvertedCurrencies.isEmpty()
    fun money(value: Double) = if (safe) currency.format(value) else "—"
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                label = "A Pagar",
                value = money(state.expensePending),
                accent = ExpenseRed,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "A Receber",
                value = money(state.incomePending),
                accent = IncomeGreen,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                label = "Saldo Previsto",
                value = money(state.projectedBalance),
                accent = if (state.projectedBalance >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "Saldo Realizado",
                value = money(state.realizedBalance),
                accent = if (state.realizedBalance >= 0) IncomeGreen else ExpenseRed,
                modifier = Modifier.weight(1f),
            )
        }
        if (!safe) {
            Text(
                "⚠️ Falta câmbio para ${state.unconvertedCurrencies.joinToString(", ")}. " +
                    "Os totais ficam ocultos para não misturar moedas.",
                color = WarningAmber,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun CurrentMonthTotal(state: BillsUiState) {
    val safe = state.unconvertedCurrencies.isEmpty()
    val currency = LocalCurrencyConfig.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("TOTAL DE CONTAS DO MÊS", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Pago + ainda por pagar", color = TextSecondary, fontSize = 11.sp)
            }
            Text(
                if (safe) currency.format(state.expensePending + state.expensePaid) else "—",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MonthlyForecastSection(
    forecast: MonthlyBillsForecastDto?,
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PRÓXIMOS MESES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("A calcular os próximos 10 meses…", color = TextSecondary, fontSize = 12.sp)
                    }

                    failed -> {
                        Text("Não foi possível calcular os próximos meses.", color = ExpenseRed, fontSize = 12.sp)
                        TextButton(onClick = onRetry) { Text("Tentar outra vez", color = GreenPrimary) }
                    }

                    forecast != null -> {
                        val safe = forecast.unconvertedCurrencies.isEmpty()
                        if (!safe) {
                            Text(
                                "⚠️ Falta câmbio para ${forecast.unconvertedCurrencies.joinToString(", ")}. " +
                                    "Os totais e o mês mais pesado ficam ocultos.",
                                color = WarningAmber,
                                fontSize = 11.sp,
                            )
                        }
                        forecast.months.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { item ->
                                    val heaviest = safe && item.month == forecast.heaviest
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (heaviest) ExpenseRed.copy(alpha = 0.10f) else SurfaceDark,
                                                RoundedCornerShape(12.dp),
                                            )
                                            .padding(12.dp),
                                    ) {
                                        Text(monthLabel(item.month), color = TextSecondary, fontSize = 11.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (safe) currencyConfigByCode(forecast.displayCurrency).format(item.expense) else "—",
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (heaviest) {
                                            Text("Mais pesado", color = ExpenseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        if (safe && forecast.heaviest != null) {
                            Text(
                                "${monthLabel(forecast.heaviest)} é o mês mais pesado.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        forecast.relief?.takeIf { safe }?.let { relief ->
                            Text(
                                "A despesa alivia de forma sustentada a partir de ${monthLabel(relief)}.",
                                color = GreenPrimary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditLimitsSection(accounts: List<BankAccountDto>) {
    val withLimit = accounts.filter { it.creditLimit != null }
    if (withLimit.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LIMITES DE CRÉDITO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                withLimit.forEach { account ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(account.bankName, color = TextPrimary, fontSize = 13.sp)
                        Text(
                            currencyConfigByCode(account.currency).format(account.creditLimit!!),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    "⚠️ Limite de crédito e Pix crédito não são dinheiro livre.",
                    color = WarningAmber,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * O que fica em cada conta bancária depois de pagar o que falta.
 *
 * Os totais em cima respondem "sobra dinheiro?" para o casal inteiro — e num
 * casal com contas separadas isso não chega: saber que sobram 900 € não diz em
 * que conta é que eles estão, nem qual dos dois vai ficar apertado no dia 28.
 *
 * Cada cartão fala na moeda da **própria conta** (por isso `currencyConfigByCode`
 * e não o `LocalCurrencyConfig` do perfil): uma conta em euros que mostrasse
 * "R$" punha o símbolo errado por cima do número certo.
 */
@Composable
private fun AccountForecastSection(forecast: BillsForecastDto) {
    if (forecast.items.isEmpty() && forecast.unassigned.count == 0) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "O QUE FICA EM CADA CONTA",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )

        // Cada cartão soma contas de várias moedas na moeda da conta bancária.
        // Sem taxa, essa soma junta valores de face.
        forecast.unconvertedCurrencies?.takeIf { it.isNotEmpty() }?.let { moedas ->
            Text(
                "⚠️ ${moedas.joinToString(", ")} sem taxa de câmbio: os saldos previstos são aproximados.",
                color = TextDisabled,
                fontSize = 11.sp,
            )
        }

        forecast.items.forEach { account ->
            val money = currencyConfigByCode(account.currency)
            val negativo = account.projectedBalance < 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(account.bankName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                listOfNotNull(account.ownerName, account.currency).joinToString(" · "),
                                color = TextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        Text(
                            if (negativo) "não chega" else "${account.billCount} por liquidar",
                            color = if (negativo) ExpenseRed else TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        money.format(account.projectedBalance),
                        color = if (negativo) ExpenseRed else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                    Text("fica no fim, se tudo for pago", color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    ForecastLine("Hoje", money.format(account.currentBalance), TextPrimary)
                    ForecastLine("Ainda entra", "+${money.format(account.incoming)}", IncomeGreen)
                    ForecastLine("Ainda sai", "−${money.format(account.outgoing)}", ExpenseRed)
                }
            }
        }

        // O dinheiro que ninguém disse de onde sai não se reparte pelas contas
        // — calá-lo era deixar os cartões acima parecerem a história toda.
        if (forecast.unassigned.count > 0) {
            val money = currencyConfigByCode(forecast.unassigned.currency)
            val n = forecast.unassigned.count
            Text(
                "$n ${if (n == 1) "conta ainda não diz" else "contas ainda não dizem"} de que conta " +
                    "${if (n == 1) "sai" else "saem"} (${money.format(forecast.unassigned.outgoing)} a pagar) — " +
                    "${if (n == 1) "não entra" else "não entram"} nas previsões acima.",
                color = TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ForecastLine(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
    /** Nome do banco de onde sai; nulo quando a conta não o diz. */
    accountName: String?,
    /** Há contas bancárias registadas — só então faz sentido dizer que falta uma. */
    hasBankAccounts: Boolean,
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
                installmentLabel(item)?.let { label ->
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                // O chip de autoria só aparece no workspace do casal; os outros
                // dois só quando há o que assinalar.
                val owner = LocalOwnerNaming.current.nameOf(item.userId)
                // De que conta sai. Quando não está dito, a conta fica de fora
                // da previsão por conta — e um número que falta explica-se
                // melhor aqui, na linha que o causa, do que num aviso no topo.
                val missingAccount = accountName == null && !paid && hasBankAccounts
                if (overdue || item.carriedOver || owner != null || accountName != null || missingAccount) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (overdue) BillChip("Em atraso", ExpenseRed)
                        if (item.carriedOver) BillChip("Mês anterior", WarningAmber)
                        OwnerChip(item.userId)
                        accountName?.let { BillChip(it, TextSecondary) }
                        if (missingAccount) BillChip("Sem conta", WarningAmber)
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

private fun installmentLabel(item: BillItemDto): String? {
    val parts = mutableListOf<String>()
    if (item.installment != null && item.installments != null) {
        parts += "${item.installment} de ${item.installments}"
    }
    item.until?.take(7)?.let { key ->
        runCatching { YearMonth.parse(key) }.getOrNull()?.let { ym ->
            val short = ym.month.getDisplayName(TextStyle.SHORT, ptBR).removeSuffix(".")
            parts += "até $short/${ym.year.toString().takeLast(2)}"
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

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
