package com.finploit.android.ui.mealplanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.finploit.android.ui.theme.currencyConfigByCode
import com.finploit.android.ui.components.OwnerChip

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
    onLoadPlanDays: (Int) -> Unit = {},
    eatenMeals: Set<String> = emptySet(),
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
            if (plans.size >= 2) {
                item {
                    val sorted = plans.sortedByDescending { it.weekStart }
                    val newest = sorted[0]
                    val previous = sorted[1]
                    val newestCost = newest.shoppingList?.totalEstimate
                    val previousCost = previous.shoppingList?.totalEstimate
                    // Duas semanas em moedas diferentes não se subtraem: o
                    // resultado era um número plausível e sem significado. Com
                    // moedas diferentes não se mostra comparação nenhuma.
                    val mesmaMoeda = newest.currency == previous.currency
                    if (newestCost != null && previousCost != null && mesmaMoeda) {
                        val delta = newestCost - previousCost
                        val deltaColor = if (delta > 0) Color(0xFFEF5350) else GreenPrimary
                        // O símbolo é o do plano, não o da app: um cardápio
                        // gerado em euros continua a ler-se em euros depois de a
                        // conta passar a reais.
                        val moeda = newest.currency?.let { currencyConfigByCode(it) }
                            ?: LocalCurrencyConfig.current
                        val deltaLabel = if (delta > 0) "▲ +${moeda.format(delta)} vs semana anterior" else "▼ −${moeda.format(-delta)} vs semana anterior"
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(deltaColor.copy(alpha = 0.08f))
                                .border(1.dp, deltaColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(if (delta > 0) "📈" else "📉", fontSize = 20.sp)
                            Column {
                                Text("Comparação com plano anterior", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(deltaLabel, color = deltaColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            items(plans, key = { it.id }) { plan ->
                HistoryPlanCard(
                    plan = plan,
                    isDeleting = isDeletingPlan == plan.id,
                    onDelete = { onDeletePlan(plan.id) },
                    onLoadDays = { onLoadPlanDays(plan.id) },
                    eatenMeals = eatenMeals,
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
    onLoadDays: () -> Unit = {},
    eatenMeals: Set<String> = emptySet(),
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

    var expanded by remember { mutableStateOf(false) }
    val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    expanded = !expanded
                    if (expanded && plan.days.isEmpty()) onLoadDays()
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Semana de ${plan.weekStart.take(10)}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        // Quem gerou. O cardápio é do casal, mas cada plano sai
                        // das preferências de quem carregou no botão.
                        Spacer(Modifier.width(6.dp))
                        OwnerChip(plan.userId)
                    }
                    val mealTypes = listOf("breakfast", "lunch", "dinner")
                    val totalMeals = plan.days.size * 3
                    val eatenCount = plan.days.sumOf { day ->
                        mealTypes.count { type -> "${plan.id}_${day.dayOfWeek}_$type" in eatenMeals }
                    }
                    val adherencePct = if (totalMeals > 0) (eatenCount * 100) / totalMeals else 0
                    val adherenceColor = when {
                        adherencePct >= 80 -> GreenPrimary
                        adherencePct >= 50 -> Color(0xFFFFD740)
                        else -> TextDisabled
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${plan.days.size} dias", color = TextDisabled, fontSize = 12.sp)
                        if (eatenCount > 0) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(adherenceColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text("🎯 $adherencePct%", color = adherenceColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text("· toque para expandir", color = TextDisabled, fontSize = 12.sp)
                        }
                    }
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
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = TextDisabled.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))
                    plan.days.sortedBy { it.dayOfWeek }.forEach { day ->
                        val bf = parseMeal(day.breakfast)
                        val lu = parseMeal(day.lunch)
                        val di = parseMeal(day.dinner)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Text(dayNames.getOrElse(day.dayOfWeek) { "?" }, color = TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                bf?.let { Text("☀️ ${it.name}", color = TextPrimary, fontSize = 11.sp) }
                                lu?.let { Text("🌤 ${it.name}", color = TextSecondary, fontSize = 11.sp) }
                                di?.let { Text("🌙 ${it.name}", color = TextSecondary, fontSize = 11.sp) }
                                if (bf == null && lu == null && di == null) Text("Sem refeições em casa", color = TextDisabled, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            plan.shoppingList?.let { list ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = TextDisabled.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val total = list.totalItems ?: list.items.size
                    val bought = list.purchasedCount ?: list.items.count { it.purchased }
                    Text("🛒 $bought/$total itens comprados", color = TextSecondary, fontSize = 12.sp)
                    list.totalEstimate?.let {
                        val moeda = plan.currency?.let { c -> currencyConfigByCode(c) }
                            ?: LocalCurrencyConfig.current
                        Text(moeda.format(it), color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
