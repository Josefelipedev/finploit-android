package com.finploit.android.ui.transactions

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.SurfaceDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    tx: FinanceItemDto,
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var type by remember { mutableStateOf(tx.type ?: "expense") }
    var amount by remember { mutableStateOf(tx.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(tx.description ?: "") }
    // A data do movimento, não a da criação: prefixar com `createdAt` fazia
    // qualquer edição mudar em silêncio o dia do lançamento para o dia em que
    // tinha sido digitado.
    var referenceDate by remember {
        mutableStateOf(tx.movementDate.takeIf { it.length == 10 } ?: LocalDate.now().toString())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.editingTransaction) {
        if (uiState.editingTransaction == null && !uiState.isUpdating) onDismiss()
    }

    LaunchedEffect(uiState.updateError) {
        uiState.updateError?.let { snackbarHostState.showSnackbar(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Editar Transação", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.clearEdit(); onDismiss() }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tipo
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

            // Valor — aceita vírgula (separador BRL) e ponto
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Valor (${LocalCurrencyConfig.current.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = editFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            // Descrição
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = editFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            // Data da transação
            OutlinedTextField(
                value = referenceDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Data") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar data", tint = Green80)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = editFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val amountValue = amount.replace(',', '.').toDoubleOrNull() ?: return@Button
                    viewModel.updateTransaction(
                        id = tx.id,
                        type = type,
                        amount = amountValue,
                        description = description.ifBlank { null },
                        referenceDate = referenceDate,
                    )
                },
                enabled = amount.isNotBlank() && amount.replace(',', '.').toDoubleOrNull() != null && !uiState.isUpdating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green80),
            ) {
                if (uiState.isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SurfaceDark, strokeWidth = 2.dp)
                } else {
                    Text("Salvar Alterações", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // DatePickerDialog
    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(referenceDate).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        referenceDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green80,
    focusedLabelColor = Green80,
    cursorColor = Green80,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
)
