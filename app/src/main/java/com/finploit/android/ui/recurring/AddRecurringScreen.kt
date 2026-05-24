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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.SurfaceDark

private val frequencies = listOf(
    "monthly" to "Mensal",
    "weekly" to "Semanal",
    "daily" to "Diário",
    "yearly" to "Anual",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    viewModel: RecurringViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var type by remember { mutableStateOf("expense") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("monthly") }
    var dueDay by remember { mutableStateOf("1") }
    var categoria by remember { mutableStateOf("") }
    var occurrences by remember { mutableStateOf("12") }
    var frequencyExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) { viewModel.clearSaveState(); onDismiss() }
    }
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { snackbarHostState.showSnackbar(it); viewModel.clearSaveState() }
    }

    val isValid = description.isNotBlank() && amount.toDoubleOrNull() != null && categoria.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Nova Recorrente", fontWeight = FontWeight.Bold) },
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
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Valor (${LocalCurrencyConfig.current.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = categoria,
                onValueChange = { categoria = it },
                label = { Text("Categoria") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp),
            )

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = it },
            ) {
                OutlinedTextField(
                    value = frequencies.first { it.first == frequency }.second,
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
                    value = dueDay,
                    onValueChange = { dueDay = it.filter { c -> c.isDigit() } },
                    label = { Text("Dia do vencimento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = occurrences,
                    onValueChange = { occurrences = it.filter { c -> c.isDigit() } },
                    label = { Text("Ocorrências") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.create(
                        description = description.trim(),
                        amount = amount.toDoubleOrNull() ?: return@Button,
                        type = type,
                        frequency = frequency,
                        dueDay = dueDay.toIntOrNull() ?: 1,
                        categoria = categoria.trim(),
                        endDate = "2099-12-31",
                        occurrences = occurrences.toIntOrNull() ?: 12,
                    )
                },
                enabled = isValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green80),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SurfaceDark, strokeWidth = 2.dp)
                } else {
                    Text("Criar Recorrente", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green80, focusedLabelColor = Green80, cursorColor = Green80,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f), unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
)
