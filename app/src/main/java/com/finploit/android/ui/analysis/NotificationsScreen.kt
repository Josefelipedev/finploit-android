package com.finploit.android.ui.analysis

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.LocalCurrencyConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Próximas Contas", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = viewModel::load) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Green80)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
            ),
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green80)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Erro", color = ExpenseRed)
            }
            uiState.upcomingBills.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma conta vencendo em breve", color = Color.Gray)
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(uiState.upcomingBills) { bill ->
                    UpcomingBillCard(bill)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun UpcomingBillCard(bill: UpcomingBill) {
    val currency = LocalCurrencyConfig.current
    val isIncome = bill.transaction.type == "income"
    val typeColor = if (isIncome) IncomeGreen else ExpenseRed

    val urgencyColor = when (bill.daysUntilDue) {
        0 -> ExpenseRed
        1 -> Color(0xFFFFAB40)
        else -> Color.Gray
    }
    val urgencyLabel = when (bill.daysUntilDue) {
        0 -> "Vence HOJE"
        1 -> "Vence amanhã"
        else -> "Vence em ${bill.daysUntilDue} dias"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.transaction.description ?: bill.transaction.type,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(urgencyLabel, color = urgencyColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currency.format(bill.transaction.amount),
                    color = typeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    if (isIncome) "Receita" else "Despesa",
                    color = typeColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
