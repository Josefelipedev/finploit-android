package com.finploit.android.ui.transactions

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.ui.components.BillChip
import com.finploit.android.ui.components.OriginChip
import com.finploit.android.ui.components.OwnerChip
import com.finploit.android.util.isTransfer
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.LocalOwnerNaming
import com.finploit.android.ui.theme.currencyConfigByCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onEditTransaction: (FinanceItemDto) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var pendingDeleteTx by remember { mutableStateOf<FinanceItemDto?>(null) }

    val displayed = remember(uiState.transactions, uiState.searchQuery, uiState.typeFilter) {
        uiState.transactions.filter { tx ->
            (uiState.typeFilter == "all" || tx.type == uiState.typeFilter) &&
            (uiState.searchQuery.isBlank() ||
             tx.description?.contains(uiState.searchQuery, ignoreCase = true) == true ||
             tx.amount?.toString()?.contains(uiState.searchQuery) == true)
        }
    }

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            last >= total - 3 && total > 0
        }
    }

    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMore() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Undo para swipe delete: executa deleção só se o usuário não cancelar
    LaunchedEffect(pendingDeleteTx) {
        pendingDeleteTx?.let { tx ->
            val result = snackbarHostState.showSnackbar(
                // Apagar um lançamento vinculado reverte a conta para pendente
                // — o servidor já o fazia, mas em silêncio (B5). É a última
                // hora para o dizer: depois disto a conta muda de estado.
                message = tx.bill?.let { "Removida — a conta ${it.dueLabel} volta a pendente" }
                    ?: "Transação removida",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short,
            )
            if (result != SnackbarResult.ActionPerformed) {
                viewModel.deleteTransaction(tx.id)
            }
            pendingDeleteTx = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SurfaceDark)) {
        TopAppBar(
            title = { Text("Transações", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark, titleContentColor = Color.White),
            actions = {
                IconButton(onClick = viewModel::loadTransactions) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Green80)
                }
            }
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::setSearch,
            placeholder = { Text("Buscar por descrição…", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green80,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Green80,
            ),
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("all" to "Todos", "income" to "Receitas", "expense" to "Despesas").forEach { (key, label) ->
                FilterChip(
                    selected = uiState.typeFilter == key,
                    onClick = { viewModel.setTypeFilter(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Green80.copy(alpha = 0.2f),
                        selectedLabelColor = Green80,
                    ),
                )
            }
        }

        SnackbarHost(snackbarHostState) { Snackbar(it) }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green80)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Erro", color = ExpenseRed)
            }
            displayed.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma transação encontrada", color = Color.Gray)
                }
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(displayed, key = { it.id }) { tx ->
                    SwipeToDeleteTransaction(
                        tx = tx,
                        isDeleting = tx.id in uiState.deletingIds,
                        onDelete = { pendingDeleteTx = tx },
                        onEdit = { onEditTransaction(tx) },
                    )
                }
                if (uiState.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green80, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                if (!uiState.hasMore && displayed.size > 5) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("Todas as transações carregadas", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteTransaction(
    tx: FinanceItemDto,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        }
    )
    LaunchedEffect(isDeleting) { if (!isDeleting) dismissState.reset() }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ExpenseRed.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (isDeleting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White)
            }
        },
    ) {
        TransactionListItem(tx = tx, onClick = onEdit)
    }
}

@Composable
fun TransactionListItem(tx: FinanceItemDto, onClick: (() -> Unit)? = null) {
    val isIncome = tx.type == "income"
    // Uma transferência não é despesa: sai do bolso mas continua a ser tua.
    // A vermelho e rotulada "Despesa" era a leitura que se quis evitar (T6.1).
    val transferencia = isTransfer(tx.type)
    val color = when {
        isIncome -> IncomeGreen
        transferencia -> Color.Gray
        else -> ExpenseRed
    }
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    // A moeda do próprio lançamento: com a do utilizador, uma despesa em reais
    // aparecia com "€" à frente do mesmo número.
    val currencyConfig = currencyConfigByCode(tx.currency ?: LocalCurrencyConfig.current.code)

    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description ?: (tx.type?.replaceFirstChar { it.uppercase() } ?: ""),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = tx.movementDate, color = Color.Gray, fontSize = 12.sp)
                    // Quem lançou — só aparece no workspace do casal. Um
                    // registo servido do cache offline não tem `userId`, e
                    // nesse caso o chip fica de fora em vez de adivinhar.
                    if (LocalOwnerNaming.current.nameOf(tx.userId) != null) {
                        Spacer(Modifier.width(8.dp))
                        OwnerChip(tx.userId)
                    }
                    // A outra ponta do lançamento: que conta é que ele quita.
                    if (tx.bill != null) {
                        Spacer(Modifier.width(8.dp))
                        BillChip(tx.bill)
                    } else if (tx.origin != null) {
                        // De onde veio, quando não foi escrito à mão (T6.6).
                        Spacer(Modifier.width(8.dp))
                        OriginChip(tx.origin)
                    }
                }
            }
            Text(
                text = currencyConfig.format(tx.amount ?: 0.0),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}
