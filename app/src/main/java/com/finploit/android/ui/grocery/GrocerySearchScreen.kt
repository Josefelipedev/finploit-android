package com.finploit.android.ui.grocery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.GroceryProductDto
import com.finploit.android.data.dto.NearbyStoreDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

private val CHAIN_LABELS = mapOf(
    "continente" to "Continente",
    "auchan" to "Auchan",
    "mercadona" to "Mercadona",
    "pingodoce" to "Pingo Doce",
)

private val CHAIN_COLORS = mapOf(
    "continente" to Color(0xFF1565C0),
    "auchan" to Color(0xFFE65100),
    "mercadona" to Color(0xFF2E7D32),
    "pingodoce" to Color(0xFFAD1457),
)

private val CHAIN_EMOJIS = mapOf(
    "continente" to "🔵",
    "auchan" to "🟠",
    "mercadona" to "🟢",
    "pingodoce" to "🟣",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrocerySearchScreen(
    viewModel: GrocerySearchViewModel,
    initialItems: List<EnrichItem> = emptyList(),
    listTabLabel: String = "Lista de Itens",
    onBack: () -> Unit,
    requestLocationPermission: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(initialItems) {
        if (initialItems.isNotEmpty()) {
            viewModel.setMode(GroceryMode.PLAN_PRICES)
            viewModel.enrichPlanShoppingList(initialItems)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Supermercados PT", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    if (state.nearbyStores.isNotEmpty()) {
                        Text(
                            "${state.nearbyStores.size} lojas próximas",
                            color = GreenPrimary,
                            fontSize = 11.sp,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextSecondary)
                }
            },
            actions = {
                LocationChip(
                    postalCode = state.postalCode,
                    isLoading = state.isLoadingLocation,
                    hasLocation = state.location != null,
                    onClick = {
                        if (requestLocationPermission != null) requestLocationPermission()
                        else viewModel.detectLocation()
                    },
                )
                Spacer(Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        // Mode tabs
        val tabIndex = if (state.mode == GroceryMode.SEARCH) 0 else 1
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = BackgroundDark,
            contentColor = GreenPrimary,
            indicator = { tabs ->
                SecondaryIndicator(Modifier.tabIndicatorOffset(tabs[tabIndex]), color = GreenPrimary)
            },
        ) {
            Tab(selected = tabIndex == 0, onClick = { viewModel.setMode(GroceryMode.SEARCH) }) {
                Text(
                    "🔍 Pesquisar",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (tabIndex == 0) GreenPrimary else TextDisabled,
                    fontSize = 13.sp,
                    fontWeight = if (tabIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Tab(selected = tabIndex == 1, onClick = {
                viewModel.setMode(GroceryMode.PLAN_PRICES)
                if (initialItems.isNotEmpty() && state.enrichedItems.isEmpty()) {
                    viewModel.enrichPlanShoppingList(initialItems)
                }
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    Text(
                        "🛒 $listTabLabel",
                        color = if (tabIndex == 1) GreenPrimary else TextDisabled,
                        fontSize = 13.sp,
                        fontWeight = if (tabIndex == 1) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (initialItems.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${initialItems.size}",
                                color = GreenPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // Nearby stores banner (if location detected)
        AnimatedVisibility(visible = state.nearbyStores.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.nearbyStores.take(5)) { store ->
                    NearbyStoreChip(store)
                }
            }
        }

        // Supermarket filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(CHAIN_LABELS.entries.toList()) { (chain, label) ->
                val selected = chain in state.selectedSupermarkets
                val color = CHAIN_COLORS[chain] ?: GreenPrimary
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.toggleSupermarket(chain) },
                    label = {
                        Text(
                            "${CHAIN_EMOJIS[chain] ?: ""} $label",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.18f),
                        selectedLabelColor = color,
                        selectedLeadingIconColor = color,
                        containerColor = SurfaceDark,
                        labelColor = TextDisabled,
                    ),
                )
            }
        }

        AnimatedContent(
            targetState = state.mode,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "mode_content",
        ) { mode ->
            if (mode == GroceryMode.SEARCH) {
                SearchContent(
                    state = state,
                    onSearch = { keyboard?.hide(); viewModel.search() },
                    onQueryChange = viewModel::setQuery,
                )
            } else {
                PlanPricesContent(state = state, onRetry = {
                    viewModel.enrichPlanShoppingList(initialItems)
                })
            }
        }
    }
}

@Composable
private fun LocationChip(
    postalCode: String,
    isLoading: Boolean,
    hasLocation: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (hasLocation) GreenPrimary.copy(alpha = 0.15f) else SurfaceDark)
            .border(
                1.dp,
                if (hasLocation) GreenPrimary.copy(alpha = 0.4f) else TextDisabled.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GreenPrimary, strokeWidth = 2.dp)
        } else {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (hasLocation) GreenPrimary else TextDisabled,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            postalCode,
            color = if (hasLocation) GreenPrimary else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (hasLocation) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun NearbyStoreChip(store: NearbyStoreDto) {
    val color = CHAIN_COLORS[store.chain] ?: TextDisabled
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(CHAIN_EMOJIS[store.chain] ?: "🏪", fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                store.name.take(20),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "%.1fkm".format(store.distanceKm),
                color = TextDisabled,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun SearchContent(
    state: GroceryUiState,
    onSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ex: frango, arroz, ovos...", color = TextDisabled, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = CardElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GreenPrimary,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (state.query.isNotBlank() && !state.isSearching)
                            Brush.verticalGradient(listOf(GreenPrimary, IncomeGreen))
                        else
                            Brush.verticalGradient(listOf(SurfaceDark, SurfaceDark))
                    )
                    .clickable(enabled = state.query.isNotBlank() && !state.isSearching) { onSearch() },
                contentAlignment = Alignment.Center,
            ) {
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GreenPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Pesquisar",
                        tint = if (state.query.isNotBlank()) BackgroundDark else TextDisabled,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(state.error, color = ExpenseRed, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.height(4.dp))
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.isSearching) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(36.dp))
                        Text("A pesquisar \"${state.lastQuery}\"...", color = TextDisabled, fontSize = 13.sp)
                    }
                }
            }
        } else if (state.hasSearched && state.products.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Sem resultados para \"${state.lastQuery}\"", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Tenta outro nome ou verifica se o scraper está ativo", color = TextDisabled, fontSize = 12.sp)
                    }
                }
            }
        } else if (!state.hasSearched) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛍️", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Pesquisa um ingrediente", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Compara preços nos supermercados PT em tempo real", color = TextDisabled, fontSize = 13.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("frango", "arroz", "ovos").forEach { hint ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(GreenPrimary.copy(alpha = 0.1f))
                                        .border(1.dp, GreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(hint, color = GreenPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.products.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${state.products.size} resultados",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    val cheapest = state.products.minByOrNull { it.price ?: Double.MAX_VALUE }
                    if (cheapest?.price != null) {
                        Text(
                            "Menor: €%.2f".format(cheapest.price),
                            color = IncomeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        items(state.products) { product ->
            ProductCard(product)
        }
    }
}

@Composable
private fun PlanPricesContent(state: GroceryUiState, onRetry: () -> Unit) {
    when {
        state.isEnriching -> EnrichingLoadingState(itemCount = 0)
        state.enrichedItems.isEmpty() && state.error != null -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Erro ao buscar preços", color = ExpenseRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(state.error, color = TextDisabled, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Tentar novamente", color = BackgroundDark)
                    }
                }
            }
        }
        state.enrichedItems.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", fontSize = 52.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Sem lista de compras", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Gera um plano semanal na aba Alimentação primeiro", color = TextDisabled, fontSize = 13.sp)
                }
            }
        }
        else -> {
            // estimatedPrice já é o preço total do item (não por unidade)
            val totalEstimated = state.enrichedItems.sumOf { it.estimatedPrice }
            val totalBest = state.enrichedItems.sumOf { it.bestPrice ?: it.estimatedPrice }
            val savings = (totalEstimated - totalBest).coerceAtLeast(0.0)
            val bestStoreMap = state.enrichedItems
                .filter { it.bestSource != null && it.bestPrice != null }
                .groupBy { it.bestSource!! }
                .mapValues { (_, items) -> items.sumOf { it.bestPrice ?: 0.0 } }
            val bestStore = bestStoreMap.minByOrNull { it.value }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    PriceSummaryCard(
                        estimated = totalEstimated,
                        best = totalBest,
                        savings = savings,
                        itemCount = state.enrichedItems.size,
                    )
                }

                if (bestStore != null && savings > 0.1) {
                    item {
                        BestStoreCard(
                            chain = bestStore.key,
                            totalPrice = bestStore.value,
                            savings = savings,
                        )
                    }
                }

                item {
                    Text(
                        "${state.enrichedItems.size} itens · ${state.enrichedItems.count { it.bestPrice != null }} com preço real",
                        color = TextDisabled,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }

                items(state.enrichedItems) { item ->
                    EnrichedItemCard(item)
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun EnrichingLoadingState(itemCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔍", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "A pesquisar preços...",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Consultando Continente, Auchan, Mercadona e Pingo Doce",
            color = TextDisabled,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(32.dp))
        // Skeleton cards
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .alpha(alpha)
                    .background(CardBackground),
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PriceSummaryCard(
    estimated: Double,
    best: Double,
    savings: Double,
    itemCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Estimativa IA", color = TextDisabled, fontSize = 11.sp)
                    Text("€%.2f".format(estimated), color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = GreenPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("Melhor preço real", color = TextDisabled, fontSize = 11.sp)
                    Text("€%.2f".format(best), color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
            if (savings > 0.1) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(IncomeGreen.copy(alpha = 0.1f))
                        .border(1.dp, IncomeGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Poupas €%.2f comparando com os preços da IA".format(savings),
                            color = IncomeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BestStoreCard(chain: String, totalPrice: Double, savings: Double) {
    val color = CHAIN_COLORS[chain] ?: GreenPrimary
    val label = CHAIN_LABELS[chain] ?: chain
    val emoji = CHAIN_EMOJIS[chain] ?: "🏪"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Melhor opção: $label",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    "Lista completa por €%.2f".format(totalPrice),
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("-€%.2f".format(savings), color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("vs IA", color = TextDisabled, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun EnrichedItemCard(item: EnrichedShoppingItemDto) {
    val qty = if (item.quantity == item.quantity.toLong().toDouble())
        item.quantity.toLong().toString() else "%.1f".format(item.quantity)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("$qty ${item.unit}", color = TextDisabled, fontSize = 12.sp)
                }
                if (item.bestPrice != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "€%.2f".format(item.bestPrice),
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        val bestColor = CHAIN_COLORS[item.bestSource] ?: TextDisabled
                        Text(
                            "${CHAIN_EMOJIS[item.bestSource] ?: ""} ${CHAIN_LABELS[item.bestSource] ?: item.bestSource ?: ""}",
                            color = bestColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sem dados", color = TextDisabled, fontSize = 12.sp)
                        Text("€%.2f est.".format(item.estimatedPrice), color = TextDisabled.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }

            if (item.products.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                // Price comparison dots per store
                val byStore = item.products.groupBy { it.source }
                    .mapValues { (_, ps) -> ps.minByOrNull { it.price ?: Double.MAX_VALUE } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    byStore.forEach { (source, cheapest) ->
                        val color = CHAIN_COLORS[source] ?: TextDisabled
                        if (cheapest?.price != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.12f))
                                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(CHAIN_EMOJIS[source] ?: "•", fontSize = 10.sp)
                                    Text(
                                        "€%.2f".format(cheapest.price),
                                        color = if (source == item.bestSource) IncomeGreen else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (source == item.bestSource) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: GroceryProductDto) {
    val chainColor = CHAIN_COLORS[product.source] ?: GreenPrimary
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !product.url.isNullOrBlank()) {
                product.url?.let { uriHandler.openUri(it) }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center,
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Text(CHAIN_EMOJIS[product.source] ?: "🛒", fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(chainColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "${CHAIN_EMOJIS[product.source] ?: ""} ${CHAIN_LABELS[product.source] ?: product.source}",
                            color = chainColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (product.unitPrice != null && product.unit != null) {
                        Text("€%.2f/%s".format(product.unitPrice, product.unit), color = TextDisabled, fontSize = 10.sp)
                    }
                    if (product.availability == "in_stock") {
                        Text("✓ disponível", color = GreenPrimary.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (product.price != null) {
                    Text("€%.2f".format(product.price), color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                if (!product.url.isNullOrBlank()) {
                    Text("ver →", color = chainColor.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }
        }
    }
}
