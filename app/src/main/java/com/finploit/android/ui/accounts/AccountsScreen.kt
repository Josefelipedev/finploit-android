package com.finploit.android.ui.accounts

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.AccountType
import com.finploit.android.data.dto.BankAccountDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.CURRENCY_OPTIONS
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.WarningAmber
import com.finploit.android.ui.theme.currencyConfigByCode
import com.finploit.android.util.filterAmountInput
import com.finploit.android.util.parseAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showForm by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccountDto?>(null) }
    var confirmDeleteId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            if (uiState.message != null) {
                showForm = false
                editingAccount = null
            }
            viewModel.clearMessage()
        }
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
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "As contas do casal, cada uma na sua moeda. Ao lançar uma transação pela conta, a moeda já vem certa.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )

                if (showForm || editingAccount != null) {
                    AccountForm(
                        editing = editingAccount,
                        isSaving = uiState.isSaving,
                        onCancel = {
                            showForm = false
                            editingAccount = null
                        },
                        onSubmit = { bankName, accountNumber, agency, balance, creditLimit, creditUsed, currency, accountType ->
                            val editing = editingAccount
                            if (editing != null) {
                                viewModel.updateAccount(
                                    id = editing.id,
                                    bankName = bankName,
                                    accountNumber = accountNumber,
                                    agency = agency,
                                    balance = balance,
                                    creditLimit = creditLimit,
                                    creditUsed = creditUsed,
                                    currency = currency,
                                    accountType = accountType.apiValue,
                                )
                            } else {
                                viewModel.createAccount(
                                    bankName = bankName,
                                    accountNumber = accountNumber,
                                    agency = agency,
                                    balance = balance,
                                    creditLimit = creditLimit,
                                    creditUsed = creditUsed,
                                    currency = currency,
                                    accountType = accountType.apiValue,
                                )
                            }
                        },
                    )
                } else {
                    Button(
                        onClick = { showForm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = BackgroundDark,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nova conta", fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.accounts.isEmpty() && !showForm && editingAccount == null) {
                    SectionCard {
                        Text(
                            "Nenhuma conta cadastrada",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Cadastre as contas de vocês dois — ex.: a sua em euro e a dela em real.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    uiState.accounts.forEach { account ->
                        AccountCard(
                            account = account,
                            confirmDelete = confirmDeleteId == account.id,
                            onEdit = {
                                editingAccount = account
                                showForm = false
                                confirmDeleteId = null
                            },
                            onDelete = {
                                if (confirmDeleteId == account.id) {
                                    viewModel.deleteAccount(account.id)
                                    confirmDeleteId = null
                                } else {
                                    confirmDeleteId = account.id
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountForm(
    editing: BankAccountDto?,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSubmit: (
        bankName: String,
        accountNumber: String?,
        agency: String?,
        balance: Double?,
        creditLimit: Double?,
        creditUsed: Double?,
        currency: String,
        accountType: AccountType,
    ) -> Unit,
) {
    var bankName by remember { mutableStateOf(editing?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf(editing?.accountNumber ?: "") }
    var agency by remember { mutableStateOf(editing?.agency ?: "") }
    var balance by remember { mutableStateOf(editing?.balance?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var creditLimit by remember { mutableStateOf(editing?.creditLimit?.toString() ?: "") }
    var creditUsed by remember { mutableStateOf(editing?.creditUsed?.toString() ?: "") }
    var currency by remember { mutableStateOf(editing?.currency ?: "BRL") }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var accountType by remember { mutableStateOf(AccountType.from(editing?.accountType)) }

    val selectedCurrency = currencyConfigByCode(currency)

    SectionCard {
        Text(
            if (editing != null) "Editar conta" else "Nova conta",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Banco") },
            placeholder = { Text("Ex.: Millennium, Nubank") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = { Text("Nº da conta (opcional)") },
            placeholder = { Text("••••1234") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = agency,
            onValueChange = { agency = it },
            label = { Text("Agência (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(12.dp))

        // Currency picker
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
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
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

        // O campo que faltava: até aqui a única maneira de dizer "isto é um
        // cartão" era preencher o limite, e quem só queria marcar o tipo não
        // tinha onde.
        Text("Tipo de conta", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            AccountType.entries.forEach { tipo ->
                val ativo = accountType == tipo
                FilterChip(
                    selected = ativo,
                    onClick = { accountType = tipo },
                    label = { Text(tipo.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary,
                        selectedContainerColor = GreenPrimary,
                        selectedLabelColor = BackgroundDark,
                    ),
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = balance,
            onValueChange = { balance = filterAmountInput(it) },
            // Já não é "saldo atual": os lançamentos somam-se por cima deste
            // valor (C5). Escrever aqui o saldo do banco de hoje seria contar
            // os movimentos duas vezes.
            label = { Text("Saldo inicial (opcional)") },
            placeholder = { Text("0,00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = creditLimit,
            onValueChange = { creditLimit = filterAmountInput(it) },
            label = { Text("Limite de crédito (opcional)") },
            supportingText = { Text("É uma referência; não entra no saldo.") },
            placeholder = { Text("0,00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(12.dp))

        // "Disponível" é conta: limite menos o que já se deve. Sem este campo,
        // o número no ecrã das contas era o limite total — que não diz o que
        // ainda dá para gastar.
        OutlinedTextField(
            value = creditUsed,
            onValueChange = { creditUsed = filterAmountInput(it) },
            label = { Text("Em débito (opcional)") },
            supportingText = {
                val disponivel = creditLimit.toDoubleOrNull()?.let { limite ->
                    creditUsed.toDoubleOrNull()?.let { usado -> limite - usado }
                }
                Text(
                    if (disponivel != null) "Disponível: ${currency.format(disponivel)}"
                    else "O que já deve neste cartão.",
                )
            },
            placeholder = { Text("0,00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancelar", color = TextSecondary)
            }
            Button(
                onClick = {
                    if (bankName.isBlank()) return@Button
                    onSubmit(
                        bankName.trim(),
                        accountNumber.trim().ifBlank { null },
                        agency.trim().ifBlank { null },
                        parseAmountInput(balance),
                        parseAmountInput(creditLimit),
                        parseAmountInput(creditUsed),
                        currency,
                        accountType,
                    )
                },
                enabled = !isSaving && bankName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = BackgroundDark,
                ),
                modifier = Modifier.weight(1f),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = BackgroundDark,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (editing != null) "Guardar" else "Salvar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: BankAccountDto,
    confirmDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val cfg = currencyConfigByCode(account.currency)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.bankName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    AccountType.from(account.accountType).label +
                        (account.accountNumber?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
                account.agency?.takeIf { it.isNotBlank() }?.let {
                    Text("Agência $it", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .background(CardElevated, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("${cfg.flag} ${account.currency}", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        // Saldo derivado (C5): o valor escrito à mão é o ponto de partida e os
        // lançamentos ligados à conta somam-se por cima. O campo manual não
        // acompanhava nada e mentia a partir do primeiro movimento.
        Text(
            cfg.format(account.currentBalance ?: account.balance),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
        val mov = account.movements
        Text(
            if (mov != null && mov.count > 0) {
                "Início ${cfg.format(account.initialBalance ?: account.balance)} · " +
                    "+${cfg.format(mov.income)} −${cfg.format(mov.expense)} " +
                    "(${mov.count} ${if (mov.count == 1) "movimento" else "movimentos"})"
            } else {
                "Saldo inicial · sem lançamentos ligados a esta conta"
            },
            color = TextDisabled,
            fontSize = 11.sp,
        )
        account.creditLimit?.let { limit ->
            Spacer(Modifier.height(10.dp))
            Text(
                "Limite de crédito · não é saldo",
                color = WarningAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                cfg.format(limit),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Editar",
                color = GreenPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onEdit() },
            )
            Text(
                if (confirmDelete) "Confirmar arquivamento?" else "Arquivar",
                color = ExpenseRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onDelete() },
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    focusedLabelColor = GreenPrimary,
    cursorColor = GreenPrimary,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
)
