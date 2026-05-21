package com.finploit.android.ui.mealplanner

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryTab(
    plans: List<MealPlanDto>,
    isLoading: Boolean,
    isDeletingPlan: Int?,
    isClearingHistory: Boolean,
    onRefresh: () -> Unit,
    onDeletePlan: (Int) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = CardBackground,
            title = { Text("Limpar histórico?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Todos os planos de alimentação serão apagados. Esta ação não pode ser desfeita.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { showClearConfirm = false; onClearAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                ) { Text("Apagar tudo", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar", color = TextDisabled)
                }
            },
        )
    }

    when {
        isLoading || isClearingHistory -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = GreenPrimary)
                if (isClearingHistory) {
                    Spacer(Modifier.height(8.dp))
                    Text("A limpar histórico…", color = TextDisabled, fontSize = 13.sp)
                }
            }
        }
        plans.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Nenhum plano anterior", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Os teus cardápios gerados aparecerão aqui", color = TextDisabled, fontSize = 13.sp)
            }
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${plans.size} planos", color = TextDisabled, fontSize = 13.sp)
                    TextButton(
                        onClick = { showClearConfirm = true },
                        enabled = !isClearingHistory,
                    ) {
                        Text("🗑 Limpar tudo", color = Color(0xFFEF5350), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(plans, key = { it.id }) { plan ->
                HistoryPlanCard(
                    plan = plan,
                    isDeleting = isDeletingPlan == plan.id,
                    onDelete = { onDeletePlan(plan.id) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryPlanCard(
    plan: MealPlanDto,
    isDeleting: Boolean,
    onDelete: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = CardBackground,
            title = { Text("Apagar plano?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("O plano da semana de ${plan.weekStart.take(10)} será apagado permanentemente.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                ) { Text("Apagar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancelar", color = TextDisabled)
                }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Semana de ${plan.weekStart.take(10)}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text("${plan.days.size} dias planeados", color = TextDisabled, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (plan.active) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GreenPrimary.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Ativo", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFEF5350), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Apagar plano",
                                tint = Color(0xFFEF5350).copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            plan.shoppingList?.let { list ->
                Spacer(Modifier.height(8.dp))
                Divider(color = TextDisabled.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val total = list.totalItems ?: list.items.size
                    val bought = list.purchasedCount ?: list.items.count { it.purchased }
                    Text("🛒 $bought/$total itens comprados", color = TextSecondary, fontSize = 12.sp)
                    list.totalEstimate?.let {
                        Text(LocalCurrencyConfig.current.format(it), color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
