package com.finploit.android.ui.mealplanner

import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.MealShoppingItemDto
import com.finploit.android.data.dto.parsedUsedInDays
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.util.filterAmountInput
import com.finploit.android.util.parseAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingTab(
    items: List<MealShoppingItemDto>,
    totalEstimate: Double?,
    tips: String?,
    onToggle: (Int) -> Unit,
    onSearchPrices: (List<EnrichItem>) -> Unit = {},
    onExportToShoppingList: () -> Unit = {},
    isExporting: Boolean = false,
    enrichedItems: Map<String, EnrichedShoppingItemDto> = emptyMap(),
    isEnrichingPrices: Boolean = false,
    onEnrichPrices: () -> Unit = {},
    enrichedAt: Long? = null,
    shoppingFilter: ShoppingFilter = ShoppingFilter.PENDING,
    onResetShopping: () -> Unit = {},
    isResettingShopping: Boolean = false,
    /** Fechar a lista lança a despesa do que se comprou (C4). */
    closedAt: String? = null,
    onCloseShopping: () -> Unit = {},
    onReopenShopping: () -> Unit = {},
    isClosingShopping: Boolean = false,
    onFilterChange: (ShoppingFilter) -> Unit = {},
    collapsedCategories: Set<String> = emptySet(),
    onToggleCategory: (String) -> Unit = {},
    onBuyAllInCategory: (String) -> Unit = {},
    pantryAddSuggestion: MealShoppingItemDto? = null,
    onDismissPantrySuggestion: () -> Unit = {},
    onAddSuggestionToPantry: () -> Unit = {},
    onAddCustomItem: (String, Double, String, String?, Double?) -> Unit = { _, _, _, _, _ -> },
    onUpdateActualPrice: (Int, Double?) -> Unit = { _, _ -> },
    selectedSupermarketFilter: String? = null,
    onSupermarketFilterChange: (String?) -> Unit = {},
) {
    val context = LocalContext.current
    val currencyConfig = LocalCurrencyConfig.current

    var searchQuery by remember { mutableStateOf("") }
    var priceDialogItem by remember { mutableStateOf<MealShoppingItemDto?>(null) }
    var priceDialogInput by remember { mutableStateOf("") }

    priceDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { priceDialogItem = null },
            containerColor = CardBackground,
            title = { Text("Preço real pago", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = priceDialogInput,
                    onValueChange = { priceDialogInput = filterAmountInput(it) },
                    label = { Text("Valor pago (${currencyConfig.symbol})", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary, cursorColor = GreenPrimary),
                )
            },
            confirmButton = {
                Button(
                    onClick = { onUpdateActualPrice(item.id, parseAmountInput(priceDialogInput)); priceDialogItem = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) { Text("Guardar", color = BackgroundDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { onUpdateActualPrice(item.id, null); priceDialogInput = ""; priceDialogItem = null }) {
                    Text("Limpar", color = TextDisabled)
                }
            },
        )
    }

    // Pantry suggestion dialog
    pantryAddSuggestion?.let { suggestion ->
        AlertDialog(
            onDismissRequest = onDismissPantrySuggestion,
            containerColor = CardBackground,
            title = { Text("Adicionar à despensa?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Acabou de comprar ${suggestion.name}. Quer adicioná-lo à despensa para o plano alimentar considerar que já o tem em casa?", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = onAddSuggestionToPantry, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text("Sim, adicionar", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onDismissPantrySuggestion) { Text("Não", color = TextDisabled) } },
        )
    }

    // Add custom item dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemUnit by remember { mutableStateOf("un") }
    var newItemCategory by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardBackground,
            title = { Text("Adicionar item", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newItemName, onValueChange = { newItemName = it },
                        label = { Text("Nome *", fontSize = 12.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary, cursorColor = GreenPrimary),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newItemQty, onValueChange = { newItemQty = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Qtd", fontSize = 12.sp) }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary, cursorColor = GreenPrimary),
                        )
                        OutlinedTextField(
                            value = newItemUnit, onValueChange = { newItemUnit = it },
                            label = { Text("Und.", fontSize = 12.sp) }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary, cursorColor = GreenPrimary),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newItemCategory, onValueChange = { newItemCategory = it },
                            label = { Text("Categoria", fontSize = 12.sp) }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary, cursorColor = GreenPrimary),
                        )
                        OutlinedTextField(
                            value = newItemPrice, onValueChange = { newItemPrice = filterAmountInput(it) },
                            label = { Text("Preço est.", fontSize = 12.sp) }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary, cursorColor = GreenPrimary),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            onAddCustomItem(newItemName, newItemQty.toDoubleOrNull() ?: 1.0, newItemUnit, newItemCategory.ifBlank { null }, parseAmountInput(newItemPrice))
                            newItemName = ""; newItemQty = "1"; newItemUnit = "un"; newItemCategory = ""; newItemPrice = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) { Text("Adicionar", color = BackgroundDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar", color = TextDisabled) } },
        )
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛒", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Nenhuma lista de compras", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Gere um cardápio para criar sua lista", color = TextDisabled, fontSize = 13.sp)
            }
        }
        return
    }

    val purchased = items.count { it.purchased }
    val total = items.size
    val filteredItems = items.let { all ->
        val byFilter = when (shoppingFilter) {
            ShoppingFilter.ALL -> all
            ShoppingFilter.PENDING -> all.filter { !it.purchased }
            ShoppingFilter.PURCHASED -> all.filter { it.purchased }
        }
        if (searchQuery.isBlank()) byFilter
        else byFilter.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }
    val grouped = filteredItems.groupBy { it.category ?: "Outros" }
        .entries.sortedBy { categorySortKey(it.key) }
        .associate { it.key to it.value }
    val remainingEstimate = items.filter { !it.purchased }.sumOf { i ->
        enrichedItems[i.name.trim().lowercase()]?.bestPrice ?: i.estimatedPrice ?: 0.0
    }
    // O que a despesa vai valer ao fechar (C4): o preço pago quando existe, o
    // estimado quando ainda não se preencheu. A mesma conta que o servidor faz.
    val spentSoFar = items.filter { it.purchased }.sumOf { i ->
        i.actualPrice ?: i.estimatedPrice ?: 0.0
    }

    val enrichItems = items.filter { !it.purchased }.map { i ->
        EnrichItem(name = i.name, quantity = i.quantity, unit = i.unit, estimatedPrice = i.estimatedPrice ?: 0.0)
    }
    val pendingCount = total - purchased

    val hasEnriched = enrichedItems.isNotEmpty()
    val enrichedTotal = remember(enrichedItems) { enrichedItems.values.sumOf { it.bestPrice ?: it.estimatedPrice } }
    val aiTotal = remember(enrichedItems) { enrichedItems.values.sumOf { it.estimatedPrice } }
    val savings = aiTotal - enrichedTotal
    val bestStore = remember(enrichedItems) {
        if (!hasEnriched) null
        else enrichedItems.values
            .mapNotNull { it.bestSource }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShoppingFilter.entries.forEach { f ->
                    val selected = shoppingFilter == f
                    val label = when (f) {
                        ShoppingFilter.ALL -> "Todos"
                        ShoppingFilter.PENDING -> "Pendentes"
                        ShoppingFilter.PURCHASED -> "Comprados"
                    }
                    val count = when (f) {
                        ShoppingFilter.ALL -> total
                        ShoppingFilter.PENDING -> total - purchased
                        ShoppingFilter.PURCHASED -> purchased
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterChange(f) },
                        label = { Text("$label ($count)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = GreenPrimary,
                        ),
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar item…", fontSize = 13.sp, color = TextDisabled) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", tint = TextDisabled, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = TextDisabled.copy(alpha = 0.3f),
                    cursorColor = GreenPrimary,
                ),
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("$purchased de $total itens comprados", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.width(180.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(TextDisabled.copy(alpha = 0.2f))) {
                                if (total > 0) Box(modifier = Modifier.fillMaxWidth(purchased.toFloat() / total).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(GreenPrimary, IncomeGreen))))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Restante", color = TextDisabled, fontSize = 11.sp)
                            Text(currencyConfig.format(remainingEstimate), color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            totalEstimate?.let { Text("de ${currencyConfig.format(it)}", color = TextDisabled, fontSize = 11.sp) }
                        }
                    }
                    if (purchased == total && total > 0) {
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GreenPrimary.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("🎉 Todas as compras concluídas!", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (isResettingShopping) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GreenPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        "↺ Reiniciar",
                                        color = GreenPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GreenPrimary.copy(alpha = 0.12f))
                                            .clickable { onResetShopping() }
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                    // C4: fechar a lista é o que faz o cardápio chegar aos
                    // gastos. O total é o preço pago (ou o estimado, quando
                    // ainda não se preencheu) dos itens comprados.
                    Spacer(Modifier.height(10.dp))
                    if (closedAt != null) {
                        Text(
                            "Lista fechada — ${currencyConfig.format(spentSoFar)} já lançados como despesa.",
                            color = GreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (isClosingShopping) "A reabrir..." else "↩︎ Reabrir (apaga a despesa)",
                            color = TextDisabled,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TextDisabled.copy(alpha = 0.12f))
                                .clickable(enabled = !isClosingShopping) { onReopenShopping() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    } else if (purchased > 0) {
                        Text(
                            if (isClosingShopping) {
                                "A fechar..."
                            } else {
                                "💸 Fechar e lançar ${currencyConfig.format(spentSoFar)}"
                            },
                            color = BackgroundDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GreenPrimary)
                                .clickable(enabled = !isClosingShopping) { onCloseShopping() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    enrichedAt?.let { ts ->
                        val time = remember(ts) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts)) }
                        Text("Preços actualizados às $time", color = TextDisabled, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEnrichPrices,
                    enabled = enrichItems.isNotEmpty() && !isEnrichingPrices,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.85f),
                        disabledContainerColor = TextDisabled.copy(alpha = 0.2f),
                    ),
                ) {
                    if (isEnrichingPrices) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("A buscar preços...", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("💶", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Actualizar preços nos supermercados", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = { onSearchPrices(enrichItems) },
                    enabled = enrichItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.85f), disabledContainerColor = TextDisabled.copy(alpha = 0.2f)),
                ) {
                    Text("🛒", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver comparação detalhada", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        if (hasEnriched && enrichedItems.values.all { it.bestPrice == null }) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD740).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFFFD740).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Column {
                        Text("Nenhum preço encontrado", color = Color(0xFFFFD740), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("O scraper pode estar a reiniciar. Tenta de novo em alguns segundos.", color = TextDisabled, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
        if (hasEnriched && savings > 0.50) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(GreenPrimary.copy(alpha = 0.10f))
                        .border(1.dp, GreenPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("💰 Poupança potencial", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("vs estimativa da IA", color = TextDisabled, fontSize = 11.sp)
                    }
                    Text(currencyConfig.format(savings), color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        if (bestStore != null) {
            val storeEmoji = when (bestStore.lowercase()) {
                "continente" -> "🏪"
                "auchan" -> "🟠"
                "pingodoce" -> "🟡"
                "mercadona" -> "🟢"
                else -> "🏬"
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1565C0).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("$storeEmoji Melhor supermercado", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("mais itens ao melhor preço", color = TextDisabled, fontSize = 11.sp)
                    }
                    Text(bestStore.replaceFirstChar { it.uppercase() }, color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
        if (hasEnriched) {
            item {
                val storeLabels = listOf(
                    null to "🏬 Todos",
                    "continente" to "🏪 Continente",
                    "auchan" to "🟠 Auchan",
                    "pingodoce" to "🟡 Pingo Doce",
                )
                Column {
                    Text("Filtrar por supermercado", color = TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        storeLabels.forEach { (store, label) ->
                            val selected = selectedSupermarketFilter == store
                            // Compute total for this store
                            val storeTotal = if (store == null) {
                                enrichedItems.values.sumOf { it.bestPrice ?: it.estimatedPrice }
                            } else {
                                enrichedItems.values.sumOf { item ->
                                    item.products.filter { it.source.lowercase() == store }
                                        .mapNotNull { it.price }.minOrNull() ?: 0.0
                                }
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSupermarketFilterChange(if (selected) null else store) },
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, fontSize = 11.sp)
                                        if (storeTotal > 0.0) Text(currencyConfig.format(storeTotal), fontSize = 9.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = GreenPrimary,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
            ) {
                Text("➕", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar item avulso", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
        item {
            val shareText = buildString {
                append("🛒 Lista de Compras — Plano Alimentar\n\n")
                grouped.forEach { (cat, catItems) ->
                    append("📦 $cat\n")
                    catItems.forEach { i ->
                        val qty = if (i.quantity == i.quantity.toLong().toDouble()) i.quantity.toLong().toString() else "%.1f".format(i.quantity)
                        append("  • ${i.name} — $qty ${i.unit}")
                        i.estimatedPrice?.let { append(" (~${currencyConfig.format(it)})") }
                        append("\n")
                    }
                    append("\n")
                }
                totalEstimate?.let { append("💰 Total estimado: ${currencyConfig.format(it)}") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar lista"))
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Partilhar", color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                if (pendingCount > 0) {
                    Button(
                        onClick = onExportToShoppingList,
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary.copy(alpha = 0.12f),
                            disabledContainerColor = TextDisabled.copy(alpha = 0.10f),
                        ),
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GreenPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("📋", fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Exportar ($pendingCount)", color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        tips?.takeIf { it.isNotEmpty() }?.let { tip ->
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFD740).copy(alpha = 0.08f)).border(1.dp, Color(0xFFFFD740).copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("💰", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(tip, color = Color(0xFFFFD740).copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        if (shoppingFilter == ShoppingFilter.PURCHASED && purchased == 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhum item comprado ainda.", color = TextDisabled, fontSize = 13.sp)
                    }
                }
            }
        }
        if (shoppingFilter == ShoppingFilter.PENDING && purchased == total) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Todas as compras concluídas!", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        grouped.forEach { (category, categoryItems) ->
            val collapsed = category in collapsedCategories
            val allCategoryItems = items.filter { (it.category ?: "Outros") == category }
            val catPurchased = allCategoryItems.count { it.purchased }
            val catPending = allCategoryItems.count { !it.purchased }
            item(key = "header_$category") {
                val catEstimate = categoryItems.sumOf { enrichedItems[it.name.trim().lowercase()]?.bestPrice ?: it.estimatedPrice ?: 0.0 }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleCategory(category) }.padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenPrimary))
                    Spacer(Modifier.width(8.dp))
                    Text(category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("$catPurchased/${allCategoryItems.size}", color = if (catPurchased == allCategoryItems.size) GreenPrimary else TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (catPending > 0 && shoppingFilter != ShoppingFilter.PURCHASED) {
                        TextButton(
                            onClick = { onBuyAllInCategory(category) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("Comprar tudo", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text(currencyConfig.format(catEstimate), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
                    }
                    Icon(
                        imageVector = if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (!collapsed) {
                items(categoryItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        enriched = enrichedItems[item.name.trim().lowercase()],
                        onToggle = { onToggle(item.id) },
                        onPriceClick = {
                            priceDialogInput = item.actualPrice?.let { "%.2f".format(it) } ?: ""
                            priceDialogItem = item
                        },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
internal fun ShoppingItemRow(
    item: MealShoppingItemDto,
    enriched: EnrichedShoppingItemDto? = null,
    onToggle: () -> Unit,
    onPriceClick: () -> Unit = {},
) {
    val qty = if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString() else "%.1f".format(item.quantity)
    val currencyConfig = LocalCurrencyConfig.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.purchased) CardBackground.copy(alpha = 0.5f) else CardBackground),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape)
                    .background(if (item.purchased) Brush.radialGradient(listOf(GreenPrimary, IncomeGreen)) else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(1.5.dp, if (item.purchased) GreenPrimary else TextDisabled.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (item.purchased) Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = if (item.purchased) TextDisabled else TextPrimary, fontSize = 14.sp, fontWeight = if (!item.purchased) FontWeight.Medium else FontWeight.Normal, textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None)
                Text("$qty ${item.unit}", color = TextDisabled, fontSize = 12.sp)
                val usedCount = remember(item.usedInDays) { item.parsedUsedInDays().size }
                if (!item.purchased && usedCount >= 2) {
                    Spacer(Modifier.height(3.dp))
                    Text("✦ $usedCount dias", color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!item.purchased && !item.packageNote.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFD740).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFFFFD740).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📦 ", fontSize = 10.sp)
                        Text(item.packageNote, color = Color(0xFFFFD740).copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
                if (!item.purchased && enriched != null) {
                    val pricesByStore = remember(enriched.products) {
                        enriched.products
                            .filter { it.price != null && it.price > 0 }
                            .groupBy { it.source.lowercase() }
                            .mapValues { (_, prods) -> prods.minByOrNull { it.price!! }!! }
                            .entries.sortedBy { it.value.price }
                    }
                    if (pricesByStore.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            pricesByStore.forEachIndexed { index, (source, product) ->
                                val isCheapest = index == 0
                                val chipEmoji = when (source) {
                                    "continente" -> "🏪"
                                    "auchan" -> "🟠"
                                    "pingodoce" -> "🟡"
                                    "mercadona" -> "🟢"
                                    else -> "🏷️"
                                }
                                val chipColor = if (isCheapest) GreenPrimary else TextDisabled
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(chipColor.copy(alpha = 0.10f))
                                        .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        "$chipEmoji ${source.replaceFirstChar { it.uppercase() }} ${currencyConfig.format(product.price!!)}",
                                        color = chipColor,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCheapest) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                item.estimatedPrice?.let {
                    Text(
                        currencyConfig.format(it),
                        color = if (item.purchased || enriched?.bestPrice != null) TextDisabled else GreenPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (!item.purchased && enriched?.bestPrice == null) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (item.purchased || enriched?.bestPrice != null) TextDecoration.LineThrough else TextDecoration.None,
                    )
                }
                if (!item.purchased && enriched?.bestPrice != null) {
                    Text(currencyConfig.format(enriched.bestPrice), color = GreenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (item.purchased) {
                    if (item.actualPrice != null) {
                        Text(
                            "✓ ${currencyConfig.format(item.actualPrice)}",
                            color = GreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onPriceClick() },
                        )
                    } else {
                        Text(
                            "Preço real?",
                            color = TextDisabled.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onPriceClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
