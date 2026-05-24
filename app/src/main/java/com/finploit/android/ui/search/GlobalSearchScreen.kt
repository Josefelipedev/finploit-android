package com.finploit.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    viewModel: GlobalSearchViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Pesquisa transações, metas, listas...", color = TextDisabled, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = TextDisabled.copy(0.3f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GreenPrimary,
                    ),
                    trailingIcon = if (state.query.isNotBlank()) {
                        { IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Close, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(18.dp)) } }
                    } else null,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            state.query.length < 2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔍", fontSize = 48.sp)
                    Text("Escreve para pesquisar", color = TextDisabled, fontSize = 14.sp)
                }
            }
            state.transactions.isEmpty() && state.goals.isEmpty() && state.shoppingLists.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem resultados para \"${state.query}\"", color = TextDisabled)
                }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.transactions.isNotEmpty()) {
                    item { SectionHeader("💰 Transações (${state.transactions.size})") }
                    items(state.transactions.take(5)) { tx ->
                        val isIncome = tx.type == "income"
                        val color = if (isIncome) IncomeGreen else ExpenseRed
                        SearchResultCard(
                            icon = if (isIncome) "↑" else "↓",
                            title = tx.description ?: tx.type ?: "Transação",
                            subtitle = tx.createdAt.take(10),
                            value = "€%.2f".format(tx.amount ?: 0.0),
                            valueColor = color,
                        )
                    }
                }
                if (state.goals.isNotEmpty()) {
                    item { SectionHeader("🎯 Metas (${state.goals.size})") }
                    items(state.goals) { goal ->
                        val current = goal.currentValue ?: 0.0
                        val target = goal.targetValue
                        val progress = if (target > 0) current / target else 0.0
                        SearchResultCard(
                            icon = "🏆",
                            title = goal.name,
                            subtitle = "${(progress * 100).toInt()}% concluída",
                            value = "€%.2f".format(target),
                            valueColor = GreenPrimary,
                        )
                    }
                }
                if (state.shoppingLists.isNotEmpty()) {
                    item { SectionHeader("🛒 Listas (${state.shoppingLists.size})") }
                    items(state.shoppingLists) { list ->
                        SearchResultCard(
                            icon = "📋",
                            title = list.name,
                            subtitle = "${list.items.size} itens",
                            value = "",
                            valueColor = TextDisabled,
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SearchResultCard(icon: String, title: String, subtitle: String, value: String, valueColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, color = TextDisabled, fontSize = 12.sp)
            }
            if (value.isNotBlank()) Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
