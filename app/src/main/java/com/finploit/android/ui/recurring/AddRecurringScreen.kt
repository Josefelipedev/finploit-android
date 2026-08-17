package com.finploit.android.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.finploit.android.data.dto.RecurringTransactionDto
import com.finploit.android.util.filterAmountInput
import com.finploit.android.util.parseAmountInput
import com.finploit.android.util.round2
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.SurfaceDark

// "Diário" saiu (C1): a geração de contas nunca o produziu — a recorrente era
// criada, aparecia na lista e não gerava conta nem aviso. O servidor deixou de
// a aceitar. O rótulo continua no `RecurringScreen` para o caso de existir
// alguma gravada de antes.
private val frequencies = listOf(
    "monthly" to "Mensal",
    "weekly" to "Semanal",
    "yearly" to "Anual",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    viewModel: RecurringViewModel,
    onDismiss: () -> Unit,
    /** Quando vem preenchida, o ecrã edita em vez de criar. */
    existing: RecurringTransactionDto? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyConfig = LocalCurrencyConfig.current

    var type by remember { mutableStateOf("expense") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("monthly") }
    var dueDay by remember { mutableStateOf("1") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    var occurrences by remember { mutableStateOf("12") }
    var totalAmount by remember { mutableStateOf("") }
    var useBusinessDay by remember { mutableStateOf(false) }
    var businessDay by remember { mutableStateOf("5") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var notification by remember { mutableStateOf(true) }
    var datePickerFor by remember { mutableStateOf<String?>(null) }

    // Editar: o formulário abre com o que já lá está. Sem isto, guardar
    // apagaria tudo o que não fosse escrito outra vez.
    LaunchedEffect(existing?.id) {
        existing?.let { r ->
            type = if (r.type == "income" || r.type == "receita") "income" else "expense"
            description = r.description.orEmpty()
            amount = String.format("%.2f", r.amount).replace('.', ',')
            frequency = r.frequency
            dueDay = (r.dueDay ?: 1).toString()
            useBusinessDay = r.businessDay != null
            businessDay = (r.businessDay ?: 5).toString()
            selectedCategoryId = r.categoryId
            selectedAccountId = r.accountId
            occurrences = (r.occurrences ?: 0).takeIf { it > 0 }?.toString().orEmpty()
            totalAmount = r.contractedTotal?.let { String.format("%.2f", it).replace('.', ',') }.orEmpty()
            startDate = r.startDate?.take(10).orEmpty()
            endDate = r.endDate?.take(10).orEmpty()
            notification = r.notification ?: true
        }
    }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) { viewModel.clearSaveState(); onDismiss() }
    }
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { snackbarHostState.showSnackbar(it); viewModel.clearSaveState() }
    }

    val isValid =
        description.isNotBlank() && parseAmountInput(amount) != null && selectedCategoryId != null

    // Total e parcela são a mesma coisa vista de dois lados, ligados pelo número
    // de parcelas: escrever num deles refaz o outro. Quem grava — e quem dá o
    // resto do arredondamento à última parcela — é o servidor.
    fun formatAmount(value: Double) = String.format("%.2f", value).replace('.', ',')

    fun onInstallmentTyped(raw: String) {
        amount = filterAmountInput(raw)
        val n = occurrences.toIntOrNull() ?: 0
        val parcel = parseAmountInput(amount)
        totalAmount = if (n > 0 && parcel != null) formatAmount(round2(parcel * n)) else ""
    }

    fun onTotalTyped(raw: String) {
        totalAmount = filterAmountInput(raw)
        val n = occurrences.toIntOrNull() ?: 0
        val total = parseAmountInput(totalAmount)
        if (n > 0 && total != null) amount = formatAmount(round2(total / n))
    }

    fun onOccurrencesTyped(raw: String) {
        occurrences = raw.filter { c -> c.isDigit() }
        val n = occurrences.toIntOrNull() ?: 0
        // Mudar o número de parcelas mantém o TOTAL e refaz a parcela.
        val total = parseAmountInput(totalAmount)
        if (n > 0 && total != null) amount = formatAmount(round2(total / n))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text(if (existing == null) "Nova Recorrente" else "Editar Recorrente", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
            ),
        )

        SnackbarHost(snackbarHostState) { Snackbar(it) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Tipo", color = Color.Gray, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("income" to "Receita", "expense" to "Despesa").forEach { (key, label) ->
                    val selected = type == key
                    val color = if (key == "income") IncomeGreen else ExpenseRed
                    FilterChip(
                        selected = selected,
                        onClick = { type = key },
                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { onInstallmentTyped(it) },
                label = {
                    Text(
                        if ((occurrences.toIntOrNull() ?: 0) > 0)
                            "Valor da parcela (${currencyConfig.symbol})"
                        else "Valor (${currencyConfig.symbol})"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.categories.find { it.id == selectedCategoryId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    containerColor = Color(0xFF1E1E2E),
                ) {
                    uiState.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, color = Color.White) },
                            onClick = { selectedCategoryId = cat.id; categoryExpanded = false },
                        )
                    }
                }
            }

            // Conta bancária: é daqui que sai a previsão por conta — cada conta
            // gerada herda-a, e o pagamento leva-a ao lançamento. Só aparece
            // quando há contas registadas; um seletor com uma opção só ("Sem
            // conta") não ajuda ninguém.
            if (uiState.bankAccounts.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it },
                ) {
                    val selectedAccount = uiState.bankAccounts.find { it.id == selectedAccountId }
                    OutlinedTextField(
                        value = selectedAccount?.let { "${it.bankName} · ${it.currency}" } ?: "Sem conta",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (type == "income") "Entra na conta (opcional)" else "Sai da conta (opcional)")
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false },
                        containerColor = Color(0xFF1E1E2E),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem conta", color = Color.White) },
                            onClick = { selectedAccountId = null; accountExpanded = false },
                        )
                        uiState.bankAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.bankName} · ${account.currency}", color = Color.White) },
                                onClick = { selectedAccountId = account.id; accountExpanded = false },
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = it },
            ) {
                OutlinedTextField(
                    // `first {}` rebentava a editar uma recorrente cuja
                    // frequência já não está na lista — a diária, agora que
                    // saiu. Mostra-se a chave crua em vez de deitar o ecrã
                    // abaixo.
                    value = frequencies.firstOrNull { it.first == frequency }?.second ?: frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequência") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false },
                    containerColor = Color(0xFF1E1E2E),
                ) {
                    frequencies.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            onClick = { frequency = key; frequencyExpanded = false },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = if (useBusinessDay) businessDay else dueDay,
                    onValueChange = {
                        val digits = it.filter { c -> c.isDigit() }
                        if (useBusinessDay) businessDay = digits else dueDay = digits
                    },
                    label = { Text(if (useBusinessDay) "N.º dia útil" else "Dia do vencimento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = occurrences,
                    onValueChange = { onOccurrencesTyped(it) },
                    label = { Text("Parcelas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(checked = useBusinessDay, onCheckedChange = { useBusinessDay = it })
                Column {
                    Text("Vence em dia útil", color = Color.White, fontSize = 14.sp)
                    Text(
                        if (useBusinessDay)
                            "O $businessDay.º dia útil do mês (feriados não contam)."
                        else "Dia fixo do mês.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                    )
                }
            }

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Início (opcional)") },
                placeholder = { Text("dd/mm/aaaa") },
                trailingIcon = {
                    TextButton(onClick = { datePickerFor = "start" }) { Text("Escolher") }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Término (opcional)") },
                placeholder = { Text("dd/mm/aaaa") },
                trailingIcon = {
                    TextButton(onClick = { datePickerFor = "end" }) { Text("Escolher") }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(checked = notification, onCheckedChange = { notification = it })
                Text("Avisar no dia do vencimento", color = Color.White, fontSize = 14.sp)
            }

            if ((occurrences.toIntOrNull() ?: 0) > 0) {
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { onTotalTyped(it) },
                    label = { Text("Valor total (${currencyConfig.symbol})") },
                    supportingText = {
                        Text("Escreva no total ou na parcela — o outro acompanha.")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val parcela = parseAmountInput(amount) ?: return@Button
                    val categoria = selectedCategoryId ?: return@Button
                    val diaFixo = if (useBusinessDay) null else (dueDay.toIntOrNull() ?: 1)
                    val diaUtil = if (useBusinessDay) (businessDay.toIntOrNull() ?: 5) else null
                    if (existing == null) {
                        viewModel.create(
                            description = description.trim(),
                            amount = parcela,
                            type = type,
                            frequency = frequency,
                            dueDay = diaFixo,
                            businessDay = diaUtil,
                            categoryId = categoria,
                            accountId = selectedAccountId,
                            startDate = startDate.ifBlank { null },
                            endDate = endDate.ifBlank { null },
                            occurrences = occurrences.toIntOrNull() ?: 12,
                            notification = notification,
                            totalAmount = parseAmountInput(totalAmount),
                            currency = currencyConfig.code,
                        )
                    } else {
                        viewModel.update(
                            id = existing.id,
                            description = description.trim(),
                            amount = parcela,
                            type = type,
                            frequency = frequency,
                            dueDay = diaFixo,
                            businessDay = diaUtil,
                            categoryId = categoria,
                            accountId = selectedAccountId,
                            startDate = startDate.ifBlank { null },
                            endDate = endDate.ifBlank { null },
                            occurrences = occurrences.toIntOrNull() ?: 12,
                            notification = notification,
                            totalAmount = parseAmountInput(totalAmount),
                            currency = currencyConfig.code,
                        )
                    }
                },
                enabled = isValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green80),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SurfaceDark, strokeWidth = 2.dp)
                } else {
                    Text(if (existing == null) "Criar Recorrente" else "Salvar Alterações", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // O mesmo padrão do EditTransactionScreen: guarda ISO, o diálogo mostra o
    // calendário do sistema (que já vem no idioma do telemóvel).
    datePickerFor?.let { alvo ->
        val atual = if (alvo == "start") startDate else endDate
        val initialMillis = runCatching {
            LocalDate.parse(atual).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val iso = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        if (alvo == "start") startDate = iso else endDate = iso
                    }
                    datePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (alvo == "start") startDate = "" else endDate = ""
                    datePickerFor = null
                }) { Text("Limpar") }
            },
        ) { DatePicker(state = state) }
    }

}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green80, focusedLabelColor = Green80, cursorColor = Green80,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f), unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
)
