package com.finploit.android.ui.pantry

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.PantryItemDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(viewModel: PantryViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (state.showAddDialog) {
        AddPantryItemDialog(
            editing = state.editingItem,
            name = state.nameInput,
            quantity = state.quantityInput,
            unit = state.unitInput,
            category = state.categoryInput,
            isSaving = state.isSaving,
            onNameChange = viewModel::setName,
            onQuantityChange = viewModel::setQuantity,
            onUnitChange = viewModel::setUnit,
            onCategoryChange = viewModel::setCategory,
            onConfirm = viewModel::save,
            onDismiss = viewModel::closeDialog,
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = CardBackground, contentColor = TextPrimary)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("🏠 Estoque / Pantry", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearAll) {
                            Text("Limpar tudo", color = Color(0xFFEF5350), fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = GreenPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = BackgroundDark)
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }

        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏠", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Estoque vazio", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Adiciona os ingredientes que tens em casa.", color = TextDisabled, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("A IA vai usá-los no próximo cardápio.", color = TextDisabled, fontSize = 12.sp)
                }
            }
            return@Scaffold
        }

        val grouped = state.items.groupBy { it.category ?: "Outros" }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(GreenPrimary.copy(alpha = 0.08f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.items.size} ingredientes no estoque — a IA usará estes no próximo cardápio.",
                        color = GreenPrimary, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
            }
            grouped.forEach { (category, categoryItems) ->
                item(key = "cat_$category") {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(GreenPrimary))
                        Spacer(Modifier.width(8.dp))
                        Text(category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("(${categoryItems.size})", color = TextDisabled, fontSize = 11.sp)
                    }
                }
                items(categoryItems, key = { it.id }) { item ->
                    PantryItemRow(item = item, onEdit = { viewModel.openAddDialog(item) }, onRemove = { viewModel.remove(item.id) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PantryItemRow(item: PantryItemDto, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                val detail = buildString {
                    if (item.quantity != null) {
                        append(if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString() else "%.1f".format(item.quantity))
                        if (item.unit != null) append(" ${item.unit}")
                    } else if (item.unit != null) {
                        append(item.unit)
                    }
                }
                if (detail.isNotEmpty()) Text(detail, color = TextDisabled, fontSize = 12.sp)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color(0xFFEF5350).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPantryItemDialog(
    editing: PantryItemDto?,
    name: String,
    quantity: String,
    unit: String,
    category: String,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenPrimary,
        focusedLabelColor = GreenPrimary,
        unfocusedBorderColor = TextDisabled.copy(alpha = 0.4f),
        unfocusedLabelColor = TextDisabled,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = GreenPrimary,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(if (editing != null) "Editar item" else "Adicionar ao estoque", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Ingrediente *") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantity, onValueChange = onQuantityChange, label = { Text("Qtd.") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = unit, onValueChange = onUnitChange, label = { Text("Unidade") }, modifier = Modifier.weight(1f), colors = fieldColors, singleLine = true)
                }
                OutlinedTextField(value = category, onValueChange = onCategoryChange, label = { Text("Categoria (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors, singleLine = true)
                Text("Ex: Proteína, Vegetal, Fruta, Grãos", color = TextDisabled, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.trim().isNotEmpty() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp)
                else Text("Guardar", color = BackgroundDark)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDisabled) } },
    )
}
