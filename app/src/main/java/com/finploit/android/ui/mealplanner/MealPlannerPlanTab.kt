package com.finploit.android.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanTab(
    plan: MealPlanDto?,
    isGenerating: Boolean,
    error: String?,
    selectedBudget: BudgetPreset,
    currencyCode: String,
    onSelectBudget: (BudgetPreset) -> Unit,
    onGenerate: () -> Unit,
    schedule: List<ScheduleItemDto>,
    prepTimeFilter: Int? = null,
    onSetPrepTimeFilter: (Int?) -> Unit = {},
    onDayClick: (MealPlanDayDto) -> Unit,
) {
    val scheduleByDay = schedule.associateBy { it.dayOfWeek }
    val currencyConfig = LocalCurrencyConfig.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column {
                Text("Orçamento semanal", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BudgetPreset.entries) { preset ->
                        val isSelected = preset == selectedBudget
                        val color = when (preset) {
                            BudgetPreset.ECONOMY -> Color(0xFF69F0AE)
                            BudgetPreset.BALANCED -> GreenPrimary
                            BudgetPreset.PREMIUM -> Color(0xFFFFD740)
                            BudgetPreset.FREE -> TextSecondary
                        }
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.18f) else CardBackground)
                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isGenerating) { onSelectBudget(preset) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(preset.label, color = if (isSelected) color else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            val amount = preset.amountForCurrency(currencyCode)
                            val desc = amount?.let { "~${currencyConfig.symbol}${it.toInt()}/sem" } ?: preset.description
                            Text(desc, color = if (isSelected) color.copy(alpha = 0.8f) else TextDisabled, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        if (error != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))) {
                    Text(error, color = ExpenseRed, modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                }
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (!isGenerating) Brush.horizontalGradient(listOf(GreenPrimary, IncomeGreen)) else Brush.horizontalGradient(listOf(TextDisabled, TextDisabled))),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = onGenerate, enabled = !isGenerating, modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    shape = RoundedCornerShape(14.dp), elevation = null,
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BackgroundDark, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Gerando cardápio com IA...", color = BackgroundDark, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(if (plan == null) "🥗 Gerar Cardápio com IA" else "🔄 Regenerar Cardápio", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        if (plan == null && !isGenerating) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Como funciona", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 2.dp))
                    OnboardingStepRow(1, "Configure sua agenda", "Na aba Agenda defina os dias de trabalho e folga", GreenPrimary)
                    OnboardingStepRow(2, "Gere o cardápio com IA", "Escolha o orçamento e toque em Gerar Cardápio", Color(0xFF64B5F6))
                    OnboardingStepRow(3, "Compare preços", "Na aba Compras actualize os preços nos supermercados e economize", Color(0xFFFFD740))
                }
            }
        } else if (plan != null) {
            item {
                val today = java.time.LocalDate.now()
                val daysByDow = plan.days.associateBy { it.dayOfWeek }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    items(7) { dow ->
                        val day = daysByDow[dow]
                        val dayColor = DAY_COLORS.getOrElse(dow) { GreenPrimary }
                        val todayDow = if (today.dayOfWeek.value == 7) 0 else today.dayOfWeek.value
                        val isToday = dow == todayDow
                        Column(
                            modifier = Modifier
                                .width(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isToday) dayColor.copy(alpha = 0.2f) else CardBackground)
                                .border(if (isToday) 1.5.dp else 1.dp, if (isToday) dayColor else TextDisabled.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable(enabled = day != null) { day?.let { onDayClick(it) } }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(DAY_NAMES.getOrElse(dow) { "" }, color = if (isToday) dayColor else TextDisabled, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(DAY_EMOJIS.getOrElse(dow) { "?" }, fontSize = 14.sp)
                            Spacer(Modifier.height(2.dp))
                            val cal = day?.calories
                            if (cal != null) Text("${cal}k", color = TextDisabled, fontSize = 9.sp)
                            else Text("—", color = TextDisabled.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                }
            }
            item {
                val planTotal = plan.shoppingList?.totalEstimate ?: 0.0
                val itemCount = plan.shoppingList?.items?.size ?: 0
                val dayCount = plan.days.size
                val budgetAmount = selectedBudget.amountForCurrency(currencyCode)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Total estimado", color = TextDisabled, fontSize = 11.sp)
                            Text(currencyConfig.format(planTotal), color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("$itemCount itens · $dayCount dias", color = TextDisabled, fontSize = 11.sp)
                        }
                        if (budgetAmount != null) {
                            val overBudget = planTotal > budgetAmount
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background((if (overBudget) ExpenseRed else GreenPrimary).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (overBudget) "▲ acima do orçamento" else "✓ no orçamento",
                                    color = if (overBudget) ExpenseRed else GreenPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
            item {
                val allMeals = plan.days.flatMap { d ->
                    listOfNotNull(parseMeal(d.breakfast), parseMeal(d.lunch), parseMeal(d.dinner))
                }
                if (allMeals.isNotEmpty()) {
                    val totalCals = allMeals.sumOf { it.calories }
                    val totalProtein = allMeals.mapNotNull { it.protein }.sum()
                    val totalCarbs = allMeals.mapNotNull { it.carbs }.sum()
                    val totalFat = allMeals.mapNotNull { it.fat }.sum()
                    val avgCals = if (plan.days.isNotEmpty()) totalCals / plan.days.size else 0
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Resumo Nutricional Semanal", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Média: ~$avgCals kcal/dia", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            val maxVal = maxOf(totalProtein, totalCarbs, totalFat).toFloat().coerceAtLeast(1f)
                            listOf(
                                Triple("Proteína", totalProtein.toInt(), Color(0xFF64B5F6)),
                                Triple("Carbs", totalCarbs.toInt(), Color(0xFFFFD740)),
                                Triple("Gordura", totalFat.toInt(), Color(0xFFFF8A65)),
                            ).forEach { (label, value, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, color = TextDisabled, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(TextDisabled.copy(alpha = 0.15f))) {
                                        Box(modifier = Modifier.fillMaxWidth(value.toFloat() / maxVal).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("${value}g", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(42.dp))
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Todos", 15 to "≤15 min", 30 to "≤30 min").forEach { (mins, label) ->
                        FilterChip(
                            selected = prepTimeFilter == mins,
                            onClick = { onSetPrepTimeFilter(mins) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = GreenPrimary,
                                labelColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
            val costPerDay = (plan.shoppingList?.totalEstimate ?: 0.0).let { total ->
                if (plan.days.isNotEmpty()) total / plan.days.size else null
            }
            val filteredDays = plan.days.sortedBy { it.dayOfWeek }.filter { day ->
                if (prepTimeFilter == null) true
                else {
                    val meals = listOfNotNull(parseMeal(day.breakfast), parseMeal(day.lunch), parseMeal(day.dinner))
                    meals.isNotEmpty() && meals.all { meal ->
                        val mins = meal.prepTime?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
                        mins == null || mins <= prepTimeFilter
                    }
                }
            }
            items(filteredDays) { day ->
                DayCard(day = day, scheduleType = scheduleByDay[day.dayOfWeek]?.dayType, costPerDay = costPerDay, onClick = { onDayClick(day) })
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
internal fun DayCard(day: MealPlanDayDto, scheduleType: String?, costPerDay: Double? = null, onClick: () -> Unit) {
    val breakfast = parseMeal(day.breakfast)
    val lunch = parseMeal(day.lunch)
    val dinner = parseMeal(day.dinner)
    val hasSnack = parseSnack(day.snacks) != null
    val dayColor = DAY_COLORS.getOrElse(day.dayOfWeek) { GreenPrimary }
    val typeColor = scheduleType?.let { DAY_TYPE_COLORS[it] } ?: TextDisabled

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(onClick = onClick)) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)).background(dayColor))
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(dayColor.copy(alpha = 0.15f)).border(1.dp, dayColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(DAY_EMOJIS.getOrElse(day.dayOfWeek) { "📅" }, fontSize = 14.sp)
                        Text(DAY_NAMES.getOrElse(day.dayOfWeek) { "?" }, color = dayColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val currencyConfigForDay = LocalCurrencyConfig.current
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(DAY_FULL_NAMES.getOrElse(day.dayOfWeek) { "?" }, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        scheduleType?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(typeColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(DAY_TYPE_LABELS[it] ?: it, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        day.calories?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(IncomeGreen.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("🔥 $it kcal", color = IncomeGreen, fontSize = 10.sp)
                            }
                        }
                        costPerDay?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF64B5F6).copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("~${currencyConfigForDay.format(it)}/dia", color = Color(0xFF64B5F6), fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (breakfast != null) MacroChip("☀️ ${breakfast.name.take(16)}", dayColor)
                        if (lunch != null) MacroChip("🌤 ${lunch.name.take(16)}", Color(0xFF64B5F6))
                        else if (scheduleType == "WORK" || scheduleType == "HALF_OFF") MacroChip("🌤 Fora", TextDisabled)
                        if (dinner != null) MacroChip("🌙 ${dinner.name.take(16)}", Color(0xFFCE93D8))
                        else if (scheduleType == "WORK") MacroChip("🌙 Fora", TextDisabled)
                        if (hasSnack) MacroChip("🍎 Lanche", Color(0xFFFFD740))
                    }
                }
                Text("›", color = TextDisabled, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
internal fun OnboardingStepRow(step: Int, title: String, subtitle: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.07f))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$step", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TextDisabled, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
