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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.ShoppingItemDto
import com.finploit.android.data.dto.StorePriceDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextDisabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListDetailScreen(
    viewModel: ShoppingViewModel,
    onBack: () -> Unit,
    onSearchPrices: (List<EnrichItem>) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val list = uiState.selectedList ?: return

    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showAddDialog) {
        AddItemDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, price ->
                viewModel.addOrUpdateItem(list.id, name, qty, unit, price)
            },
        )
    }

    // Diálogo de comparação de preços por loja
    if (uiState.storePricesItem != null) {
        StorePricesDialog(
            itemName = uiState.storePricesItem!!,
            prices = uiState.storePrices,
            isLoading = uiState.isLoadingPrices,
            onDismiss = { viewModel.clearStorePrices() },
        )
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) { showAddDialog = false; viewModel.clearSaveState() }
    }

    LaunchedEffect(uiState.enrichResult) {
        uiState.enrichResult?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearEnrichResult()
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceDark,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TopAppBar(
                title = { Text(list.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar item", tint = Green80)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                ),
            )

            val purchased = list.items.count { it.purchased }
            val scrapedTotal = list.items.sumOf { it.scrapedPrice ?: it.price ?: 0.0 }
            val total = list.items.sumOf { it.price ?: 0.0 }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("$purchased/${list.items.size} itens", color = Color.Gray, fontSize = 13.sp)
                Column(horizontalAlignment = Alignment.End) {
                    if (scrapedTotal > 0 && scrapedTotal != total) {
                        Text("Scraper: ${LocalCurrencyConfig.current.format(scrapedTotal)}", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (total > 0) Text("Estimado: ${LocalCurrencyConfig.current.format(total)}", color = Green80, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Botões de ação
            val pendingItems = list.items.filter { !it.purchased }
            if (pendingItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Buscar preços via GrocerySearch (navegação existente)
                    Button(
                        onClick = {
                            onSearchPrices(pendingItems.map { i ->
                                EnrichItem(i.name, i.quantity, i.unit ?: "un", i.price ?: 0.0)
                            })
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary.copy(alpha = 0.15f)),
                    ) {
                        Text("🔍", fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("Pesquisar", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // Actualizar preços e guardar no item
                    Button(
                        onClick = { viewModel.enrichPrices(list.id) },
                        enabled = !uiState.isEnriching,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A2A)),
                    ) {
                        if (uiState.isEnriching) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GreenPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("💶", fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Actualizar preços", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (list.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Lista vazia", color = Color.Gray)
                        Text("Toque em + para adicionar itens", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(list.items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggle = { viewModel.toggleItem(item.id, !item.purchased, list.id) },
                            onDelete = { viewModel.deleteItem(item.id, list.id) },
                            onShowPrices = { viewModel.loadStorePrices(item.name) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemDto,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onShowPrices: () -> Unit = {},
) {
    val supermarketEmoji = when (item.supermarket) {
        "continente" -> "🏪"
        "auchan" -> "🟠"
        "mercadona" -> "🟢"
        "pingodoce" -> "🟡"
        else -> null
    }
    val supermarketLabel = item.supermarket?.replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onShowPrices),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.purchased) CardBackground.copy(alpha = 0.5f) else CardBackground,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.purchased,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Green80,
                    uncheckedColor = Color.Gray,
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = if (item.purchased) Color.Gray else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.purchased) TextDecoration.LineThrough else null,
                    fontSize = 14.sp,
                )
                val subtitle = buildString {
                    append("${item.quantity} ${item.unit ?: "un."}")
                    item.price?.takeIf { it > 0 }?.let { append(" · ${LocalCurrencyConfig.current.format(it)}") }
                }
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                // Scraper price + supermarket badge
                if (item.scrapedPrice != null && item.scrapedPrice > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GreenPrimary.copy(alpha = 0.15f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (supermarketEmoji != null) {
                                    Text(supermarketEmoji, fontSize = 11.sp)
                                    Spacer(Modifier.width(3.dp))
                                }
                                if (supermarketLabel != null) {
                                    Text(supermarketLabel, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    LocalCurrencyConfig.current.format(item.scrapedPrice),
                                    color = GreenPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = ExpenseRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StorePricesDialog(
    itemName: String,
    prices: List<StorePriceDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Preços: $itemName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp)
                    }
                } else if (prices.isEmpty()) {
                    Text("Sem preços disponíveis.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    val sorted = prices.sortedBy { it.price }
                    sorted.forEachIndexed { idx, p ->
                        val isFirst = idx == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFirst) Text("🏆 ", fontSize = 13.sp)
                                Text(
                                    p.supermarket.replaceFirstChar { it.uppercase() },
                                    color = if (isFirst) GreenPrimary else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                            Text(
                                LocalCurrencyConfig.current.format(p.price),
                                color = if (isFirst) GreenPrimary else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                        if (idx < sorted.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Fechar", color = GreenPrimary)
                }
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("un.") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Novo Item", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nome") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Qtd") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = dialogFieldColors(),
                    )
                    OutlinedTextField(
                        value = unit, onValueChange = { unit = it },
                        label = { Text("Unidade") }, singleLine = true,
                        modifier = Modifier.weight(1f), colors = dialogFieldColors(),
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Preço Total (${LocalCurrencyConfig.current.symbol})") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.trim(),
                        quantity.toDoubleOrNull() ?: 1.0,
                        unit.ifBlank { "un." },
                        price.toDoubleOrNull() ?: 0.0,
                    )
                },
                enabled = name.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Green80),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SurfaceDark, strokeWidth = 2.dp)
                else Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green80, focusedLabelColor = Green80, cursorColor = Green80,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f), unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
)
